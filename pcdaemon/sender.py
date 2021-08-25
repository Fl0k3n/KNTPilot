from conn_state_obs import ConnectionStateObserver
from typing import Any
from msg_codes import MsgCode
from socket import socket
import json


class Sender(ConnectionStateObserver):
    def __init__(self, client_socket: socket = None, header_size: int = 10):
        self.client_socket = client_socket
        self.header_size = header_size

    def send_json(self, code: MsgCode, body: Any):
        msg_data = json.dumps({
            'code': code.value,
            'body': body
        })

        msg = f'{len(msg_data): <{self.header_size}}{msg_data}'
        print(msg[:100])
        print("*"*100)

        self.client_socket.send(msg.encode('utf-8'))

    def connection_established(self, client_socket: socket):
        self.client_socket = client_socket

    def connection_lost(self, client_socket: socket):
        self.client_socket = None
