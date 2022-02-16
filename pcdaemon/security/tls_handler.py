import json
from pathlib import Path
from typing import Any, Tuple
from threading import Lock, Condition
from networking.abstract.conn_state_obs import ConnectionStateObserver
from networking.abstract.msg_handler import MsgHandler
from security.TCPGuard import Guard
from security.asymmetric_security_handler import AsymmetricSecurityHandler
from security.session import Session, SessionState
from security.session_handler import SessionHandler
from security.tls_packet import TLSPacket
from utils.msg_codes import MsgCode, TLSCode


# TODO error handling


class TLSHandler(ConnectionStateObserver):
    """ Considerably simplified TLS-alike class used to establish secure channel with client.
    """

    def __init__(self, certificate_path: Path, msg_handler: MsgHandler,
                 guard: Guard, session_handler: SessionHandler, secret_key_decryptor: AsymmetricSecurityHandler):
        self.certificate_path = certificate_path
        self.msg_handler = msg_handler
        self.guard = guard
        self.session_handler = session_handler
        self.secret_key_decryptor = secret_key_decryptor

        self.security_change_mutex = Lock()
        self.security_changed = Condition(self.security_change_mutex)

        # TODO taken from authenticator, replace it
        self.connected = False

    def get_basic_tls_header_size(self) -> int:
        return TLSPacket.HEADER_SIZE

    def await_secure_channel(self, session: Session):
        with self.security_change_mutex:
            while not session.is_secure():
                self.security_changed.wait()
                # TODO
                if not self.connected:
                    raise ConnectionError('TLS awaiting interrupted')

    def handle_tls_message(self, session: Session, packet: bytes):
        tls_packet = TLSPacket.build_from_raw(packet)

        if session.is_secure() and tls_packet.code == TLSCode.SECURE:
            decrypted_data = self.guard.decrypt(
                session.get_tcp_secret_key(), tls_packet.data, tls_packet.header, tls_packet.nonce)
            code, body = self._decode_tcp_message(decrypted_data)
            self.msg_handler.handle_msg(code, body)
        else:
            self._update_tls_state_machine(
                session, tls_packet.code, tls_packet.data)

    def _decode_tcp_message(self, data: bytes) -> Tuple[MsgCode, Any]:
        # TODO move it elsewhere
        msg = json.loads(data.decode('utf-8'))
        return MsgCode(msg['code']), msg['body']

    def _update_tls_state_machine(self, session: Session, code: TLSCode, data: bytes):
        session_state = session.get_session_state()

        if code == TLSCode.HELLO and session_state == SessionState.START:
            self._send_certificate(session)
        elif code == TLSCode.SECRET and session_state == SessionState.CERTIFICATE_SENT:
            self._establish_secure_connection(session, data)
        else:
            print('unexpected code, session status combination',
                  code, session_state)  # TODO

    def _send_certificate(self, session: Session):
        with open(self.certificate_path, 'rb') as f:
            cert = f.read()

        tls_packet = TLSPacket(TLSCode.CERTIFICATE, len(cert), 0, b'', cert)

        sock = session.get_tcp_socket()
        sock.send(tls_packet.full)

        session.set_session_state(SessionState.CERTIFICATE_SENT)

    def _establish_secure_connection(self, session: Session, encrypted_secret_key: bytes):
        secret_key = self.secret_key_decryptor.decrypt(encrypted_secret_key)

        with self.security_change_mutex:
            session.set_tcp_secret_key(secret_key)
            session.set_session_state(SessionState.ESTABLISHED)
            self.security_changed.notify_all()

    def connection_established(self, session: Session):
        self.connected = True

    def connection_lost(self, session: Session):
        with self.security_change_mutex:
            self.connected = False
            self.security_changed.notify_all()
