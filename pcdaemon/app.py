import dotenv
import click
import logging
from pathlib import Path
from dotenv import dotenv_values
from networking.server import Server
from networking.message_sender import MessageSender
from networking.session_handler import SessionHandler
from networking.stream_msg_handler import StreamMsgHandler
from networking.message_listener import MessageListener
from networking.media_handler import MediaHandler
from security.certificate_authority import CertificateAuthority
from security.key_generator import KeyGenerator
from security.primitives import AsymmetricAlgorithm, RSA_AsymmetricParams, RSA_PublicKey
from security.UDPGuard import UDPGuard
from security.asymmetric_security_handler import RSA_AsymmetricSecurityHandler
from security.message_security_preprocessor import MessageSecurityPreprocessor
from security.authenticator import Authenticator
from security.TCPGuard import TCPGuard
from security.tls_handler import TLSHandler
from media.streamers.streamer import Streamer
from media.ss_capturer import SSCapturer
from media.sound_capturer import SoundCapturer

path = Path(__file__).parent.joinpath('.env')
config = dotenv_values(path)

ip_addr = config['IP_ADDR']
tcp_port = int(config['TCP_PORT'])
udp_port = int(config['UDP_PORT'])

password = config['PASSWORD']
private_key_path = Path(config["PRIVATE_KEY_PATH"])
certificate_path = Path(config['CERTIFICATE_PATH'])

sound_args = [int(config['AUDIO_' + arg])
              for arg in ('CHUNK_SIZE', 'SAMPLE_RATE', 'CHANNELS')]
mute_on_start = config['MUTE_ON_START'] == 'false'
max_fps = int(config['MAX_FPS'])


def setup_logger():
    logger_format = '[%(filename)s:%(lineno)d] %(levelname)-8s %(message)s'
    logging.basicConfig(level=logging.INFO, format=logger_format)


@click.group()
def cli():
    pass


@click.command(name="passwd", help="set password used to connect to desktop from phone")
@click.argument('password')
def set_server_password(password: str):
    hashed = Authenticator.hash_password(password).decode(encoding='ascii')

    dotenv.set_key(path, "PASSWORD", hashed)
    click.echo(f'Saved using key: "PASSWORD"')


@click.command(name="gencert", help="generate public key certificate")
def genereate_certificate():
    click.echo('generating certificate...')
    public_key_path = Path(config["PUBLIC_KEY_PATH"])
    key_len = int(config["ASYMMETRIC_KEY_LEN"])

    ca_public_key_path = Path(config["CA_PUBLIC_KEY_PATH"])
    ca_private_key_path = Path(config["CA_PRIVATE_KEY_PATH"])

    rsa_key = KeyGenerator.generate_RSA_key(
        public_key_path, private_key_path, key_len).public_key()
    pub_key = RSA_PublicKey(rsa_key.n, rsa_key.e)

    key_params = RSA_AsymmetricParams(key_len)
    algorithm = AsymmetricAlgorithm.RSA

    ca_params = RSA_AsymmetricParams(key_len)

    ca = CertificateAuthority(
        ca_public_key_path, ca_private_key_path, ca_params)

    cert = ca.generate_certificate(pub_key, key_params, algorithm)

    cert.save_as_json(certificate_path)

    click.echo(f'''
certificate saved at: {certificate_path}
public key saved at:  {public_key_path}
private key saved at: {private_key_path}''')


@click.command(name='serve', help="run server")
def run_server():
    setup_logger()
    # ----- init security
    auth = Authenticator(password)
    asym_handler = RSA_AsymmetricSecurityHandler(private_key_path)
    tcp_guard = TCPGuard()
    udp_guard = UDPGuard()
    tcp_preprocessor = MessageSecurityPreprocessor(tcp_guard)
    udp_preprocessor = MessageSecurityPreprocessor(udp_guard)
    tls_handler = TLSHandler(certificate_path, tcp_guard, asym_handler)

    # ----- init communication and media
    media_handler = MediaHandler(ip_addr, udp_port, udp_preprocessor)
    session_handler = SessionHandler()

    sound_capturer = SoundCapturer(mute_on_start, *sound_args)
    ss_capturer = SSCapturer()
    streamer = Streamer(media_handler, ss_capturer,
                        sound_capturer, max_fps=max_fps)

    msg_handler = StreamMsgHandler(auth, streamer)

    sender = MessageSender(tcp_preprocessor)
    listener = MessageListener(tcp_preprocessor, tls_handler, msg_handler)

    server = Server(ip_addr, tcp_port, auth, msg_handler,
                    tls_handler, session_handler, sender, streamer)

    for conn_obs in (tls_handler, media_handler, sender, listener, auth, msg_handler, streamer):
        server.add_connection_state_observer(conn_obs)
        msg_handler.add_connection_state_observer(conn_obs)

    auth.add_auth_state_obs(sender)

    server.serve()


if __name__ == "__main__":
    cli.add_command(set_server_password)
    cli.add_command(genereate_certificate)
    cli.add_command(run_server)
    cli()
