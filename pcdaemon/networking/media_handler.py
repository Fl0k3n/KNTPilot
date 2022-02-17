import atexit
import socket
from socket import AF_INET, SOCK_DGRAM
from networking.abstract.conn_state_obs import ConnectionStateObserver
from networking.media_sender import DataSender
from security.message_security_preprocessor import MessageSecurityPreprocessor
from security.session import Session


class MediaHandler(ConnectionStateObserver):
    def __init__(self, ip_addr: str, port: int, msg_security_preproc: MessageSecurityPreprocessor):
        self.ip_addr = ip_addr
        self.port = port
        self.msg_security_preproc = msg_security_preproc
        self.remote_port = self.port
        self.data_sender = None

        self.serv_sock = self._setup_server_socket()

    def connection_established(self, session: Session):
        client_tcp_socket = session.get_tcp_socket()
        ip_addr = client_tcp_socket.getpeername()[0]

        session.set_udp_socket(self.serv_sock)
        session.set_udp_secret_key(self.msg_security_preproc.generate_key())
        session.set_udp_peer_addr(ip_addr, self.remote_port)

        self.data_sender = DataSender(session, self.msg_security_preproc)

    def connection_lost(self, session: Session):
        self.data_sender = None

    def _setup_server_socket(self):
        sock = socket.socket(AF_INET, SOCK_DGRAM)
        atexit.register(sock.close)

        return sock

    def send_audio_bytes(self, audio_frame: bytes):
        self.data_sender.send_audio_frame(audio_frame)

    def send_video_bytes(self, video_frame: bytes):
        self.data_sender.send_video_frame(video_frame)
