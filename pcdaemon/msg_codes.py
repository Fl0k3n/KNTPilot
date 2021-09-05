from enum import Enum

# keep consistent with networking/MsgCode.java


class MsgCode(Enum):
    # body = base64 utf-8 encoded jpg img
    SSHOT = 0           # ->
    # body = dx: int, dy: int -> deltas for how much upper left point of screen should be moved
    MOVE_SCREEN = 1     # <-
    # body = x: float, dy: float -> click cords, relative to current screen pos
    CLICK = 2           # <-
    # body = empty string
    CHANGE_MONITOR = 3  # <-
    # body = ratio: float
    RESCALE = 4         # <-
    # body = key: char, special_code: int
    KEYBOARD_INPUT = 5  # <-
