from abc import ABC, abstractmethod


class SoundCapturedObserver(ABC):
    @abstractmethod
    def on_frame_captured(self, frame: bytes):
        pass
