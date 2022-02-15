import socket
import atexit
import traceback
from pathlib import Path
from dotenv import dotenv_values
from typing import List
from media.sound_capturer import SoundCapturer
from networking.media_handler import MediaHandler
from networking.abstract.sender import Sender
from networking.abstract.msg_handler import MsgHandler
from security.asymmetric_security_handler import AsymmetricSecurityHandler, RSA_AsymmetricSecurityHandler
from security.session_handler import SessionHandler
from security.TCPGuard import TCPGuard
from security.tls_handler import TLSHandler
from utils.auth_state_obs import AuthStateObserver
from media.ss_capturer import SSCapturer
from utils.authenticator import Authenticator
from media.streamers.streamer import Streamer
from networking.stream_msg_handler import StreamMsgHandler
from networking.abstract.conn_state_obs import ConnectionStateObserver
from utils.msg_codes import MsgCode
from networking.message_sender import MessageSender
from networking.listener import Listener


# TODO both audio and video should be sent on second socket using UDP

class Server(Sender, AuthStateObserver):
    _MAX_CONNECTIONS = 1  # TODO(pointless?) not ready for more

    def __init__(self, addr: str, tcp_port: int, udp_port: int,
                 auth: Authenticator, msg_handler: MsgHandler,
                 tls_handler: TLSHandler, session_handler: SessionHandler):
        self._PORT = tcp_port
        self._IP_ADDR = addr
        self.auth = auth
        self.streamer = None
        self.msg_handler = msg_handler
        self.session_handler = session_handler
        self.media_handler = MediaHandler(udp_port, addr)  # TODO
        self.tls_handler = tls_handler

        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        atexit.register(self.socket.close)

        self.socket.bind((self._IP_ADDR, self._PORT))
        self.socket.listen(self._MAX_CONNECTIONS)

        self.sender = MessageSender(self.tls_handler)
        self.listener = Listener(self.tls_handler, self.msg_handler)

        self.connection_state_observers: List[ConnectionStateObserver] = [
            self.session_handler, self.tls_handler, self.media_handler,
            self.sender, self.listener, self.auth, self.msg_handler]

        for obs in self.connection_state_observers:
            self.msg_handler.add_conn_state_obs(obs)

    def listen_for_connection(self, streamer: Streamer):
        self.streamer = streamer
        print('Waiting for connection...')
        self.client, self.client_addr = self.socket.accept()
        print(f'Connected: {self.client_addr}')
        self._handle_connection()

    def _handle_connection(self):
        for obs in self.connection_state_observers:
            obs.connection_established(self.client)

        print("waiting for secure channel")
        self.tls_handler.await_secure_channel(
            self.session_handler.get_session(self.client))
        print("ok secure channel estb")
        self.auth.await_authentication(self.client)
        print("OK auth estb")
        self.msg_handler.add_conn_state_obs(self.streamer)
        self.streamer.stream()

    def send_ss(self, ss_base64: str):
        self.sender.send_json(MsgCode.SSHOT, {'image': ss_base64})

    def send_audio_frame(self, frame64: str):
        self.sender.send_json(MsgCode.AUDIO_FRAME, {'frame': frame64})

    def send_audio_bytes(self, audio_frame: bytes):
        self.media_handler.send_audio_bytes(audio_frame)

    def auth_suceeded(self, client_socket: socket):
        self.sender.send_json(MsgCode.AUTH_CHECKED, {'is_granted': True})

    def auth_failed(self, client_socket: socket):
        self.sender.send_json(MsgCode.AUTH_CHECKED, {'is_granted': False})


def main():
    # TODO IOC
    path = Path(__file__).parent.joinpath('.env')
    config = dotenv_values(path)

    auth = Authenticator(config['PASSWORD'])
    msg_handler = StreamMsgHandler(auth)
    ss_capturer = SSCapturer()

    asym_handler = RSA_AsymmetricSecurityHandler(
        Path(config["PRIVATE_KEY_PATH"]))

    tcp_guard = TCPGuard(asym_handler)
    session_handler = SessionHandler()
    tls_handler = TLSHandler(
        Path(config['CERTIFICATE_PATH']), msg_handler, tcp_guard, session_handler)

    sound_args = [int(config['AUDIO_' + arg])
                  for arg in ('CHUNK_SIZE', 'SAMPLE_RATE', 'CHANNELS')]
    sound_capturer = SoundCapturer(
        config['MUTE_ON_START'] == 'false', *sound_args)

    server = Server(config['IP_ADDR'], int(config['TCP_PORT']), int(
        config['UDP_PORT']), auth, msg_handler, tls_handler, session_handler)
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
            msg_handler.remove_conn_state_obs(streamer)
            streamer.mute_sound()
        except Exception as e:
            traceback.print_exc()
            break


if __name__ == '__main__':
    main()
