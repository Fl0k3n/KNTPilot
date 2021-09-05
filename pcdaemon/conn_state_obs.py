from abc import ABC, abstractmethod
from authenticator import Authenticator
from socket import socket


class ConnectionStateObserver(ABC):
    @abstractmethod
    def connection_established(self, client_socket: socket):
        pass

    @abstractmethod
    def connection_lost(self, client_socket: socket):
        pass
