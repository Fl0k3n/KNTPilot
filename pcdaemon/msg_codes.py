from enum import Enum

# keep consistent with java networking/MsgCode.java


class MsgCode(Enum):
    # body = base64 utf-8 encoded jpg img
    SSHOT = 0  # ->
    # body = dx: int, dy: int -> deltas for how much upper left point of screen should be moved
    MOVE_SCREEN = 1  # <-
