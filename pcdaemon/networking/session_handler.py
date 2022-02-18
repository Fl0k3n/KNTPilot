from socket import socket
from typing import Dict

from networking.session import Session, SessionState


class SessionHandler:
    def __init__(self) -> None:
        self.sessions: Dict[socket, Session] = {}

    def create_session(self, client_socket: socket) -> Session:
        session = Session(client_socket)
        self.sessions[client_socket] = session
        return session

    def get_session(self, client_socket: socket) -> Session:
        return self.sessions[client_socket]

    def close_session(self, client_socket: socket):
        try:
            self.sessions.pop(client_socket).set_session_state(
                SessionState.LOST)
        finally:
            client_socket.close()
