package com.example.pilot.networking.udp;

public enum MediaCode {
    AUDIO_FRAME,
    VIDEO_FRAME;

    public static MediaCode fromInteger(int x) {
        switch (x) {
            case 1: return AUDIO_FRAME;
            case 2: return VIDEO_FRAME;
            default:
                throw new IllegalArgumentException("code [" + x + "] is not supported");
        }
    }
}
