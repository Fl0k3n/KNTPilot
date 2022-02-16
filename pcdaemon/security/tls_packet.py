from struct import pack, unpack
from typing import Tuple
from utils.msg_codes import TLSCode

'''
TlS('ish) Packet
  |--------------------------------------------|
  |  tls_code(8)|    size(16)   | nonce_len(8) |
  |--------------------------------------------|
  {                                            }
  {             nonce(nonce_len * 8)           }
  {                                            }
  |--------------------------------------------|
  {                                            }
  {               data(size * 8)               }
  {                                            }
  |--------------------------------------------|
'''


class TLSPacket:
    HEADER_SIZE = 4
    _HEADER_FORMAT = ">BHB"

    def __init__(self, code: TLSCode, size: int, nonce_len: int, nonce: bytes, data: bytes) -> None:
        self.code = code
        self.size = size
        self.nonce_len = nonce_len

        self.nonce = nonce

        self.header = pack(self._HEADER_FORMAT, self.code.value,
                           self.size, self.nonce_len) + nonce
        self.data = data
        self.full = self.header + self.data

    @classmethod
    def _extract_header(cls, basic_header: bytes) -> Tuple[TLSCode, int, int]:
        code, size, nonce_len = unpack(
            cls._HEADER_FORMAT, basic_header[:cls.HEADER_SIZE])
        return TLSCode(code), size, nonce_len

    @classmethod
    def build_from_raw(cls, raw: bytes) -> "TLSPacket":
        code, size, nonce_len = cls._extract_header(raw)
        nonce = raw[cls.HEADER_SIZE: cls.HEADER_SIZE + nonce_len]
        data = raw[cls.HEADER_SIZE+nonce_len:]

        return TLSPacket(TLSCode(code), size, nonce_len, nonce, data)

    @classmethod
    def build_from_header_and_data(cls, header: bytes, data: bytes) -> "TLSPacket":
        return cls.build_from_raw(header + data)

    @classmethod
    def get_packet_size(cls, basic_header: bytes, tag_size: int) -> int:
        """Returns packet size excluding size of basic_header"""
        code, size, nonce_len = cls._extract_header(basic_header)
        msg_size = size + nonce_len

        if code == TLSCode.SECURE:
            msg_size += tag_size

        return msg_size
