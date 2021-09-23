from sound_caputured_obs import SoundCapturedObserver
from typing import List
import pyaudio
from threading import Lock


class SoundCapturer:
    def __init__(self, chunk_size=1024, rate=44100, channels=2) -> None:
        self.CHUNK = chunk_size
        self.RATE = rate
        self.CHANNELS = channels
        self.FORMAT = pyaudio.paInt16

        self.sound_captured_obss: List[SoundCapturedObserver] = []

        self.keep_capturing = False
        self.capture_lock = Lock()

    def add_sound_captured_obs(self, obs: SoundCapturedObserver):
        self.sound_captured_obss.append(obs)

    def start_capturing(self):
        with self.capture_lock:
            self.keep_capturing = True

        p = pyaudio.PyAudio()
        stream = p.open(format=self.FORMAT,
                        channels=self.CHANNELS,
                        rate=self.RATE,
                        input=True,
                        frames_per_buffer=self.CHUNK)

        while True:
            with self.capture_lock:
                if not self.keep_capturing:
                    break

            data = stream.read(self.CHUNK)
            for obs in self.sound_captured_obss:
                obs.on_frame_captured(data)

        stream.stop_stream()
        stream.close()
        p.terminate()

    def stop_capturing(self):
        with self.capture_lock:
            self.keep_capturing = False
