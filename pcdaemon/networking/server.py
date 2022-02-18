import logging
import socket
import atexit
from typing import List
from networking.abstract.msg_handler import MsgHandler
from networking.session_handler import SessionHandler
from security.tls_handler import TLSHandler
from security.authenticator import Authenticator
from media.streamers.streamer import Streamer
from networking.abstract.conn_state_obs import ConnectionStateObserver
from networking.message_sender import MessageSender


class Server:
    # more is probably pointless and not supported now (and likely never)
    _MAX_CONNECTIONS = 1

    def __init__(self, addr: str, tcp_port: int,
                 auth: Authenticator, msg_handler: MsgHandler,
                 tls_handler: TLSHandler, session_handler: SessionHandler,
                 sender: MessageSender, streamer: Streamer):
        self._PORT = tcp_port
        self._IP_ADDR = addr
        self.auth = auth
        self.msg_handler = msg_handler
        self.session_handler = session_handler
        self.tls_handler = tls_handler
        self.sender = sender
        self.streamer = streamer

        self._init_server_socket()

        self.connection_state_observers: List[ConnectionStateObserver] = []

    def add_connection_state_observer(self, obs: ConnectionStateObserver):
        self.connection_state_observers.append(obs)

    def _init_server_socket(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        atexit.register(self.socket.close)

        self.socket.bind((self._IP_ADDR, self._PORT))
        self.socket.listen(self._MAX_CONNECTIONS)

    def _listen_for_connection(self):
        logging.info('Waiting for connection...')
        client, client_addr = self.socket.accept()
        logging.info(f'Connected: {client_addr}')
        return client

    def _handle_connection(self, client: socket):
        session = self.session_handler.create_session(client)

        for obs in self.connection_state_observers:
            obs.connection_established(session)

        logging.info("waiting for secure channel")
        self.tls_handler.await_secure_channel(session)
        logging.info("waiting for user authentication")
        self.auth.await_authentication(session)
        logging.info("auth established, sending media secret key")
        self.sender.send_media_secret_key(session)
        logging.info("key sent, waiting for confirmation")
        self.streamer.await_secure_media_channel()
        logging.info("staring streaming")
        self.streamer.stream()

    def serve(self):
        while True:
            try:
                client = self._listen_for_connection()
                self._handle_connection(client)
            except (ConnectionError, BrokenPipeError):
                logging.info("lost connection", exc_info=True)
            except Exception:
                logging.critical(
                    "unexpected exception while handling connection", exc_info=True)
            finally:
                if client is not None:
                    self.session_handler.close_session(client)
                logging.info("session closed")
