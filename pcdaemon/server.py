from sound_capturer import SoundCapturer
from sender import Sender
from msg_handler import MsgHandler
from auth_state_obs import AuthStateObserver
from ss_capturer import SSCapturer
from authenticator import Authenticator
from streamer import Streamer
from stream_msg_handler import StreamMsgHandler
from conn_state_obs import ConnectionStateObserver
from typing import List
from msg_codes import MsgCode
import socket
import atexit
import traceback
from socket_sender import SocketSender
from listener import Listener
from dotenv import dotenv_values


class Server(Sender, AuthStateObserver):
    _HEADER_SIZE = 10
    _MAX_CONNECTIONS = 1  # TODO(pointless?) not ready for more

    def __init__(self, addr: str, port: int, auth: Authenticator, msg_handler: MsgHandler):
        self._PORT = port
        self._IP_ADDR = addr
        self.auth = auth
        self.streamer = None
        self.msg_handler = msg_handler

        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        atexit.register(self.socket.close)

        self.socket.bind((self._IP_ADDR, self._PORT))
        self.socket.listen(self._MAX_CONNECTIONS)

        self.sender = SocketSender(header_size=self._HEADER_SIZE)
        self.listener = Listener(
            self.msg_handler, header_size=self._HEADER_SIZE)

        self.connection_state_observers: List[ConnectionStateObserver] = [
            self.sender, self.listener, self.auth, self.msg_handler]

    def listen_for_connection(self, streamer: Streamer):
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
        self.sender.send_json(MsgCode.SSHOT, {'image': ss_base64})

    def send_audio_frame(self, frame64: str):
        self.sender.send_json(MsgCode.AUDIO_FRAME, {'frame': frame64})

    def auth_suceeded(self, client_socket: socket):
        self.sender.send_json(MsgCode.AUTH_CHECKED, {'is_granted': True})

    def auth_failed(self, client_socket: socket):
        self.sender.send_json(MsgCode.AUTH_CHECKED, {'is_granted': False})


def main():
    config = dotenv_values(".env")

    auth = Authenticator(config['PASSWORD'])
    msg_handler = StreamMsgHandler(auth)
    ss_capturer = SSCapturer()
    sound_capturer = SoundCapturer()  # TODO get init args from config

    server = Server(config['IP_ADDR'], int(config['PORT']), auth, msg_handler)
    streamer = Streamer(server, ss_capturer, sound_capturer,
                        max_fps=int(config['MAX_FPS']))

    msg_handler.set_streamer(streamer)
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
