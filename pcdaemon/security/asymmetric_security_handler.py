from abc import ABC, abstractmethod
from pathlib import Path
from Crypto.PublicKey import RSA
from Crypto.Cipher import PKCS1_OAEP


class AsymmetricSecurityHandler(ABC):
    @abstractmethod
    def decrypt(self, data: bytes):
        pass


class RSA_AsymmetricSecurityHandler(AsymmetricSecurityHandler):
    SUPPORTED_EXTENCTION = "pem"

    def __init__(self, private_key_path: Path):
        self.private_key_path = private_key_path

        assert self.private_key_path.name.endswith(
            self.SUPPORTED_EXTENCTION), f"unsupported extension, expected {self.SUPPORTED_EXTENCTION}"

    def decrypt(self, data: bytes) -> bytes:
        # since this will likely be used just once
        # no need to cache this key(even better if it doesnt reside in RAM longer)

        with open(self.private_key_path, 'r') as f:
            key = RSA.import_key(f.read())

        cipher = PKCS1_OAEP.new(key)
        return cipher.decrypt(data)
