from abc import ABC, abstractmethod


class SsSender(ABC):
    @abstractmethod
    def send_ss(self, ss_base64: str):
        pass

    @abstractmethod
    def send_ss_bytes(self, ss: bytes):
        pass
