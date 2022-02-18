from abc import ABC, abstractmethod


class SoundSender(ABC):
    @abstractmethod
    def send_audio_bytes(self, audio_frame: bytes):
        pass
