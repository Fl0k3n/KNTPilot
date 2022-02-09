from datetime import date
from dateutil.relativedelta import relativedelta
from security.certificate import Certificate
from security.key_generator import KeyGenerator
from security.primitives import AsymmetricAlgorithm, AsymmetricParams, HashAlgorithm, PublicKey, RSA_PSS_SignatureParams, RSA_AsymmetricParams, RSA_PublicKey, SignatureAlgorithm, SignatureParams
from Crypto.PublicKey import RSA
from Crypto.Hash import SHA256
from Crypto.Signature import pss


class CertificateAuthority:
    SIGNATURE_ALGORITHM = SignatureAlgorithm.RSASSA_PSS
    HASH_ALGORITHM = HashAlgorithm.SHA256
    SALT_LEN = 20
    VALID_FOR_YEARS = 1

    def __init__(self, CA_keys_filename: str, CA_keys_params: RSA_AsymmetricParams):
        self.CA_keys_filename = CA_keys_filename
        self.CA_keys_params = CA_keys_params
        self.signature_params = RSA_PSS_SignatureParams(
            self.SALT_LEN, self.HASH_ALGORITHM)

    def _load_CA_keys(self) -> RSA.RsaKey:
        try:
            return KeyGenerator.load_RSA_key(self.CA_keys_filename)
        except FileNotFoundError:
            print(
                f'CA keys not found, generating new to {self.CA_keys_filename}')
            return KeyGenerator.generate_RSA_key(self.CA_keys_filename, self.CA_keys_params.get_key_bit_len())

    def _sign_certificate(self, cert: Certificate) -> bytes:
        hashed_cert = SHA256.new(cert.encode())

        key = self._load_CA_keys()
        signature = pss.new(key, salt_bytes=self.SALT_LEN).sign(hashed_cert)

        return signature

    def generate_certificate(self, public_key: PublicKey, public_key_params: AsymmetricParams,
                             algorithm: AsymmetricAlgorithm) -> Certificate:
        start_date = date.today()
        end_date = start_date + relativedelta(years=+self.VALID_FOR_YEARS)

        cert = Certificate(start_date, end_date,
                           self.SIGNATURE_ALGORITHM, self.signature_params,
                           algorithm, public_key_params, public_key)

        signature = self._sign_certificate(cert)

        cert.set_signature(signature)

        return cert
