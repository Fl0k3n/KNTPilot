from abc import ABC, abstractmethod


class Guard(ABC):
    @abstractmethod
    def encrypt(self, key: bytes, message: bytes, aad: bytes, nonce: bytes) -> bytes:
        """Encrypts message using given key and nonce, AAD is guarded by mac tag and appended to the end of encrypted message.
        """
        pass

    @abstractmethod
    def decrypt(self, key: bytes, message: bytes, aad: bytes, nonce: bytes) -> bytes:
        """Decrypts message using given key and nonce and verifies it and AAD using MAC
           tag which is assumed to be appended to the end of encrypted message.
        """
        pass

    @abstractmethod
    def get_nonce_length(self) -> int:
        pass

    @abstractmethod
    def get_tag_length(self) -> int:
        pass

    @abstractmethod
    def get_nonce(self) -> bytes:
        pass
