import json
from pathlib import Path
from struct import pack, unpack
from socket import socket
from typing import Any, Tuple
from threading import Lock, Condition
from networking.abstract.conn_state_obs import ConnectionStateObserver
from networking.abstract.msg_handler import MsgHandler
from security.TCPGuard import TCPGuard
from security.session import Session, SessionState
from security.session_handler import SessionHandler
from utils.msg_codes import MsgCode, TLSCode
from Crypto.Random import get_random_bytes


'''
TlS('ish) Packet
  |--------------------------------------------|
  |  tls_code(8)|    size(16)   | nonce_len(8) |
  |--------------------------------------------|
  {                                            }
  {             nonce(nonce_len * 8)           }
  {                                            }
  |--------------------------------------------|
  {                                            }
  {               data(size * 8)               }
  {                                            }
  |--------------------------------------------|
'''

# TODO error handling


class TLSHandler(ConnectionStateObserver):
    """ Considerably simplified TLS-alike class used to establish secure channel with client.
    """
    TCP_HEADER_SIZE = 4
    TCP_HEADER_FORMAT = ">BhB"

    def __init__(self, certificate_path: Path, msg_handler: MsgHandler,
                 tcp_guard: TCPGuard, session_handler: SessionHandler):
        self.certificate_path = certificate_path
        self.msg_handler = msg_handler
        self.tcp_guard = tcp_guard
        self.session_handler = session_handler

        self.security_change_mutex = Lock()
        self.security_changed = Condition(self.security_change_mutex)

        # TODO taken from authenticator, replace it
        self.connected = False

    def get_basic_tls_header_size(self) -> int:
        return self.TCP_HEADER_SIZE

    def await_secure_channel(self, session: Session):
        with self.security_change_mutex:
            while not session.is_secure():
                self.security_changed.wait()
                # TODO
                if not self.connected:
                    raise ConnectionError('TLS awaiting interrupted')

    def _parse_header(self, packet: bytes) -> Tuple[int, int, int]:
        return unpack(self.TCP_HEADER_FORMAT, packet[:self.TCP_HEADER_SIZE])

    def _extract_nonce_size(self, packet: bytes) -> int:
        return self._parse_header(packet)[2]

    def _extract_data_size(self, packet: bytes) -> int:
        return self._parse_header(packet)[1]

    def _extract_nonce(self, packet: bytes) -> bytes:
        nonce_size = self._extract_nonce_size(packet)
        return packet[self.TCP_HEADER_SIZE: self.TCP_HEADER_SIZE + nonce_size]

    def get_total_packet_size(self, header: bytes) -> int:
        print(self._parse_header(header))
        code, data_size, nonce_size = self._parse_header(header)
        print("****************************")
        print(data_size, nonce_size)

        packet_length = data_size + nonce_size + self.TCP_HEADER_SIZE

        if TLSCode(code) == TLSCode.SECURE:
            packet_length += self.tcp_guard.get_tag_length()

        return packet_length

    def handle_tls_message(self, client_socket: socket, packet: bytes):
        session = self.session_handler.get_session(client_socket)
        print('packet len', len(packet))
        code = TLSCode(self._parse_header(packet)[0])
        print('got tls: ', packet[:self.TCP_HEADER_SIZE])

        separator_idx = self.TCP_HEADER_SIZE + self._extract_nonce_size(packet)
        header, data = packet[:separator_idx], packet[separator_idx:]

        if session.is_secure() and code == TLSCode.SECURE:
            nonce = self._extract_nonce(packet)

            decrypted_data = self.tcp_guard.decrypt_message(
                session, data, header, nonce)
            code, body = self._decode_tcp_message(decrypted_data)
            self.msg_handler.handle_msg(code, body)
        else:
            self._update_tls_state_machine(session, code, data)

    def preprocess_message(self, client_tcp_socket: socket, data: bytes) -> bytes:
        # TODO replace socket with session
        session = self.session_handler.get_session(client_tcp_socket)
        assert session.is_secure(), "cant send message via unsecure channel"  # TODO

        print(len(data))

        nonce_length = self.tcp_guard.get_nonce_length()
        nonce = self._get_nonce(nonce_length)

        header = pack(self.TCP_HEADER_FORMAT,
                      TLSCode.SECURE.value, len(data), nonce_length) + nonce
        return header + self.tcp_guard.encrypt_message(session, data, header, nonce)

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

        header = pack(self.TCP_HEADER_FORMAT,
                      TLSCode.CERTIFICATE.value, len(cert), 0)

        sock = session.get_tcp_socket()
        sock.send(header + cert)

        print('sent certificate')

        session.set_session_state(SessionState.CERTIFICATE_SENT)

    def _establish_secure_connection(self, session: Session, encrypted_secret_key: bytes):
        secret_key = self.tcp_guard.decrypt_secret_key(encrypted_secret_key)
        session.set_tcp_secret_key(secret_key)
        session.set_session_state(SessionState.ESTABLISHED)

        with self.security_change_mutex:
            self.security_changed.notify_all()

    def connection_established(self, client_socket: socket):
        self.connected = True

    def connection_lost(self, client_socket: socket):
        with self.security_change_mutex:
            self.connected = False
            self.security_changed.notify_all()

    def _get_nonce(self, len: int):
        return get_random_bytes(len)
