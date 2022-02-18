from enum import Enum
from pathlib import Path
from Crypto.Protocol.KDF import scrypt
from Crypto.Random import get_random_bytes
from Crypto.PublicKey import RSA


class SecurityLevel(Enum):
    SESSION = 1,
    LONG_TERM = 4


class KeyGenerator:
    RSA_KEY_FORMAT = "PEM"

    @staticmethod
    def deriveKey(password: str, key_len, sec_lvl: SecurityLevel, salt_len: int = 32) -> bytes:
        salt = get_random_bytes(salt_len)

        if sec_lvl == SecurityLevel.SESSION:
            N, r, p = 2 ** 14, 8, 1
        elif sec_lvl == SecurityLevel.LONG_TERM:
            N, r, p = 2 ** 20, 8, 1

        return scrypt(password, salt, key_len, N=N, r=r, p=p, num_keys=1)

    @classmethod
    def generate_RSA_key(cls, public_path: Path, private_path: Path, bit_len: int, public_exp: int = 65537) -> RSA.RsaKey:
        cls._assert_rsa_filename(private_path)

        key = RSA.generate(bit_len, e=public_exp)

        with open(private_path, 'wb') as f:
            f.write(key.export_key(cls.RSA_KEY_FORMAT))

        with open(public_path, 'wb') as f:
            f.write(key.public_key().export_key(cls.RSA_KEY_FORMAT))

        return key

    @classmethod
    def load_RSA_key(cls, path: Path) -> RSA.RsaKey:
        """ Raises FileNotFoudException"""
        cls._assert_rsa_filename(path)

        with open(path, 'rb') as f:
            return RSA.import_key(f.read())

    @classmethod
    def _assert_rsa_filename(cls, path: Path):
        assert path.name.endswith(cls.RSA_KEY_FORMAT.lower(
        )), f"only {cls.RSA_KEY_FORMAT} files are supported"

    @staticmethod
    def generate_secret_key(length_bytes: int) -> bytes:
        return get_random_bytes(length_bytes)

    @staticmethod
    def verify_password(password: str) -> bool:
        pass
