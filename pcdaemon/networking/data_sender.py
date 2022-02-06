from struct import pack
from socket import socket
from typing import Generator
from utils.media_msg_codes import MediaMsgCode


class DataSender:

    def __init__(self, udp_sock: socket, peer_port: int, peer_ip: str, max_dgram_size: int = 1460):
        self.sock = udp_sock
        self.port = peer_port
        self.peer_ip = peer_ip

        # default is equal to default TCP MSS for ethernet
        self.max_dgram_size = max_dgram_size

        self.audio_frame_seq = 0

    def send_audio_frame(self, audio_frame: bytes):
        for fragment in self._build_audio_packets(audio_frame):
            self.sock.sendto(fragment, (self.peer_ip, self.port))

        self.audio_frame_seq += 1

    def _build_audio_packets(self, audio_frame: bytes) -> Generator[bytes, None, None]:
        """
        AUDIO packet:
        |--------------------------------------------|
        |  code(8)    |    size(16)   | reserved(8)  |
        |--------------------------------------------|
        |           sequence number(32)              |
        |--------------------------------------------|
        |      offset(16)      |     reserved(16)    |
        |--------------------------------------------|
        |                   data                     |
        |--------------------------------------------|
        """
        PACKET_FORMAT = '>bHxIHxx'
        code = MediaMsgCode.AUDIO_FRAME.value
        size = len(audio_frame)

        for audio_frame_sub_seq in range(0, len(audio_frame), self.max_dgram_size):
            header = pack(PACKET_FORMAT,
                          code, size, self.audio_frame_seq, audio_frame_sub_seq)

            data = audio_frame[audio_frame_sub_seq: audio_frame_sub_seq +
                               self.max_dgram_size]

            print(
                f"created fragment: {len(audio_frame)} {code}, {size}, {self.audio_frame_seq}")

            yield header + data
