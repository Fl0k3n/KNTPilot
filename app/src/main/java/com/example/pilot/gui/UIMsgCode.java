package com.example.pilot.gui;

public enum UIMsgCode {
    UPDATE_IMAGE,
    FAILED_TO_CONNECT,
    CONNECTION_LOST;

    public static UIMsgCode fromInteger(int x) {
        switch(x) {
            case 0:
                return UPDATE_IMAGE;
            case 1:
                return FAILED_TO_CONNECT;
            case 2:
                return CONNECTION_LOST;
            default:
                throw new IllegalArgumentException("code [" + x + "] is not supported");
        }
    }
}
