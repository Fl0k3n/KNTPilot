import base64
import json
from datetime import date
from pathlib import Path
from security.primitives import AsymmetricAlgorithm, AsymmetricParams, SignatureAlgorithm, PublicKey, SignatureParams


class Certificate:
    DATE_FORMAT = '%d-%m-%Y'

    def __init__(self, not_before_date: date, not_after_date: date,
                 certificate_algorithm: SignatureAlgorithm, certificate_params: SignatureParams,
                 subject_algorithm: AsymmetricAlgorithm, subject_params: AsymmetricParams,
                 subject_public_key: PublicKey, signature: bytes = None):
        self.not_before_date = not_before_date
        self.not_after_date = not_after_date

        self.certificate_algorithm = certificate_algorithm
        self.certificate_params = certificate_params

        self.subject_algorithm = subject_algorithm
        self.subject_params = subject_params
        self.subject_public_key = subject_public_key

        self.signature = signature

    def set_signature(self, signature: bytes):
        self.signature = signature

    def is_signed(self) -> bool:
        return self.signature is not None

    def encode(self) -> bytes:
        return base64.b64encode(self._convert_to_json().encode('utf-8'))

    def save_as_json(self, path: Path):
        with open(path, 'w') as f:
            f.write(self._convert_to_json())

    def _convert_to_json(self) -> str:
        res = {
            'not_before_date': self.not_before_date.strftime(self.DATE_FORMAT),
            'not_after_date': self.not_after_date.strftime(self.DATE_FORMAT),
            'certificate_algorithm': self.certificate_algorithm.value,
            'certificate_params': self.certificate_params.get_as_dict(),
            'subject_algorithm': self.subject_algorithm.value,
            'subject_params': self.subject_params.get_as_dict(),
            'subject_public_key': self.subject_public_key.get_as_dict()
        }

        if self.is_signed():
            res['signature'] = base64.b64encode(self.signature).decode('ascii')

        json_cert = json.dumps(res)
        return json_cert.replace(' ', '')
