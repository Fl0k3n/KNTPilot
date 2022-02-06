package com.example.pilot.ui.utils;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;


public class AudioFrame implements MediaFrame{
    private final byte[] buff;
    private final long seqNum;
    private final int totalSize;
    private int recvdSize;

    // list instead of set, should be short (possibly even 1 element)
    // used to prevent errors caused by udp duplicate delivery
    private List<Short> rcvdOffsets;

    public AudioFrame(int totalSize, long seqNum) {
        this.totalSize = totalSize;
        this.seqNum = seqNum;
        this.buff = new byte[totalSize];
        this.recvdSize = 0;
        this.rcvdOffsets = new LinkedList<>();
    }

    public void putFragment(ByteBuffer byteBuffer, short offset, int size) {
        if (rcvdOffsets.contains(offset))
            return;

        rcvdOffsets.add(offset);

        recvdSize += size;
        byteBuffer.get(this.buff, offset, size);
    }

    public boolean isFullyRecvd() {
        return recvdSize == totalSize;
    }


    public byte[] getBytes() {
        return buff;
    }

    @Override
    public long getSeqNum() {
        return seqNum;
    }

    public int getTotalSize() {
        return totalSize;
    }
}
