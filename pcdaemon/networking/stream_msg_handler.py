import json
from networking.abstract.conn_state_obs import ConnectionStateObserver
from security.session import Session
from utils.auth_state_obs import AuthStateObserver
from utils.special_key_codes import KeyboardModifier, SpecialKeyCode
from typing import Any
from utils.msg_codes import MsgCode
from networking.abstract.msg_handler import MsgHandler
from utils.authenticator import Authenticator
from media.streamers.streamer import Streamer


class StreamMsgHandler(MsgHandler, ConnectionStateObserver):
    def __init__(self, auth: Authenticator):
        super().__init__()
        self.auth = auth
        self.streamer = None
        self.session = None

    def set_streamer(self, streamer: Streamer):
        self.streamer = streamer

    def connection_established(self, session: Session):
        self.session = session

    def handle_msg(self, code: MsgCode, data: Any):
        if code == MsgCode.AUTH:
            # authenticator will call its state observers
            self.auth.validate(data['password'], self.session)
            return

        assert self.session.is_authenticated(
        ), f"Rcvd code {code} before authentication"

        if code == MsgCode.MOVE_SCREEN:
            self.streamer.move_screen(data['dx'], data['dy'])
        elif code == MsgCode.CLICK:
            self.streamer.click(data['x'], data['y'])
        elif code == MsgCode.CHANGE_MONITOR:
            self.streamer.change_monitor()
        elif code == MsgCode.RESCALE:
            self.streamer.rescale(data['ratio'])
        elif code == MsgCode.KEYBOARD_INPUT:
            key_modes = json.loads(data['key_modes'])
            self.streamer.press_key(
                data['key'], SpecialKeyCode(data['special_code']), [KeyboardModifier(x) for x in key_modes])
        elif code == MsgCode.SCROLL:
            self.streamer.scroll(data['up'])
        elif code == MsgCode.SS_RCVD:
            self.streamer.ss_rcvd()
        elif code == MsgCode.MUTE:
            self.streamer.mute_sound()
        elif code == MsgCode.UNMUTE:
            self.streamer.unmute_sound()
        elif code == MsgCode.UDP_SECRET_ACK:
            print("GOT ACK")
            self.streamer.secure_channel_established()
        else:
            raise RuntimeError(
                f'Received unsupported msg code {code} with data\n{data}')

    def rcving_failed(self, err: Exception):
        print("Lost connection")
        print(err)
        session = self.session
        for obs in self.conn_state_obss:
            obs.connection_lost(session)

    def connection_lost(self, session: Session):
        self.session = None
