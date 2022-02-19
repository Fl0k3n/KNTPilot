import logging
import queue
import threading
from networking.abstract.sound_sender import SoundSender
from media.sound_caputured_obs import SoundCapturedObserver
from media.sound_capturer import SoundCapturer


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

        logging.info("sound streamer stopped")

    def on_frame_captured(self, frame: bytes):
        with self.stream_lock:
            if self.keep_streaming:
                self.buffer.put(frame)

    def stream_sound(self):
        with self.stream_lock:
            self.keep_streaming = True

        threading.Thread(
            target=self.sound_capturer.start_capturing, daemon=True).start()

        threading.Thread(
            target=self._stream_sound_data, daemon=True).start()

    def _stream_sound_data(self):
        logging.debug("starting to stream sound")

        while True:
            data = self.buffer.get(block=True)

            with self.stream_lock:
                if not self.keep_streaming:
                    logging.info("sound streamer stop acked")
                    self._flush_buffer()
                    break
            try:
                self.sender.send_audio_bytes(data)
            except ConnectionError:
                logging.info(
                    "sound streamer detected connetion error, stopping")
                break
            except Exception:
                logging.exception("Failed to send sound data, exiting")
                break

    def _flush_buffer(self):
        # caller should hold self.stream_lock
        while True:
            try:
                self.buffer.get(block=False)
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
