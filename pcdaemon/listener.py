from authenticator import Authenticator
from special_key_codes import SpecialKeyCode
from typing import Any
from msg_codes import MsgCode
from streamer import Streamer
from conn_state_obs import ConnectionStateObserver
from socket import socket
import json
import threading


class Listener(ConnectionStateObserver):
    def __init__(self, auth: Authenticator, client_socket: socket = None, header_size: int = 10):
        self.auth = auth
        self.client = client_socket
        self.header_size = header_size
        self.streamer = None
        self.thread = None

        self.keep_listenning = True
        self.keep_listenning_lock = threading.Lock()

    def set_streamer(self, streamer: Streamer):
        self.streamer = streamer

    def _recv(self, size: int) -> str:
        msg = ''
        while len(msg) < size:
            cur = self.client.recv(size).decode('utf-8')
            if len(cur) == 0:
                raise ConnectionAbortedError("Received EOF")
            msg += cur
        return msg

    def _listen(self):
        CHUNK_SIZE = 8 * 1024
        try:
            while True:
                with self.keep_listenning_lock:
                    if not self.keep_listenning:
                        return
                msg_size = int(self._recv(self.header_size).strip())
                data = []
                while len(data) < msg_size:
                    data.extend(self._recv(
                        min(CHUNK_SIZE, msg_size - len(data))))

                data = json.loads(''.join(data))
                print(data)

                self._handle_msg(MsgCode(data['code']), data['body'])
        except (ConnectionAbortedError, ConnectionResetError) as e:
            print("Lost connection")
            print(e)
            self.streamer.stop_streaming()
            return

    def _handle_msg(self, code: MsgCode, data: Any):
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
        else:
            raise RuntimeError(
                f'Received unsupported msg code {code} with data\n{data}')

    def connection_established(self, client_socket: socket):
        self.client = client_socket
        self.keep_listenning = True
        self.thread = threading.Thread(
            target=self._listen, daemon=True)

        self.thread.start()

    def connection_lost(self, client_socket: socket):
        with self.keep_listenning_lock:
            self.client = None
            self.thread = None
            self.keep_listenning = False
