from security.guard import Guard
from security.session import Session
from security.tls_packet import TLSPacket
from utils.msg_codes import TLSCode


class MessageSecurityPreprocessor:

    def __init__(self, guard: Guard):
        self.guard = guard

    def preprocess_to_send(self, session: Session, message_to_send: bytes) -> bytes:
        nonce = self.guard.get_nonce()

        tls_packet = TLSPacket(TLSCode.SECURE, len(
            message_to_send), len(nonce), nonce, message_to_send)

        encrypted_data = self.guard.encrypt(
            session.get_tcp_secret_key(), tls_packet.data, tls_packet.header, nonce)

        return TLSPacket.build_from_header_and_data(tls_packet.header, encrypted_data).full

    def preprocess_received(self, session: Session, received_message: bytes) -> bytes:
        tls_packet = TLSPacket.build_from_raw(received_message)

        assert len(tls_packet.nonce) == self.guard.get_nonce_length()

        return self.guard.decrypt(session.get_tcp_secret_key(), tls_packet.data, tls_packet.header, tls_packet.nonce)

    def get_basic_header_size(self) -> int:
        return TLSPacket.HEADER_SIZE

    def get_message_size(self, basic_header: bytes) -> int:
        return TLSPacket.get_packet_size(basic_header, self.guard.get_tag_length())
