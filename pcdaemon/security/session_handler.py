
from socket import socket
from typing import Dict
from networking.abstract.conn_state_obs import ConnectionStateObserver

from security.session import Session, SessionState


class SessionHandler(ConnectionStateObserver):
    def __init__(self) -> None:
        self.sessions: Dict[socket, Session] = {}

    def get_session(self, client_socket: socket) -> Session:
        return self.sessions[client_socket]

    def connection_established(self, client_socket: socket):
        self.sessions[client_socket] = Session(client_socket)

    def connection_lost(self, client_socket: socket):
        self.sessions.pop(client_socket).set_session_state(SessionState.LOST)
