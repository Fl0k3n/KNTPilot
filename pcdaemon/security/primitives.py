from enum import Enum
from abc import ABC, abstractmethod


class HashAlgorithm(Enum):
    SHA256 = "SHA-256"


class AsymmetricAlgorithm(Enum):
    RSA = "RSA"


class SignatureAlgorithm(Enum):
    RSA_PKCS15 = "RSA-PKCS1.5"
    RSASSA_PSS = "RSASSA-PSS"


class SymmetricAlgorithm(Enum):
    AES = "AES"
    CHACHA20 = "CHACHA20"


class AsymmetricParams(ABC):
    @abstractmethod
    def get_as_dict(self):
        pass


class PublicKey(ABC):
    @abstractmethod
    def get_as_dict(self):
        pass


class SignatureParams(ABC):
    @abstractmethod
    def get_as_dict(self):
        pass


class SymmetricParams(ABC):
    @abstractmethod
    def get_as_dict(self):
        pass


class RSA_AsymmetricParams(AsymmetricParams):
    def __init__(self, key_bit_len: int):
        self.key_bit_len = key_bit_len

    def get_key_bit_len(self):
        return self.key_bit_len

    def get_as_dict(self):
        return {
            "key_bit_len": self.key_bit_len
        }


class EmptyParams(SignatureParams):
    def get_as_dict(self):
        return {}


class RSA_PSS_SignatureParams(SignatureParams):
    def __init__(self, salt_len_bytes: int, hash_algorithm: HashAlgorithm):
        self.salt_len_bytes = salt_len_bytes
        self.hash_algorithm = hash_algorithm
        self.mask_function = "MGF1"

    def get_as_dict(self):
        return {
            **self.__dict__,
            "hash_algorithm": self.hash_algorithm.value
        }


class RSA_PublicKey(PublicKey):
    def __init__(self, n: int, e: int):
        self.n = n
        self.e = e

    def get_as_dict(self):
        return {
            'n': str(self.n),
            'e': str(self.e)
        }


class AES_SymmetricParams(SymmetricParams):
    def __init__(self, key_size: int) -> None:
        self.key_size = key_size

    def get_as_dict(self):
        return self.__dict__
