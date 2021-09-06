package com.example.pilot.gui;

public enum UIMsgCode {
    UPDATE_IMAGE,
    FAILED_TO_CONNECT,
    CONNECTION_LOST,
    CONNECTION_ESTB,
    AUTH_STATUS,
    UPDATE_FPS;

    public static UIMsgCode fromInteger(int x) {
        switch(x) {
            case 0:
                return UPDATE_IMAGE;
            case 1:
                return FAILED_TO_CONNECT;
            case 2:
                return CONNECTION_LOST;
            case 3:
                return CONNECTION_ESTB;
            case 4:
                return AUTH_STATUS;
            case 5:
                return UPDATE_FPS;
            default:
                throw new IllegalArgumentException("code [" + x + "] is not supported");
        }
    }
}
