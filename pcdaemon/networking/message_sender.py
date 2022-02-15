import json
from socket import socket
from threading import Lock
from typing import Any
from networking.abstract.conn_state_obs import ConnectionStateObserver
from security.tls_handler import TLSHandler
from utils.msg_codes import MsgCode


class MessageSender(ConnectionStateObserver):
    def __init__(self, tls_handler: TLSHandler, client_socket: socket = None):
        self.client_socket = client_socket
        self.tls_handler = tls_handler
        self.sender_lock = Lock()

    def send_json(self, code: MsgCode, body: Any):
        msg_data = json.dumps({
            'code': code.value,
            'body': body
        })

        msg = self.tls_handler.preprocess_message(
            self.client_socket, msg_data.encode('utf-8'))

        with self.sender_lock:
            self.client_socket.send(msg)

    def connection_established(self, client_socket: socket):
        with self.sender_lock:
            self.client_socket = client_socket

    def connection_lost(self, client_socket: socket):
        with self.sender_lock:
            self.client_socket = None
