from abc import ABC, abstractmethod
from networking.session import Session


class ConnectionStateObserver(ABC):
    @abstractmethod
    def connection_established(self, session: Session):
        pass

    @abstractmethod
    def connection_lost(self, session: Session):
        pass
