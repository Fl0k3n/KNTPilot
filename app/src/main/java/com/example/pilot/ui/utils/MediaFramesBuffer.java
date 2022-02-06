package com.example.pilot.ui.utils;


/**
 * TCP window-alike class, designed for multimedia sent via UDP such that some frames can be missed
 * and some can be delivered more than once, out-of-order frames will be saved in appropriate buffer position,
 * gaps with missed frames can be skipped.
 *
 * Overrun exception will be thrown if one frame would try to overwrite another, user should catch it and
 * appropriately shift or reset the buffer.
 *
 * User should check buffer size before either inserting or removing frame, class is not thread safe.
 */
public class MediaFramesBuffer<T extends MediaFrame>{
    private final int size;
    private final MediaFrame[] buff;
    private int rPtr, lastInOrderPtr;
    private long lastInOrderSeq;
    private int filledSize;
    private int consecutiveFilledSize;

    public MediaFramesBuffer(int size) {
        this.size = size;
        this.filledSize = consecutiveFilledSize = 0;
        buff = new MediaFrame[size];

        resetSequence();
    }

    public void resetSequence() {
        // should be called only when buffer is empty
        lastInOrderSeq = -1;
        rPtr = lastInOrderPtr = 0;
    }

    public int getConsecutiveFilledSize() {
        return consecutiveFilledSize;
    }

    public int getFilledSize() {
        return filledSize;
    }

    public void skipMissingGap() {
        // skip present frames

        while (filledSize > 0 && buff[rPtr] != null) {
            clearReadPos();
            filledSize--;
        }

        consecutiveFilledSize = 0;

        if (filledSize == 0) {
            resetSequence();
            return;
        }

        // rPtr now points to first free frame in gap

        while (buff[rPtr] == null) {
            rPtr = modInc(rPtr);
        }

        // put lastInOrderPtr at first free position after consecutive present space
        lastInOrderPtr = rPtr;

        do {
            lastInOrderSeq = buff[lastInOrderPtr].getSeqNum();
            lastInOrderPtr = modInc(lastInOrderPtr);
            consecutiveFilledSize++;
        } while (lastInOrderPtr != rPtr && buff[lastInOrderPtr] != null);
    }

    public T get() {
        T res = (T) buff[rPtr];
        clearReadPos();
        consecutiveFilledSize--;
        filledSize--;
        return res;
    }

    public void put(MediaFrame frame) throws OverrunException {
        // ignore UDP duplicate
        if (frame.getSeqNum() <= lastInOrderSeq)
            return;

        long seq = frame.getSeqNum();

        if (lastInOrderSeq == -1 || seq == lastInOrderSeq + 1) {
            tryPutFrame(frame, lastInOrderPtr);
            updateLastInOrderPtr();
        }
        else {
            int delta = (int)(seq - lastInOrderSeq);
            tryPutFrame(frame, (lastInOrderPtr + delta - 1) % size);
        }
    }


    public void shiftBuff(int shiftBy) {
        // assumes that by < filledSize - 1

        boolean skippedGap = false;

        while (shiftBy > 0) {
            if (buff[rPtr] != null) {
                clearReadPos();
                filledSize--;
                consecutiveFilledSize--;
                shiftBy--;
            }
            else {
                rPtr = modInc(rPtr);
                skippedGap = true;
            }
        }

        while (buff[rPtr] == null) {
            rPtr = modInc(rPtr);
        }


        if (skippedGap) {
            lastInOrderPtr = rPtr;
            consecutiveFilledSize = 0;
            updateLastInOrderPtr();
        }
    }


    private void clearReadPos() {
        buff[rPtr] = null;
        rPtr = modInc(rPtr);
    }

    private void tryPutFrame(MediaFrame frame, int idx) throws OverrunException {
        if (buff[idx] != null && frame.getSeqNum() != buff[idx].getSeqNum())
            throw new OverrunException("buffer Overrun\tgot seq: " + frame.getSeqNum() +
                    " when seq: " + buff[idx].getSeqNum() + " was still buffered");

        buff[idx] = frame;
        filledSize++;
    }

    private void updateLastInOrderPtr() {
        do {
            lastInOrderSeq = buff[lastInOrderPtr].getSeqNum();
            consecutiveFilledSize++;
            lastInOrderPtr = modInc(lastInOrderPtr);

            if (buff[lastInOrderPtr] == null)
                break;
        } while(lastInOrderPtr != rPtr);
    }

    private int modInc(int x) {
        return (x + 1) % size;
    }
}
