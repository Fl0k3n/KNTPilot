
from socket import socket
from typing import Dict

from security.session import Session, SessionState


class SessionHandler:
    def __init__(self) -> None:
        self.sessions: Dict[socket, Session] = {}

    def create_session(self, client_socket: socket) -> Session:
        session = Session(client_socket)
        self.sessions[client_socket] = socket
        return session

    def get_session(self, client_socket: socket) -> Session:
        return self.sessions[client_socket]

    def remove_session(self, client_socket: socket):
        self.sessions.pop(client_socket).set_session_state(SessionState.LOST)
