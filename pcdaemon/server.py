from auth_state_obs import AuthStateObserver
from ss_capturer import SSCapturer
from authenticator import Authenticator
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
from dotenv import dotenv_values


class Server(SsSender, AuthStateObserver):
    # _IP_ADDR = '192.168.0.167'
    _HEADER_SIZE = 10
    _MAX_CONNECTIONS = 1  # TODO not ready for more

    def __init__(self, addr: str, port: int, auth: Authenticator):
        self._PORT = port
        self._IP_ADDR = addr
        self.auth = auth
        self.streamer = None

        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        atexit.register(self.socket.close)

        self.socket.bind((self._IP_ADDR, self._PORT))
        self.socket.listen(self._MAX_CONNECTIONS)

        self.sender = Sender(header_size=self._HEADER_SIZE)
        self.listener = Listener(self.auth, header_size=self._HEADER_SIZE)

        self.connection_state_observers: List[ConnectionStateObserver] = [
            self.sender, self.listener, self.auth]

    def listen_for_connection(self, streamer: Streamer):
        self.listener.set_streamer(streamer)
        self.streamer = streamer
        print('waiting for connection')
        self.client, self.client_addr = self.socket.accept()
        print(f'Connected: {self.client_addr}')
        self._handle_connection()

    def _handle_connection(self):
        for obs in self.connection_state_observers:
            obs.connection_established(self.client)

        self.auth.await_authentication(self.client)
        self.streamer.stream()

    def send_ss(self, ss_base64: str):
        self.sender.send_json(MsgCode.SSHOT, ss_base64)

    def auth_suceeded(self, client_socket: socket):
        self.sender.send_json(MsgCode.AUTH_CHECKED, True)

    def auth_failed(self, client_socket: socket):
        self.sender.send_json(MsgCode.AUTH_CHECKED, False)


def main():
    config = dotenv_values(".env")

    auth = Authenticator(config['PASSWORD'])
    server = Server(config['IP_ADDR'], config['PORT'], auth)
    ss_capturer = SSCapturer()
    streamer = Streamer(server, ss_capturer, max_fps=config['MAX_FPS'])

    auth.add_auth_state_obs(server)

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
