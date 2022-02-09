from enum import Enum
from abc import ABC, abstractmethod


class HashAlgorithm(Enum):
    SHA256 = "SHA256"


class AsymmetricAlgorithm(Enum):
    RSA = "RSA"


class SignatureAlgorithm(Enum):
    RSASSA_PSS = "RSASSA_PSS"


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


class RSA_AsymmetricParams(AsymmetricParams):
    def __init__(self, key_bit_len: int):
        self.key_bit_len = key_bit_len

    def get_key_bit_len(self):
        return self.key_bit_len

    def get_as_dict(self):
        return {
            "key_bit_len": self.key_bit_len
        }


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
            'n': self.n,
            'e': self.e
        }
