import json
from threading import Lock
from typing import Any
from networking.abstract.conn_state_obs import ConnectionStateObserver
from security.message_security_preprocessor import MessageSecurityPreprocessor
from security.session import Session
from utils.msg_codes import MsgCode


class MessageSender(ConnectionStateObserver):
    def __init__(self, preprocessor: MessageSecurityPreprocessor):
        self.session = None
        self.preprocessor = preprocessor
        self.sender_lock = Lock()

    def send_json(self, code: MsgCode, body: Any):
        msg_data = json.dumps({
            'code': code.value,
            'body': body
        })

        session = self.session
        msg = self.preprocessor.preprocess_to_send(
            session, msg_data.encode('utf-8'))

        with self.sender_lock:
            session.get_tcp_socket().send(msg)

    def connection_established(self, session: Session):
        with self.sender_lock:
            self.session = session

    def connection_lost(self, session: Session):
        with self.sender_lock:
            self.session = None
