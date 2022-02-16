import threading
from socket import socket
from typing import Tuple
from networking.abstract.msg_handler import MsgHandler
from security.message_security_preprocessor import MessageSecurityPreprocessor
from security.session import Session
from security.tls_handler import TLSHandler
from networking.abstract.conn_state_obs import ConnectionStateObserver


class MessageListener(ConnectionStateObserver):
    def __init__(self, msg_preprocessor: MessageSecurityPreprocessor, tls_handler: TLSHandler, msg_handler: MsgHandler):
        self.msg_preprocessor = msg_preprocessor
        self.tls_handler = tls_handler
        self.msg_handler = msg_handler
        self.header_size = msg_preprocessor.get_basic_header_size()
        self.session = None
        self.thread = None

        self.keep_listenning = True
        self.keep_listenning_lock = threading.Lock()

    def _recv(self, sock: socket, size: int) -> bytes:
        msg = b''
        while len(msg) < size:
            cur = sock.recv(size)
            if len(cur) == 0:
                raise ConnectionAbortedError("Received EOF")
            msg += cur
        return msg

    def _listen(self):
        sock = self.session.get_tcp_socket()
        try:
            while True:
                with self.keep_listenning_lock:
                    if not self.keep_listenning:
                        return

                basic_header, msg_size = self._receive_basic_header(sock)

                packet = self._receive_full_packet(
                    sock, basic_header, msg_size)

                self.tls_handler.handle_tls_message(self.session, packet)
        except (ConnectionAbortedError, ConnectionResetError) as e:
            self.msg_handler.rcving_failed(e)
            return

    def _receive_basic_header(self, sock: socket) -> Tuple[bytes, int]:
        # returns basic header and size of data that still needs to be received for this packet
        basic_header = self._recv(sock, self.header_size)
        msg_size = self.msg_preprocessor.get_message_size(basic_header)
        return basic_header, msg_size

    def _receive_full_packet(self, sock: socket, basic_header: bytes, msg_size: int) -> bytes:
        CHUNK_SIZE = 8 * 1024
        data = [basic_header]
        recvd_size = 0

        while recvd_size < msg_size:
            fragment = self._recv(sock,
                                  min(CHUNK_SIZE, msg_size - recvd_size))
            recvd_size += len(fragment)
            data.append(fragment)

        return b''.join(data)

    def connection_established(self, session: Session):
        self.session = session
        self.keep_listenning = True
        self.thread = threading.Thread(
            target=self._listen, daemon=True)

        self.thread.start()

    def connection_lost(self, session: Session):
        with self.keep_listenning_lock:
            self.session = None
            self.thread = None
            self.keep_listenning = False
