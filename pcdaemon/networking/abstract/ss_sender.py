from abc import ABC, abstractmethod


class SsSender(ABC):
    @abstractmethod
    def send_ss(self, ss_base64: str):
        pass
