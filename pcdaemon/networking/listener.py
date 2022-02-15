import threading
from socket import socket
from networking.abstract.msg_handler import MsgHandler
from security.tls_handler import TLSHandler
from networking.abstract.conn_state_obs import ConnectionStateObserver


class Listener(ConnectionStateObserver):
    def __init__(self, tls_handler: TLSHandler, msg_handler: MsgHandler, client_socket: socket = None):
        self.tls_handler = tls_handler
        self.msg_handler = msg_handler
        self.client = client_socket
        self.header_size = tls_handler.get_basic_tls_header_size()
        self.thread = None

        self.keep_listenning = True
        self.keep_listenning_lock = threading.Lock()

    def _recv(self, size: int) -> bytes:
        msg = b''
        while len(msg) < size:
            cur = self.client.recv(size)
            if len(cur) == 0:
                raise ConnectionAbortedError("Received EOF")
            msg += cur
        return msg

    def _listen(self):
        CHUNK_SIZE = 8 * 1024
        try:
            while True:
                with self.keep_listenning_lock:
                    if not self.keep_listenning:
                        return

                basic_header = self._recv(self.header_size)
                msg_size = self.tls_handler.get_total_packet_size(
                    basic_header) - self.header_size

                print('got header', basic_header)
                print('expected size', msg_size - 12)

                data = [basic_header]
                recvd_size = 0

                while recvd_size < msg_size:
                    fragment = self._recv(
                        min(CHUNK_SIZE, msg_size - recvd_size))
                    recvd_size += len(fragment)
                    data.append(fragment)

                print('got full packet')
                self.tls_handler.handle_tls_message(
                    self.client, b''.join(data))
        except (ConnectionAbortedError, ConnectionResetError) as e:
            self.msg_handler.rcving_failed(e)
            return

    def connection_established(self, client_socket: socket):
        self.client = client_socket
        self.keep_listenning = True
        self.thread = threading.Thread(
            target=self._listen, daemon=True)

        self.thread.start()

    def connection_lost(self, client_socket: socket):
        with self.keep_listenning_lock:
            self.client = None
            self.thread = None
            self.keep_listenning = False
