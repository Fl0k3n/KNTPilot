from abc import ABC, abstractmethod
from typing import Any
from msg_codes import MsgCode


class MsgHandler(ABC):
    @abstractmethod
    def handle_msg(self, code: MsgCode, data: Any):
        pass

    @abstractmethod
    def rcving_failed(self, err: Exception):
        pass
