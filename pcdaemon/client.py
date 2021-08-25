import socket
from typing import Any, Generator, Tuple
import json
import base64
from server import MsgCode
import time


class Client:
    _PORT = 9559

    def __init__(self):
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.socket.connect(('127.0.0.1', self._PORT))
        print('connected')

        try:
            bm_start = None
            fps = 0
            for code, data in self.listen():
                if bm_start is None:
                    bm_start = time.time()
                if time.time() - bm_start > 1:
                    break
                # print(f'Got code: [{code}] | data: {data}')
                if MsgCode(code) == MsgCode.SSHOT:
                    img = base64.b64decode(data)
                    fps += 1

            print(f'FPS: {fps}')

        except Exception as e:
            print(e)
            print('aborting')
            pass

        self.socket.close()

    def _recv(self, size) -> str:
        msg = ''
        while len(msg) < size:
            cur = self.socket.recv(size).decode('utf-8')
            if len(cur) == 0:
                raise ConnectionAbortedError
            msg += cur
        return msg

    def listen(self) -> Generator[Tuple[MsgCode, Any], None, None]:
        HEADER_SIZE = 10
        CHUNK_SIZE = 8 * 1024
        try:
            while True:
                msg_size = int(self._recv(HEADER_SIZE).strip())
                data = []
                while len(data) < msg_size:
                    data.extend(self._recv(
                        min(CHUNK_SIZE, msg_size - len(data))))

                data = json.loads(''.join(data))
                yield data['code'], data['body']
        except ConnectionAbortedError as e:
            print("Lost connection ", e)
            raise


if __name__ == '__main__':
    Client()
