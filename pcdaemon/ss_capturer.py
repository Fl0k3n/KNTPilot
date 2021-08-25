from mss.base import MSSBase
import io
from PIL import Image
import base64
from threading import Lock


class SSCapturer:
    def __init__(self, top: int = 0, left: int = 0,
                 widht: int = 840, height: int = 600,
                 screen_num: int = 2):
        self.screen_x, self.screen_y = left, top
        self.screen_w, self.screen_h = widht, height
        self.streamed_screen_num = screen_num

        self.screen_settings_lock = Lock()

    def get_ss_base64(self, sct: MSSBase) -> str:
        with self.screen_settings_lock:
            mon = {"top":   self.screen_y,   "left": self.screen_x,
                   "width": self.screen_w, "height": self.screen_h,
                   "mon":   sct.monitors[self.streamed_screen_num]}

        # im = self.sct.grab(self.sct.monitors[1])
        im = sct.grab(mon)

        img = Image.frombytes("RGB", im.size, im.bgra, "raw", "BGRX")

        with io.BytesIO() as stream:
            img.save(stream, format='JPEG')
            stream.seek(0)
            bytes = stream.read()

        return base64.b64encode(bytes).decode('utf-8')

    def move_screen(self, dx: int, dy: int):
        with self.screen_settings_lock:
            self.screen_x -= dx
            self.screen_x = max(self.screen_x, 0)
            self.screen_y -= dy
            self.screen_y = max(self.screen_y, 0)
