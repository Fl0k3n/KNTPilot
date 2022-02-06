from networking.abstract.sound_sender import SoundSender
from media.sound_caputured_obs import SoundCapturedObserver
from media.sound_capturer import SoundCapturer
import threading
import queue
import base64

# TODO remove base64, send raw bytes


class SoundStreamer(SoundCapturedObserver):
    def __init__(self, sender: SoundSender, sound_capturer: SoundCapturer) -> None:
        self.sound_capturer = sound_capturer
        self.sender = sender
        self.buffer = queue.Queue()
        self.sound_capturer.add_sound_captured_obs(self)

        self.keep_streaming = False
        self.stream_lock = threading.Lock()

    def stop_streaming(self):
        with self.stream_lock:
            self.keep_streaming = False
            self.sound_capturer.stop_capturing()
            self.buffer.put('\0')  # wake from blocking wait on buffer data

    def on_frame_captured(self, frame: bytes):
        # frame64 = base64.b64encode(frame).decode('utf-8')
        with self.stream_lock:
            if self.keep_streaming:
                # self.buffer.put(frame64)
                self.buffer.put(frame)

    def stream_sound(self):
        with self.stream_lock:
            self.keep_streaming = True

        threading.Thread(
            target=self.sound_capturer.start_capturing, daemon=True).start()

        threading.Thread(
            target=self._stream_sound_data, daemon=True).start()

    def _stream_sound_data(self):
        while True:
            data = self.buffer.get(block=True)

            with self.stream_lock:
                if not self.keep_streaming:
                    self._flush_buffer()
                    break
            try:
                # self.sender.send_audio_frame(data)
                self.sender.send_audio_bytes(data)
            except ConnectionError:
                break
            except Exception as e:
                print('Failed to send sound data', e)
                break

    def _flush_buffer(self):
        # caller should hold self.stream_lock
        while True:
            try:
                self.buffer.get(block=False)  # flush
            except queue.Empty:
                break

    def mute(self):
        with self.stream_lock:
            self.sound_capturer.change_capture_mode(False)
            self._flush_buffer()

    def unmute(self):
        with self.stream_lock:
            self._flush_buffer()  # not needed probably
            self.sound_capturer.change_capture_mode(True)
