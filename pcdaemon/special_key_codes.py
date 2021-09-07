from enum import Enum

# keep it consistent with java/networking/SpecialKeyCode.java


class SpecialKeyCode(Enum):
    NONE = 0  # normal keyboard key without ctrl, shift etc
    BACKSPACE = 1
    WINDOWS_KEY = 2


class KeyboardModifier(Enum):
    CTRL_KEY = 0
    ALT_KEY = 1
    SHIFT_KEY = 2

    @staticmethod
    def to_string(key: "KeyboardModifier") -> str:
        strings = ['ctrl', 'alt', 'shift']
        return strings[key.value]
