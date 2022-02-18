from enum import Enum
from socket import socket
from typing import Tuple


class SessionState(Enum):
    START = 0
    CERTIFICATE_SENT = 1
    ESTABLISHED = 2
    LOST = 3


class Session:
    def __init__(self, client_tcp_socket: socket):
        self.tcp_sock = client_tcp_socket
        self.session_state = SessionState.START

        self.udp_sock = None
        self.udp_peer_ip = None
        self.udp_peer_port = None

        self.tcp_secret_key = None
        self.udp_secret_key = None

        self.authenticated = False

    def get_session_state(self) -> SessionState:
        return self.session_state

    def set_auth_state(self, authenticated: bool):
        self.authenticated = authenticated

    def is_authenticated(self) -> bool:
        return self.authenticated

    def set_tcp_secret_key(self, key: bytes):
        self.tcp_secret_key = key

    def get_tcp_secret_key(self) -> bytes:
        return self.tcp_secret_key

    def set_udp_secret_key(self, key: bytes):
        self.udp_secret_key = key

    def get_udp_secret_key(self) -> bytes:
        return self.udp_secret_key

    def set_session_state(self, session_state: SessionState):
        self.session_state = session_state

    def get_tcp_socket(self) -> socket:
        return self.tcp_sock

    def get_udp_socket(self) -> socket:
        return self.udp_sock

    def set_udp_socket(self, sock: socket):
        self.udp_sock = sock

    def is_secure(self) -> bool:
        return self.session_state == SessionState.ESTABLISHED

    def set_udp_peer_addr(self, ip_addr: str, port: int):
        self.udp_peer_ip = ip_addr
        self.udp_peer_port = port

    def get_udp_peer_addr(self) -> Tuple[str, int]:
        return self.udp_peer_ip, self.udp_peer_port

    def __str__(self) -> str:
        return f'[Peer: {self.tcp_sock.getpeername()} - {self.session_state} - authenticated: {self.authenticated}]'
