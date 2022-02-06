from abc import ABC, abstractmethod


class SoundSender(ABC):
    @abstractmethod
    def send_audio_frame(self, frame64: str):
        pass

    @abstractmethod
    def send_audio_bytes(self, audio_frame: bytes):
        pass
