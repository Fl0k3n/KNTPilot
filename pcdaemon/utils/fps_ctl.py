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


# class FpsController:
#     _AVG_OF = 10  # while computing avg time per frame take this # of frames

#     def __init__(self, max_fps: int):
#         self.max_fps = max_fps
#         self.max_frame_time = 1 / max_fps
#         self.reset()

#     def start_timer(self):
#         self.start = time.time()

#     def frame_sent(self):
#         end = time.time()
#         self.buff.put(end - self.start)
#         self.start = end

#     def wait_when_legal(self) -> float:
#         """returns estimation of fps"""
#         avg_frame_time = self.buff.get_avg()
#         if avg_frame_time > self.max_frame_time:
#             return

#         time.sleep(self._AVG_OF * (self.max_frame_time - avg_frame_time))
#         return 1.0 / avg_frame_time

#     def reset(self):
#         self.buff = CircularBuff(self._AVG_OF)
#         self.start = None


class FpsController:
    _ALPHA = 0.875

    def __init__(self, max_fps: int):
        self.min_frame_time = 1 / max_fps
        self.reset()

    def start_timer(self):
        self.start = time.time()

    def frame_sent(self):
        end = time.time()
        self.frame_time_approx = self._ALPHA * self.frame_time_approx + \
            (1 - self._ALPHA) * (end - self.start)
        self.start = end

    def get_frame_time_approx_secs(self) -> float:
        return self.frame_time_approx

    def wait_when_legal(self) -> float:
        """returns estimation of fps"""

        if self.frame_time_approx > self.min_frame_time:
            time.sleep(self.min_frame_time / 2)
        else:
            time.sleep(self.min_frame_time - (self.frame_time_approx / 2))

        return 1.0 / self.frame_time_approx

    def reset(self):
        self.start = None
        self.frame_time_approx = self.min_frame_time * 2
