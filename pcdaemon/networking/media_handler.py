import atexit
import socket
from socket import AF_INET, SOCK_DGRAM
from networking.abstract.conn_state_obs import ConnectionStateObserver
from networking.media_sender import DataSender
from security.session import Session


class MediaHandler(ConnectionStateObserver):
    def __init__(self, port: int, ip_addr: str, remote_port: int = None):
        self.port = port
        self.ip_addr = ip_addr
        self.remote_port = self.port if remote_port is None else remote_port

        self.serv_sock = self._setup_server_socket()

        self.data_sender = None

    def connection_established(self, session: Session):
        client_tcp_socket = session.get_tcp_socket()
        ip_addr = client_tcp_socket.getpeername()[0]
        self.data_sender = DataSender(
            self.serv_sock, self.remote_port, ip_addr)

    def connection_lost(self, session: Session):
        # TODO
        pass

    def _setup_server_socket(self):
        sock = socket.socket(AF_INET, SOCK_DGRAM)
        atexit.register(sock.close)

        return sock

    def send_audio_bytes(self, audio_frame: bytes):
        self.data_sender.send_audio_frame(audio_frame)
