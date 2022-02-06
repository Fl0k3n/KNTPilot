import bcrypt
import threading
from socket import socket
from typing import Dict, List
from collections import defaultdict
from utils.auth_state_obs import AuthStateObserver
from networking.abstract.conn_state_obs import ConnectionStateObserver


class Authenticator(ConnectionStateObserver):
    def __init__(self, hashed_password: str) -> None:
        self.pswd = hashed_password.encode('utf-8')
        # TODO only one is needed
        self.auths: Dict[socket, bool] = defaultdict(lambda: False)
        self.auth_state_obss: List[AuthStateObserver] = []

        self.auth_lock = threading.Lock()
        self.auth_state_changed = threading.Condition(self.auth_lock)
        self.connected = False

    def add_auth_state_obs(self, obs: AuthStateObserver):
        self.auth_state_obss.append(obs)

    def validate(self, password: str, client_socket: socket):
        with self.auth_lock:
            is_valid = bcrypt.checkpw(
                password.encode(encoding='utf-8'), self.pswd)

            self.auths[client_socket] = is_valid
            # call from listenning thread
            for obs in self.auth_state_obss:
                if is_valid:
                    obs.auth_suceeded(client_socket)
                else:
                    obs.auth_failed(client_socket)

            self.auth_state_changed.notify_all()

    def await_authentication(self, client_socket: socket):
        with self.auth_lock:
            while not self.auths[client_socket]:
                self.auth_state_changed.wait()
                if not self.connected:
                    raise ConnectionError('Auth awaiting interrupted')

    def is_validated(self, client_socket: socket) -> bool:
        with self.auth_lock:
            return self.auths[client_socket]

    def connection_established(self, client_socket: socket):
        self.connected = True

    def connection_lost(self, client_socket: socket):
        with self.auth_lock:
            self.connected = False
            self.auths[client_socket] = False
            self.auth_state_changed.notify_all()

    @staticmethod
    def hash_password(passwd: str):
        return bcrypt.hashpw(passwd.encode(encoding='utf-8'), bcrypt.gensalt())
