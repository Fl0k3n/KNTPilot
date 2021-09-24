from abc import ABC, abstractmethod
from networking.conn_state_obs import ConnectionStateObserver
from typing import Any, List
from utils.msg_codes import MsgCode


class MsgHandler(ABC):
    def __init__(self) -> None:
        self.conn_state_obss: List[ConnectionStateObserver] = []

    @abstractmethod
    def handle_msg(self, code: MsgCode, data: Any):
        pass

    @abstractmethod
    def rcving_failed(self, err: Exception):
        pass

    def add_conn_state_obs(self, obs: ConnectionStateObserver):
        self.conn_state_obss.append(obs)

    def remove_conn_state_obs(self, obs: ConnectionStateObserver):
        try:
            self.conn_state_obss.remove(obs)
        except ValueError:
            pass
