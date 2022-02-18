import time


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
        # time.sleep(self.min_frame_time * 0.75)

        if self.frame_time_approx < self.min_frame_time:
            delta = self.min_frame_time - self.frame_time_approx
            time.sleep(100 * delta * self.min_frame_time)

        return 1.0 / self.frame_time_approx

    def reset(self):
        self.start = None
        self.frame_time_approx = self.min_frame_time * 2
