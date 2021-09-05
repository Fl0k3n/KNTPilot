from abc import ABC, abstractmethod
from socket import socket


class AuthStateObserver(ABC):
    @abstractmethod
    def auth_suceeded(self, client_socket: socket):
        pass

    @abstractmethod
    def auth_failed(self, client_socket: socket):
        pass
