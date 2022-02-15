from typing import Tuple
import mss
import io
from PIL import Image
import base64
from threading import Lock

# TODO send raw instead of base64
# remove rescaling, always send full size


class SSCapturer:
    def __init__(self, top: int = 0, left: int = 0,
                 #  widht: int = 840, height: int = 800,
                 widht: int = 200, height: int = 200,
                 screen_num: int = 2):
        self.DEFAULT_WIDTH, self.DEFAULT_HEIGHT = widht, height

        self.screen_x, self.screen_y = left, top
        self.ss_witdth, self.ss_height = widht, height

        self.streamed_screen_num = screen_num

        self.monitor_offset_x = None
        self.screen_w, self.screen_h = None, None

        self.screen_settings_lock = Lock()
        self.sct = None

    def get_ss_base64(self) -> str:
        with self.screen_settings_lock:
            mon = {"top":   self.screen_y,   "left": self.screen_x + self.monitor_offset_x,
                   "width": self.ss_witdth, "height": self.ss_height,
                   "mon":   self.sct.monitors[self.streamed_screen_num]}

        # im = self.sct.grab(self.sct.monitors[1])
        im = self.sct.grab(mon)

        img = Image.frombytes("RGB", im.size, im.bgra, "raw", "BGRX")

        with io.BytesIO() as stream:
            img.save(stream, format='JPEG')
            stream.seek(0)
            bytes = stream.read()

        return base64.b64encode(bytes).decode('utf-8')

    def __enter__(self):
        with self.screen_settings_lock:
            self.sct = mss.mss()
            mon = self.sct.monitors[self.streamed_screen_num]
            self.screen_w, self.screen_h = mon['width'], mon['height']
            self.monitor_offset_x = mon['left']

    def __exit__(self, *args, **kwargs):
        with self.screen_settings_lock:
            self.sct.close()
            self.sct = None
            self.screen_w, self.screen_h = None, None

    def move_screen(self, dx: int, dy: int):
        with self.screen_settings_lock:
            if self.sct is None:
                return

            self.screen_x -= dx
            self.screen_y -= dy

            self._fix_bounds()

    def get_monitor_num(self) -> int:
        # not needed here, left for consistency
        with self.screen_settings_lock:
            return self.streamed_screen_num

    def get_ss_view_topleft(self) -> Tuple[int, int]:
        with self.screen_settings_lock:
            return self.screen_x + self.monitor_offset_x, self.screen_y

    def change_monitor(self):
        with self.screen_settings_lock:
            self.ss_witdth = self.DEFAULT_WIDTH
            self.ss_height = self.DEFAULT_HEIGHT
            self.streamed_screen_num = 1 if self.streamed_screen_num == 2 else 2
            mon = self.sct.monitors[self.streamed_screen_num]
            self.monitor_offset_x = mon['left']
            self.screen_w, self.screen_h = mon['width'], mon['height']
            self.screen_x = 0
            self.screen_y = 0

            self.ss_witdth = min(self.screen_w, self.ss_witdth)
            self.ss_height = min(self.screen_h, self.ss_height)

    def rescale(self, ratio: float):
        with self.screen_settings_lock:
            self.ss_witdth = max(
                min(int(self.ss_witdth / ratio), self.screen_w), 64)
            self.ss_height = max(
                min(int(self.ss_height / ratio), self.screen_h), 64)

            self._fix_bounds()

    def _fix_bounds(self):
        overlay_x = self.screen_x + self.ss_witdth - self.screen_w
        self.screen_x -= max(overlay_x, 0)
        self.screen_x = max(self.screen_x, 0)

        overlay_y = self.screen_y + self.ss_height - self.screen_h
        self.screen_y -= max(overlay_y, 0)
        self.screen_y = max(self.screen_y, 0)
