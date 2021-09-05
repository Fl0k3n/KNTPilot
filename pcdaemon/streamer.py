from special_key_codes import SpecialKeyCode
from input_ctl import InputController
import threading
from ss_sender import SsSender
from fps_ctl import FpsController
from ss_capturer import SSCapturer


class Streamer:
    def __init__(self, sender: SsSender, ss_capturer: SSCapturer, max_fps: int = 30) -> None:
        self.sender = sender
        self.ss_capturer = ss_capturer
        self.max_fps = max_fps
        self.fps_ctl = FpsController(self.max_fps)
        self.input_ctl = InputController()

        self.keep_streaming = False
        self.stream_lock = threading.Lock()

    def stop_streaming(self):
        with self.stream_lock:
            self.keep_streaming = False

    def stream(self):
        # possible race condition if stop_streaming is called before
        # this lock is acquired, not dangerous for now TODO
        with self.stream_lock:
            self.keep_streaming = True

        self.fps_ctl.reset()
        self.fps_ctl.start_timer()

        with self.ss_capturer:
            while True:
                ss_b64 = self.ss_capturer.get_ss_base64()
                with self.stream_lock:
                    if not self.keep_streaming:
                        raise ConnectionError('Stream interrupted')

                # throws connection error on lost connection
                self.sender.send_ss(ss_b64)

                self.fps_ctl.frame_sent()
                self.fps_ctl.wait_when_legal()

    def move_screen(self, dx: int, dy: int):
        self.ss_capturer.move_screen(dx, dy)

    def click(self, x: float, y: float):
        mon_x, mon_y = self.ss_capturer.get_ss_view_topleft()
        self.input_ctl.click(mon_x + x, mon_y + y)

    def change_monitor(self):
        self.ss_capturer.change_monitor()

    def rescale(self, ratio: float):
        self.ss_capturer.rescale(ratio)

    def press_key(self, key: str, code: SpecialKeyCode):
        if code == SpecialKeyCode.NONE:
            self.input_ctl.press_key(key)
        elif code == SpecialKeyCode.BACKSPACE:
            print("BACKSPACE")
