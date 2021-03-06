from struct import pack
from typing import Generator
from security.message_security_preprocessor import MessageSecurityPreprocessor
from networking.session import Session
from utils.media_msg_codes import MediaMsgCode

"""
    Media packet:
    |--------------------------------------------|
    |  code(8)    |         reserved(24)         |
    |--------------------------------------------|
    |           sequence number(32)              |
    |--------------------------------------------|
    |                 size(32)                   |
    |--------------------------------------------|
    |                 offset(32)                 |
    |--------------------------------------------|
    |                   data                     |
    |--------------------------------------------|
"""


class DataSender:
    _PACKET_FORMAT = '>bxxxIII'

    def __init__(self, session: Session, msg_security_preproc: MessageSecurityPreprocessor, max_data_size: int = 1440):
        self.session = session
        self.msg_preproc = msg_security_preproc

        self.max_data_size = max_data_size

        # probably no need to randomize seq's like in tcp since all communication should be encrypted either way
        self.audio_frame_seq = 0
        self.video_frame_seq = 0

    def send_audio_frame(self, audio_frame: bytes):
        self._send_fragmented(MediaMsgCode.AUDIO_FRAME,
                              audio_frame, self.audio_frame_seq)
        self.audio_frame_seq += 1

    def send_video_frame(self, video_frame: bytes):
        self._send_fragmented(MediaMsgCode.VIDEO_FRAME,
                              video_frame, self.video_frame_seq)
        self.video_frame_seq += 1

    def _send_fragmented(self, media_code: MediaMsgCode, media_frame: bytes, seq_num: int):
        for fragment in self._build_media_packets(media_code, media_frame, seq_num):
            encrypted_fragment = self.msg_preproc.preprocess_to_send(
                self.session, fragment)

            self.session.get_udp_socket().sendto(
                encrypted_fragment, self.session.get_udp_peer_addr())

    def _build_media_packets(self, media_code: MediaMsgCode, media_frame: bytes, seq_num: int) -> Generator[bytes, None, None]:
        code = media_code.value
        size = len(media_frame)

        for offset in range(0, len(media_frame), self.max_data_size):
            header = pack(self._PACKET_FORMAT, code,
                          seq_num, size, offset)

            data = media_frame[offset: offset +
                               self.max_data_size]

            yield header + data
