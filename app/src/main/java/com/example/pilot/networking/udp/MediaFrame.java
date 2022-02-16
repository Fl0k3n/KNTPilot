package com.example.pilot.networking.udp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;


public class MediaFrame {
    private static final int HEADER_SIZE = 16;

    private final MediaCode code;
    private final byte[] buff;
    private final int seqNum;
    private final int totalSize;
    private int recvdSize;

    // list instead of set, should be short
    // used to prevent errors caused by udp duplicate delivery
    private final List<Integer> rcvdOffsets;

    public MediaFrame(MediaCode code, int seqNum, int totalSize) {
        this.code = code;
        this.totalSize = totalSize;
        this.seqNum = seqNum;
        this.buff = new byte[totalSize];
        this.recvdSize = 0;
        this.rcvdOffsets = new LinkedList<>();
    }

    public static MediaFrame buildFromRaw(byte[] packet) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN);

        MediaCode code = MediaCode.fromInteger(byteBuffer.get(0));
        int seqNum = byteBuffer.getInt(4);
        int totalSize = byteBuffer.getInt(8);

        return new MediaFrame(code, seqNum, totalSize);
    }


    public void putFragment(byte[] packet, int fragmentSize) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(packet).order(ByteOrder.BIG_ENDIAN);

        int offset = byteBuffer.getInt(12);
        int size = fragmentSize - HEADER_SIZE;

        if (rcvdOffsets.contains(offset))
            return;

        rcvdOffsets.add(offset);

        recvdSize += size;

        byteBuffer.position(HEADER_SIZE);
        byteBuffer.get(this.buff, offset, size);
    }

    public boolean isFullyRecvd() {
        return recvdSize == totalSize;
    }

    public byte[] getBytes() {
        return buff;
    }

    public int getSeqNum() {
        return seqNum;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public MediaCode getCode() {
        return code;
    }

    public static MediaCode extractCode(byte[] packet) {
        return MediaCode.fromInteger(packet[0]);
    }

    public static int extractSeqNum(byte[] packet) {
        return ByteBuffer
                .wrap(packet)
                .order(ByteOrder.BIG_ENDIAN)
                .getInt(4);
    }
}
