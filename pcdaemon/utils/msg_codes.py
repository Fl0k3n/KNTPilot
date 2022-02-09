from enum import Enum

# keep consistent with networking/MsgCode.java


class MsgCode(Enum):
    # body = image: str -> base64 utf-8 encoded jpg img
    SSHOT = 0           # ->
    # body = dx: int, dy: int -> deltas for how much upper left point of screen should be moved
    MOVE_SCREEN = 1     # <-
    # body = x: float, dy: float -> click cords, relative to current screen pos
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


class TLSCode(Enum):
    # body = empty string
    HELLO = 0
    # body = certificate:  base64encoded-json encoded certificate (for format see security.certificate class)
    CERTIFICATE = 1
    #
    SECRET = 2
    # body ?
    SECURE = 3
