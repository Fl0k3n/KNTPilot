from security.asymmetric_security_handler import AsymmetricSecurityHandler
from security.session import Session


class TCPGuard:
    def __init__(self, asymmetricHandler: AsymmetricSecurityHandler):
        self.asymmetricHandler = asymmetricHandler

    def encrypt_message(self, session: Session, aad: bytes, data: bytes) -> bytes:
        # just data, use aad internally
        return data

    def decrypt_message(self, session: Session, aad: bytes, data: bytes) -> bytes:
        return data

    def decrypt_secret_key(self, encrypted_key: bytes):
        # TODO handle invalid key
        return self.asymmetricHandler.decrypt(encrypted_key)

    def _assert_valid_key(self, key: bytes):
        pass
