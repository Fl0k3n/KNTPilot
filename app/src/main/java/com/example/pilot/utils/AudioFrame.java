package com.example.pilot.utils;

public class AudioFrame {
    private byte[] bytes;
    public AudioFrame(byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {return bytes;}

}
