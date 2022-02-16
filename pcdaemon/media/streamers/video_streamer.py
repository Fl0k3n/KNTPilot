import threading
from networking.abstract.ss_sender import SsSender
from media.ss_capturer import SSCapturer
from utils.fps_ctl import FpsController


class VideoStreamer:
    def __init__(self, sender: SsSender, ss_capturer: SSCapturer, max_fps: int = 30, max_batch_sent_ss: int = 3) -> None:
        self.sender = sender
        self.ss_capturer = ss_capturer
        self.max_fps = max_fps
        self.max_batch_sent_ss = max_batch_sent_ss
        self.fps_ctl = FpsController(self.max_fps)

        self.keep_streaming = False
        self.stream_lock = threading.Lock()

        self.sent_overhead = 0  # how many sshots were sent without confirmation
        self.ss_rcvd_lock = threading.Lock()
        self.ss_rcvd_cond = threading.Condition(self.ss_rcvd_lock)

    def stop_streaming(self):
        with self.stream_lock:
            self.keep_streaming = False
            self.sent_overhead = 0

        with self.ss_rcvd_lock:
            self.ss_rcvd_cond.notify_all()  # if its blocked waiting for confirmation

    def stream_video(self):
        # possible race condition if stop_streaming is called before
        # this lock is acquired, not dangerous for now TODO
        with self.stream_lock:
            self.keep_streaming = True

        self.fps_ctl.reset()
        self.fps_ctl.start_timer()

        with self.ss_capturer:
            while True:
                # with self.ss_rcvd_lock:
                #     while self.keep_streaming and self.sent_overhead >= self.max_batch_sent_ss:
                #         self.ss_rcvd_cond.wait()

                #     self.sent_overhead += 1

                # ss_b64 = self.ss_capturer.get_ss_base64()
                ss_bytes = self.ss_capturer.get_ss_bytes()
                with self.stream_lock:
                    if not self.keep_streaming:
                        raise ConnectionError('Stream interrupted')

                # throws connection error on lost connection
                # self.sender.send_ss(ss_b64)
                self.sender.send_ss_bytes(ss_bytes)

                self.fps_ctl.frame_sent()
                self.fps_ctl.wait_when_legal()

    def ss_rcvd(self):
        with self.ss_rcvd_lock:
            self.sent_overhead -= 1
            self.ss_rcvd_cond.notify_all()
