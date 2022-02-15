package com.example.pilot.security;

import java.nio.ByteBuffer;

public class Message {
    public final byte[] header;
    public final byte[] data;
    public final byte[] full;

    public Message(byte[] header, byte[] data) {
        this.header = header;
        this.data = data;
        this.full = mergeHeaderAndData();
    }


    public Message(byte[] messageBytes, int headerLength) {
        this.full = messageBytes;
        this.header = new byte[headerLength];
        this.data = new byte[this.full.length - headerLength];

        splitToHeaderAndData(headerLength);
    }

    private byte[] mergeHeaderAndData() {
        return ByteBuffer
                .allocate(header.length + data.length)
                .put(header)
                .put(data)
                .array();
    }

    private void splitToHeaderAndData(int headerLength) {
        System.arraycopy(full, 0, header, 0, headerLength);
        System.arraycopy(full, headerLength, data, 0, data.length);
    }
}
