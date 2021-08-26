package com.example.pilot.networking;

// keep consistent with pcdaemon/msg_codes.py codes,
// see that file for code reference
public enum MsgCode {
    SSHOT,
    MOVE_SCREEN,
    CLICK,
    CHANGE_MONITOR;

    public static MsgCode fromInteger(int x) {
        switch(x) {
            case 0:
                return SSHOT;
            case 1:
                return MOVE_SCREEN;
            case 2:
                return CLICK;
            case 3:
                return CHANGE_MONITOR;
            default:
                throw new IllegalArgumentException("code [" + x + "] is not supported");
        }
    }
}
