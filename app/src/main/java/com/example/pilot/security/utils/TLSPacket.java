package com.example.pilot.security.utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TLSPacket {
    public static final int HEADER_SIZE = 4;

    public final TLSCode code;
    public final short size;
    public final int nonceSize;
    public final byte[] nonce;

    public final byte[] header;
    public final byte[] data;
    public final byte[] full;

    public TLSPacket(TLSCode code, short size, int nonceSize, byte[] nonce, byte[] data) {
        this.code = code;
        this.size = size;
        this.nonceSize = nonceSize;
        this.nonce = nonce;

        this.header = convertHeaderToBytes();
        this.data = data;
        this.full = mergeHeaderAndData();

    }

    public TLSPacket(byte[] raw) {
        this.full = raw;

        nonceSize = raw[3];

        header = new byte[HEADER_SIZE + nonceSize];
        data = new byte[raw.length - header.length];

        splitToHeaderAndData();

        ByteBuffer byteBuffer = ByteBuffer.wrap(header);

        code = TLSCode.fromInteger(byteBuffer.get(0));
        size = byteBuffer.getShort(1);

        nonce = new byte[nonceSize];
        System.arraycopy(header, 4, nonce, 0, nonceSize);
    }

    public TLSPacket(byte[] header, byte[] data) {
        this.header = header;
        this.data = data;
        this.full = mergeHeaderAndData();

        ByteBuffer byteBuffer = ByteBuffer.wrap(header);

        code = TLSCode.fromInteger(byteBuffer.get(0));
        size = byteBuffer.getShort(1);
        nonceSize = byteBuffer.get(3);

        nonce = new byte[nonceSize];
        System.arraycopy(header, 4, nonce, 0, nonceSize);
    }

    private byte[] convertHeaderToBytes() {
        return ByteBuffer.allocate(HEADER_SIZE + nonce.length)
                .order(ByteOrder.BIG_ENDIAN)
                .put((byte)code.ordinal())
                .putShort(size)
                .put((byte) nonce.length)
                .put(nonce)
                .array();

    }


    private byte[] mergeHeaderAndData() {
        return ByteBuffer
                .allocate(header.length + data.length)
                .put(header)
                .put(data)
                .array();
    }

    private void splitToHeaderAndData() {
        System.arraycopy(full, 0, header, 0, header.length);
        System.arraycopy(full, header.length, data, 0, data.length);
    }

    public static int getMessageSize(byte[] basicHeader, int tagSize) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(basicHeader);
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        int messageSize = (int) byteBuffer.getShort(1) + byteBuffer.get(3);
        if (TLSCode.fromInteger(byteBuffer.get(0)) == TLSCode.SECURE)
            messageSize += tagSize;

        return messageSize;
    }
}
