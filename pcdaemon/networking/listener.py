import json
import threading
from socket import socket
from utils.msg_codes import MsgCode
from networking.abstract.msg_handler import MsgHandler
from networking.abstract.conn_state_obs import ConnectionStateObserver


'''
TCP Communication Packet
  |--------------------------------------------|
  |  code(8)    |    size(16)   | reserved(8)  |
  |--------------------------------------------|
  |                  data                      |
  |--------------------------------------------|
'''


class Listener(ConnectionStateObserver):
    def __init__(self, msg_handler: MsgHandler, client_socket: socket = None, header_size: int = 10):
        self.msg_handler = msg_handler
        self.client = client_socket
        self.header_size = header_size
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
                msg_size = int(self._recv(
                    self.header_size).decode('utf-8').strip())

                data = []
                recvd_size = 0

                while recvd_size < msg_size:
                    fragment = self._recv(
                        min(CHUNK_SIZE, msg_size - len(data)))
                    recvd_size += len(fragment)
                    data.append(fragment)

                data = json.loads(b''.join(data).decode('utf-8'))

                self.msg_handler.handle_msg(
                    MsgCode(data['code']), data['body'])
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
