import bcrypt
import threading
from typing import List
from security.session import Session
from utils.auth_state_obs import AuthStateObserver
from networking.abstract.conn_state_obs import ConnectionStateObserver


class Authenticator(ConnectionStateObserver):
    def __init__(self, hashed_password: str) -> None:
        self.pswd = hashed_password.encode('utf-8')
        self.auth_state_obss: List[AuthStateObserver] = []

        self.auth_lock = threading.Lock()
        self.auth_state_changed = threading.Condition(self.auth_lock)
        self.connected = False

    def add_auth_state_obs(self, obs: AuthStateObserver):
        self.auth_state_obss.append(obs)

    def validate(self, password: str, session: Session):
        is_valid = bcrypt.checkpw(
            password.encode(encoding='utf-8'), self.pswd)

        with self.auth_lock:
            session.set_auth_state(is_valid)
            # call from listenning thread
            for obs in self.auth_state_obss:
                if is_valid:
                    obs.auth_suceeded(session)
                else:
                    obs.auth_failed(session)

            self.auth_state_changed.notify_all()

    def await_authentication(self, session: Session):
        with self.auth_lock:
            while not session.is_authenticated():
                self.auth_state_changed.wait()
                if not self.connected:
                    raise ConnectionError('Auth awaiting interrupted')

    def connection_established(self, session: Session):
        self.connected = True

    def connection_lost(self, session: Session):
        with self.auth_lock:
            self.connected = False
            session.set_auth_state(False)
            self.auth_state_changed.notify_all()

    @staticmethod
    def hash_password(passwd: str):
        return bcrypt.hashpw(passwd.encode(encoding='utf-8'), bcrypt.gensalt())
