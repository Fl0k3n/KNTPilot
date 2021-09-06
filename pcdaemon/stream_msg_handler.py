from conn_state_obs import ConnectionStateObserver
from special_key_codes import SpecialKeyCode
from typing import Any
from msg_codes import MsgCode
from msg_handler import MsgHandler
from authenticator import Authenticator
from streamer import Streamer
from socket import socket


class StreamMsgHandler(MsgHandler, ConnectionStateObserver):
    def __init__(self, auth: Authenticator, client_socket: socket = None):
        self.auth = auth
        self.streamer = None
        self.client = client_socket

    def set_streamer(self, streamer: Streamer):
        self.streamer = streamer

    def connection_established(self, client_socket: socket):
        self.client = client_socket

    def handle_msg(self, code: MsgCode, data: Any):
        if code == MsgCode.AUTH:
            # authenticator will call its state observers
            self.auth.validate(data['password'], self.client)
            return

        if not self.auth.is_validated(self.client):
            # should never happen
            raise RuntimeError(f"Rcvd code {code} before authentication")

        if code == MsgCode.MOVE_SCREEN:
            self.streamer.move_screen(data['dx'], data['dy'])
        elif code == MsgCode.CLICK:
            self.streamer.click(data['x'], data['y'])
        elif code == MsgCode.CHANGE_MONITOR:
            self.streamer.change_monitor()
        elif code == MsgCode.RESCALE:
            self.streamer.rescale(data['ratio'])
        elif code == MsgCode.KEYBOARD_INPUT:
            self.streamer.press_key(
                data['key'], SpecialKeyCode(data['special_code']))
        elif code == MsgCode.SCROLL:
            self.streamer.scroll(data['up'])
        else:
            raise RuntimeError(
                f'Received unsupported msg code {code} with data\n{data}')

    def rcving_failed(self, err: Exception):
        print("Lost connection")
        print(err)
        self.streamer.stop_streaming()

    def connection_lost(self, client_socket: socket):
        self.client = None
