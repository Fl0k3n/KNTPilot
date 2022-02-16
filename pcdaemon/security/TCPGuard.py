from security.guard import Guard
from Crypto.Cipher import AES
from Crypto.Random import get_random_bytes


class TCPGuard(Guard):
    _TAG_LEN = 16
    _NONCE_LEN = 12

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

    def get_nonce_length(self) -> int:
        return self._NONCE_LEN

    def get_tag_length(self) -> int:
        return self._TAG_LEN

    def get_nonce(self) -> bytes:
        return get_random_bytes(self._NONCE_LEN)
