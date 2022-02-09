from enum import Enum
from socket import socket
from typing import Any

from networking.message_sender import MessageSender
from utils.msg_codes import TLSCode


class HandshakeState:
    START = 0
    HELLO_RCVD = 1
    CERTIFICATE_SENT = 2
    KEY_RECEIVED = 3
    ACK_SENT = 4


class TLSHandler:
    """ Considerably simplified TLS-alike class used to establish secure channel with client.
    """

    def __init__(self, sender: MessageSender) -> None:
        pass

    def await_secure_channel(self):
        pass

    def handle_tls_message(self, code: TLSCode, data: Any):
        pass

    def is_secure_channel_established(self, client_socket: socket):
        return False

    def preprocess_message(self):
        pass
