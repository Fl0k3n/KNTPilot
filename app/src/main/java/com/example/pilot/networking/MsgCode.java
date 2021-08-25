package com.example.pilot.networking;

public enum MsgCode {
    SSHOT;

    public static MsgCode fromInteger(int x) {
        switch(x) {
            case 0:
                return SSHOT;
            default:
                throw new IllegalArgumentException("code [" + x + "] is not supported");
        }
    }
}
