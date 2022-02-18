from abc import ABC, abstractmethod
from networking.session import Session


class AuthStateObserver(ABC):
    @abstractmethod
    def auth_suceeded(self, session: Session):
        pass

    @abstractmethod
    def auth_failed(self, session: Session):
        pass
