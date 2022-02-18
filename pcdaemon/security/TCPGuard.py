from security.guard import Guard
from Crypto.Cipher import AES

from networking.session import Session


class TCPGuard(Guard):
    _TAG_LEN = 16
    _NONCE_LEN = 12
    _KEY_LENGTH = 32

    def __init__(self):
        super().__init__(self._NONCE_LEN, self._TAG_LEN, self._KEY_LENGTH)

    def encrypt(self, key: bytes, message: bytes, aad: bytes, nonce: bytes) -> bytes:
        # just data, use aad internally
        cipher = AES.new(key, AES.MODE_GCM, nonce=nonce)
        cipher.update(aad)
        ciphertext, tag = cipher.encrypt_and_digest(message)

        return ciphertext + tag

    def decrypt(self, key: bytes, message: bytes, aad: bytes, nonce: bytes) -> bytes:
        ciphertext, tag = message[:-self._TAG_LEN], message[-self._TAG_LEN:]
        cipher = AES.new(key, AES.MODE_GCM, nonce=nonce)
        cipher.update(aad)

        return cipher.decrypt_and_verify(ciphertext, tag)

    def get_secret_key(self, session: Session) -> bytes:
        return session.get_tcp_secret_key()
