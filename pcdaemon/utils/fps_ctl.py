import time

# TODO compute fps based on RTT tcp equation


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
        self.reset()

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

    def reset(self):
        self.buff = CircularBuff(self._AVG_OF)
        self.start = None
