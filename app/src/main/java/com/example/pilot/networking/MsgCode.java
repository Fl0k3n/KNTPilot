package com.example.pilot.networking;

// keep consistent with pcdaemon/msg_codes.py codes,
// see that file for code reference
public enum MsgCode {
    SSHOT,
    MOVE_SCREEN,
    CLICK,
    CHANGE_MONITOR,
    RESCALE,
    KEYBOARD_INPUT,
    AUTH,
    AUTH_CHECKED,
    SCROLL,
    SS_RCVD,
    MUTE,
    UNMUTE;

    public static MsgCode fromInteger(int x) {
        switch(x) {
            case 0:  return SSHOT;
            case 1:  return MOVE_SCREEN;
            case 2:  return CLICK;
            case 3:  return CHANGE_MONITOR;
            case 4:  return RESCALE;
            case 5:  return KEYBOARD_INPUT;
            case 6:  return AUTH;
            case 7:  return AUTH_CHECKED;
            case 8:  return SCROLL;
            case 9:  return SS_RCVD;
            case 10: return MUTE;
            case 11: return UNMUTE;
            default:
                throw new IllegalArgumentException("code [" + x + "] is not supported");
        }
    }
}
