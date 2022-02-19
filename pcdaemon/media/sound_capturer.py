import logging
import pyaudio
from media.sound_caputured_obs import SoundCapturedObserver
from typing import List
from threading import Lock, Condition


class SoundCapturer:
    def __init__(self, capture_on_startup: bool = False, chunk_size=64,
                 rate=44100, channels=2) -> None:
        self.CHUNK = chunk_size
        self.RATE = rate
        self.CHANNELS = channels
        self.FORMAT = pyaudio.paInt16

        self.sound_captured_obss: List[SoundCapturedObserver] = []

        self.keep_capturing = capture_on_startup
        self.capture_lock = Lock()  # guards keep_capturing variable
        self.capture_mode_changed = Condition(self.capture_lock)

        self.running = False

    def add_sound_captured_obs(self, obs: SoundCapturedObserver):
        self.sound_captured_obss.append(obs)

    def start_capturing(self):
        if self.running:
            logging.critical('Sound capturing is already running')
            return

        self.running = True

        p = pyaudio.PyAudio()
        stream = p.open(format=self.FORMAT,
                        channels=self.CHANNELS,
                        rate=self.RATE,
                        input=True,
                        frames_per_buffer=self.CHUNK)

        while True:
            with self.capture_lock:
                while not self.keep_capturing:
                    if not self.running:
                        break
                    self.capture_mode_changed.wait()

            if not self.running:
                break

            try:
                data = stream.read(self.CHUNK)
                for obs in self.sound_captured_obss:
                    obs.on_frame_captured(data)
            except Exception:
                logging.exception('Failed to read sound stream data')

        stream.stop_stream()
        stream.close()
        p.terminate()

    def stop_capturing(self):
        with self.capture_lock:
            self.keep_capturing = False
            self.running = False
            self.capture_mode_changed.notify_all()

    def change_capture_mode(self, capture: bool):
        with self.capture_lock:
            self.keep_capturing = capture
            self.capture_mode_changed.notify_all()
