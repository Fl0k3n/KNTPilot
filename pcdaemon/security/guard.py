from abc import ABC, abstractmethod
from Crypto.Random import get_random_bytes

from security.session import Session


class Guard(ABC):
    def __init__(self, nonce_length: int, tag_length: int, key_length: int) -> None:
        self.nonce_length = nonce_length
        self.tag_length = tag_length
        self.key_length = key_length

    @abstractmethod
    def encrypt(self, key: bytes, message: bytes, aad: bytes, nonce: bytes) -> bytes:
        """Encrypts message using given key and nonce, AAD is guarded by mac tag and appended to the end of encrypted message.
        """
        pass

    @abstractmethod
    def decrypt(self, key: bytes, message: bytes, aad: bytes, nonce: bytes) -> bytes:
        """Decrypts message using given key and nonce, verifies it with AAD using MAC
           tag which is assumed to be appended to the end of encrypted message.
        """
        pass

    @abstractmethod
    def get_secret_key(self, session: Session) -> bytes:
        pass

    def get_nonce_length(self) -> int:
        return self.nonce_length

    def get_tag_length(self) -> int:
        return self.tag_length

    def get_nonce(self) -> bytes:
        return get_random_bytes(self.get_nonce_length())

    def get_key_length(self) -> int:
        return self.key_length
