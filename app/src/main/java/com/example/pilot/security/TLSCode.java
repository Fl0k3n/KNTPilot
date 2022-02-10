package com.example.pilot.security;

import com.example.pilot.networking.MsgCode;

public enum TLSCode {
    HELLO,
    CERTIFICATE,
    SECRET,
    SECURE;

    public static TLSCode fromInteger(int x) {
        switch(x) {
            case 0: return HELLO;
            case 1: return CERTIFICATE;
            case 2: return SECRET;
            case 3: return SECURE;
            default:
                throw new IllegalArgumentException("code [" + x + "] is not supported");
        }
    }
}
