from socket import socket
from typing import Iterable
import atexit
from networking.abstract.conn_state_obs import ConnectionStateObserver
from media.sound_capturer import SoundCapturer
from media.streamers.sound_streamer import SoundStreamer
from networking.abstract.sender import Sender
from media.streamers.video_streamer import VideoStreamer
from utils.special_key_codes import KeyboardModifier, SpecialKeyCode
from media.input_ctl import InputController
from media.ss_capturer import SSCapturer


class Streamer(ConnectionStateObserver):
    def __init__(self, sender: Sender, ss_capturer: SSCapturer, sound_capturer: SoundCapturer,
                 max_fps: int = 30, max_batch_sent_ss: int = 3) -> None:
        self.sender = sender
        self.ss_capturer = ss_capturer
        self.sound_capturer = sound_capturer
        self.max_fps = max_fps
        self.max_batch_sent_ss = max_batch_sent_ss
        self.input_ctl = InputController()

        self.video_streamer = VideoStreamer(
            sender, ss_capturer, max_fps, max_batch_sent_ss)

        self.sound_streamer = SoundStreamer(sender, sound_capturer)

        atexit.register(self.stop_streaming)

    def stop_streaming(self):
        self.video_streamer.stop_streaming()
        self.sound_streamer.stop_streaming()

    def stream(self):
        self.sound_streamer.stream_sound()
        self.video_streamer.stream_video()  # blocking, video is streamed from main thread

    def move_screen(self, dx: int, dy: int):
        self.ss_capturer.move_screen(dx, dy)

    def click(self, x: float, y: float):
        mon_x, mon_y = self.ss_capturer.get_ss_view_topleft()
        self.input_ctl.click(mon_x + x, mon_y + y)

    def change_monitor(self):
        self.ss_capturer.change_monitor()

    def rescale(self, ratio: float):
        self.ss_capturer.rescale(ratio)

    def press_key(self, key: str, code: SpecialKeyCode, modifiers: Iterable[KeyboardModifier]):
        special_map = {
            SpecialKeyCode.NONE: key,
            SpecialKeyCode.BACKSPACE: 'backspace',
            SpecialKeyCode.WINDOWS_KEY: 'winleft'
        }
        modifier_strs = [KeyboardModifier.to_string(x) for x in modifiers]

        self.input_ctl.press_key(special_map[code], modifier_strs)

    def scroll(self, up: bool):
        self.input_ctl.scroll(up)

    def ss_rcvd(self):
        self.video_streamer.ss_rcvd()

    def mute_sound(self):
        self.sound_streamer.mute()

    def unmute_sound(self):
        self.sound_streamer.unmute()

    def connection_established(self, client_socket: socket):
        pass

    def connection_lost(self, client_socket: socket):
        self.stop_streaming()
        self.input_ctl.lock_system()