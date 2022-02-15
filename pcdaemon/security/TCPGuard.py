from security.asymmetric_security_handler import AsymmetricSecurityHandler
from security.session import Session
from Crypto.Cipher import AES


class TCPGuard:
    TAG_LEN = 16
    NONCE_LEN = 12

    def __init__(self, asymmetricHandler: AsymmetricSecurityHandler):
        self.asymmetricHandler = asymmetricHandler

    def encrypt_message(self, session: Session, message: bytes, aad: bytes, nonce: bytes) -> bytes:
        # just data, use aad internally
        key = session.get_tcp_secret_key()
        cipher = AES.new(key, AES.MODE_GCM, nonce=nonce)
        cipher.update(aad)
        ciphertext, tag = cipher.encrypt_and_digest(message)

        return ciphertext + tag

    def decrypt_message(self, session: Session, message: bytes, aad: bytes, nonce: bytes) -> bytes:
        key = session.get_tcp_secret_key()
        ciphertext, tag = message[:-self.TAG_LEN], message[-self.TAG_LEN:]
        cipher = AES.new(key, AES.MODE_GCM, nonce=nonce)
        cipher.update(aad)

        return cipher.decrypt_and_verify(ciphertext, tag)

    def decrypt_secret_key(self, encrypted_key: bytes):
        # TODO handle invalid key
        return self.asymmetricHandler.decrypt(encrypted_key)

    def _assert_valid_key(self, key: bytes):
        pass

    def get_nonce_length(self) -> int:
        return self.NONCE_LEN

    def get_tag_length(self) -> int:
        return self.TAG_LEN
