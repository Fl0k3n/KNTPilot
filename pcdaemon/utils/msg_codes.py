from enum import Enum

# keep consistent with networking/MsgCode.java


class MsgCode(Enum):
    # @deprecated video is sent with different protocol
    # body = image: str -> base64 utf-8 encoded jpg img
    SSHOT = 0           # ->
    # body = dx: int, dy: int -> deltas for how much upper left point of screen should be moved
    MOVE_SCREEN = 1     # <-
    # body = x: float, dy: float, button: str -> click cords, relative to current screen pos and string
    # either "left" or "right" depending on choosen button
    CLICK = 2           # <-
    # body = empty string
    CHANGE_MONITOR = 3  # <-
    # body = ratio: float
    RESCALE = 4         # <-
    # body = key: char, special_code: int, key_modes: [int]
    KEYBOARD_INPUT = 5  # <-
    # body = password: str
    AUTH = 6            # <-
    # body = is_granted: bool
    AUTH_CHECKED = 7    # ->
    # body = up: bool
    SCROLL = 8          # <-
    # body = empty string
    SS_RCVD = 9         # ->
    # body = empty string
    MUTE = 10           # <-
    # body = empty string
    UNMUTE = 11         # <-
    # body = secret: secret key for streaming data encryption (256b ChaCha20), base64 encoded
    UDP_SECRET = 12     # ->
    # body = empty string
    UDP_SECRET_ACK = 13
    # body = x: float, dy: float, button: str -> click cords, relative to current screen pos and string
    # either "left" or "right" depending on choosen button
    DOUBLE_CLICK = 14


class TLSCode(Enum):
    # data is empty
    HELLO = 0
    # data contains utf-8 encoded certificate
    CERTIFICATE = 1
    # data contains secret key encrypted with subject's public key
    SECRET = 2
    # data contains packet from underlying protocol encrypted with established secret key
    SECURE = 3
