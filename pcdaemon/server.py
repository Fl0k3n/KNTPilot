import pickle
import socket
import atexit
from typing import Generator
import mss
import numpy as np
from PIL import Image
from enum import Enum
import json
import io
import base64
import time


class MsgCode(Enum):
    SSHOT = 0


class CircularBuff:
    def __init__(self, size: int):
        self.size = size
        self.idx = 0  # next free pos
        self.buff = [0] * self.size
        self.buff_sum = 0

    def put(self, x: float):
        self.buff_sum -= self.buff[self.idx]
        self.buff[self.idx] = x
        self.buff_sum += x
        self.idx = (self.idx + 1) % self.size

    def get_avg(self) -> float:
        return self.buff_sum / self.size


class FpsController:
    _AVG_OF = 5  # while computing avg time per frame take this # of frames

    def __init__(self, max_fps: int):
        self.max_fps = max_fps
        self.max_frame_time = 1 / max_fps
        self.buff = CircularBuff(self._AVG_OF)
        self.start = None

    def start_timer(self):
        self.start = time.time()

    def frame_sent(self):
        end = time.time()
        self.buff.put(end - self.start)
        self.start = end

    def wait_when_legal(self):
        avg_frame_time = self.buff.get_avg()
        if avg_frame_time > self.max_frame_time:
            return

        time.sleep(self._AVG_OF * (self.max_frame_time - avg_frame_time))


class Server:
    _PORT = 9559
    _MAX_FPS = 30

    def __init__(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        atexit.register(self.socket.close)

        self.socket.bind(('127.0.0.1', self._PORT))
        self.socket.listen(1)

        print('listenning')
        self.client, self.client_addr = self.socket.accept()
        print(self.client_addr)

        self.fps_ctl = FpsController(self._MAX_FPS)

        # for i in range(10):
        #     self._send_msg(MsgCode.SSHOT, 'KNTP')

    def stream(self):
        self.fps_ctl.start_timer()
        start = time.time()
        fps = 0
        with mss.mss() as sct:
            while True:
                # while time.time() - start < 1:
                try:
                    self._send_sshot(sct)
                    self.fps_ctl.frame_sent()
                    self.fps_ctl.wait_when_legal()

                    fps += 1
                except:
                    self.socket.close()
                    raise

    def _send_sshot(self, sct):
        mon = {"top": 0, "left": 0, "width": 800,
               "height": 640, "mon": sct.monitors[2]}
        # im = self.sct.grab(self.sct.monitors[1])
        im = sct.grab(mon)
        img = Image.frombytes("RGB", im.size, im.bgra, "raw", "BGRX")

        with io.BytesIO() as stream:
            img.save(stream, format='JPEG')
            stream.seek(0)
            bytes = stream.read()

        self._send_msg(MsgCode.SSHOT, base64.b64encode(
            bytes).decode(encoding='utf-8'))

    def _send_msg(self, code: MsgCode, value_bytes):
        HEADER_SIZE = 10

        msg_data = json.dumps({
            'code': code.value,
            'body': value_bytes
        })

        msg = f'{len(msg_data): <{HEADER_SIZE}}{msg_data}'
        print(msg[:100])
        print("*"*100)

        self.client.send(msg.encode('utf-8'))


if __name__ == '__main__':
    # with mss.mss() as sct:
    #     fps = 0
    #     t_start = time.time()
    #     while time.time() - t_start < 1:
    #         im = sct.grab(sct.monitors[2])
    #         img = Image.frombytes("RGB", im.size, im.bgra, "raw", "BGRX")

    #         # with io.BytesIO() as stream:
    #         #     img.save(stream, format='JPEG')
    #         #     stream.seek(0)
    #         # img.save('mf.jpg', format='JPEG')
    #         # img = cv2.imread('mf.jpg')

    #         cv2.imshow("name", img)
    #         if cv2.waitKey(25) & 0xFF == ord('q'):
    #             cv2.destroyAllWindows()
    #         fps += 1

    #     print(fps)
    while True:
        try:
            Server().stream()
        except (ConnectionError, BrokenPipeError):
            print('LOST CONNECTION')
        except Exception as e:
            print(e)
            break
