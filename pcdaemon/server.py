from ss_capturer import SSCapturer
from streamer import Streamer
from conn_state_obs import ConnectionStateObserver
from typing import List
from ss_sender import SsSender
from msg_codes import MsgCode
import socket
import atexit
import traceback
from sender import Sender
from listener import Listener

MAX_FPS = 30


class Server(SsSender):
    _PORT = 9559
    _IP_ADDR = '127.0.0.1'
    _HEADER_SIZE = 10

    def __init__(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        atexit.register(self.socket.close)

        self.socket.bind((self._IP_ADDR, self._PORT))
        self.socket.listen(1)

        self.sender = Sender(header_size=self._HEADER_SIZE)
        self.listener = Listener(header_size=self._HEADER_SIZE)

        self.connection_state_observers: List[ConnectionStateObserver] = [
            self.sender, self.listener]

    def listen_for_connection(self, streamer: Streamer):
        self.listener.set_streamer(streamer)
        print('waiting for connection')
        self.client, self.client_addr = self.socket.accept()
        print(f'Connected: {self.client_addr}')
        self._handle_connection(streamer)

    def _handle_connection(self, streamer: Streamer):
        for obs in self.connection_state_observers:
            obs.connection_established(self.client)

        streamer.stream()

    def send_ss(self, ss_base64: str):
        self.sender.send_json(MsgCode.SSHOT, ss_base64)


def main():
    server = Server()
    ss_capturer = SSCapturer()
    streamer = Streamer(server, ss_capturer, max_fps=MAX_FPS)

    while True:
        try:
            server.listen_for_connection(streamer)
        except (ConnectionError, BrokenPipeError) as e:
            print(e)
            print('LOST CONNECTION')
        except Exception as e:
            traceback.print_exc()
            break


if __name__ == '__main__':
    main()
