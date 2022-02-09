from networking import server
from security.certificate_authority import CertificateAuthority
from security.key_generator import KeyGenerator
from security.primitives import AsymmetricAlgorithm, RSA_AsymmetricParams, RSA_PublicKey


def generate_certificate():
    rsa_key = KeyGenerator.generate_RSA_key(
        "subject_key.pem", 2048).public_key()
    pub_key = RSA_PublicKey(rsa_key.n, rsa_key.e)

    key_params = RSA_AsymmetricParams(2048)
    algorithm = AsymmetricAlgorithm.RSA

    ca_params = RSA_AsymmetricParams(2048)

    ca = CertificateAuthority("ca_key.pem", ca_params)

    cert = ca.generate_certificate(pub_key, key_params, algorithm)

    cert.save_as_json('certificate.json')


def main():
    server.main()


if __name__ == "__main__":
    main()
