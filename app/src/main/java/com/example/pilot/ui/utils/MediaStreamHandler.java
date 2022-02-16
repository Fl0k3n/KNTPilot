package com.example.pilot.ui.utils;


import com.example.pilot.networking.udp.FragmentAssembler;
import com.example.pilot.networking.udp.FragmentBuffer;
import com.example.pilot.networking.udp.MediaCode;
import com.example.pilot.networking.udp.MediaFrame;
import com.example.pilot.networking.udp.StreamSkippedObserver;

import java.net.DatagramPacket;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MediaStreamHandler {
    // buffer will hold up to (prefetchMs * BUFFER_SIZE_MULTIPLIER) data
    // TODO make this adaptive
    private static final int BUFFER_SIZE_MULTIPLIER = 4;
    private static final float PREFETCH_TIME_MULTIPLIER = 1.5f;

    private final MediaPlayer mediaPlayer;
    private final int bufferPrefetchMs;

    private final int prefetchFrameAmount;
    private volatile boolean prefetchMode;
    private MediaFramesBuffer buffer;
    private LinkedList<StreamSkippedObserver> streamSkippedObservers;

    private long recvStartedAt;

    private final ReentrantLock consumerLock;
    private final Condition consumerCond;
    private Thread audioConsumerThread;

    public MediaStreamHandler(MediaPlayer mediaPlayer, int initialBufferPreFetchMs) {
        this.mediaPlayer = mediaPlayer;
        this.bufferPrefetchMs = initialBufferPreFetchMs;
        this.prefetchMode = true;

        this.consumerLock = new ReentrantLock();
        this.consumerCond = this.consumerLock.newCondition();

        this.streamSkippedObservers = new LinkedList<>();

        prefetchFrameAmount = computePrefetchSize();

        initMediaConsumerThread();
    }

    // shouldn't be used when stream is already running
    public void addStreamSkippedObserver(StreamSkippedObserver observer) {
        streamSkippedObservers.add(observer);
    }

    public MediaCode getMediaType() {
        return mediaPlayer.getMediaType();
    }

    public void restart() {
        try {
            consumerLock.lock();
            prefetchMode = true;
            restartBuffer();
            recvStartedAt = System.currentTimeMillis();
            consumerCond.signalAll();
        } finally {
            consumerLock.unlock();
        }
    }

    private void restartBuffer() {
        buffer = new MediaFramesBuffer(4 * prefetchFrameAmount);
    }

    public synchronized void addMediaFrame(MediaFrame mediaFrame) {
        while (true) {
            try {
                buffer.put(mediaFrame);
                break;
            } catch (OverrunException overrunException) {
                System.out.println(overrunException.getMessage());
                handleOverrun();
            }
        }

        if (prefetchMode && shouldFinishPrefetch()) {
            System.out.println("Starting to play");

            try {
                consumerLock.lock();
                prefetchMode = false;
                consumerCond.signalAll();
            } finally {
                consumerLock.unlock();
            }
        }
    }


    private boolean shouldFinishPrefetch() {
        return buffer.getConsecutiveFilledSize() >= prefetchFrameAmount ||
                System.currentTimeMillis() - recvStartedAt > bufferPrefetchMs * 1.5;
    }

    private void handleOverrun() {
        if (buffer.getFilledSize() < prefetchFrameAmount) {
            restart();
        }
        else {
            buffer.shiftBuff(prefetchFrameAmount - 1);
        }
    }


    private int computePrefetchSize() {
        float frameTimeSpanMs = mediaPlayer.getFrameTimeSpanMs();

        return (int)(bufferPrefetchMs / frameTimeSpanMs + 1);
    }

    private void initMediaConsumerThread() {
        audioConsumerThread = new Thread(() -> {
            while (true) {
                try {
                    try {
                        consumerLock.lock();
                        while (prefetchMode)
                            consumerCond.await();
                    } finally {
                        consumerLock.unlock();
                    }

                    Optional<MediaFrame> mediaFrame = fetchNextMediaFrame();

                    if (mediaFrame.isPresent()) {
                        mediaPlayer.enqueueMediaFrame(mediaFrame.get());
                    }
                    else {
                        System.out.println("BUFFER UNDERRUN");
                    }
                } catch (InterruptedException interruptedException) {
                    System.out.println("Media consumer interrupted, exiting.");
                    return;
                }
            }
        });

        audioConsumerThread.setDaemon(true);
        audioConsumerThread.start();
    }

    private synchronized Optional<MediaFrame> fetchNextMediaFrame() {
        if (buffer.getFilledSize() == 0) {
            prefetchMode = true;
            recvStartedAt = System.currentTimeMillis();
            buffer.resetSequence();
            return Optional.empty();
        }
        else if (buffer.getConsecutiveFilledSize() == 0) {
            buffer.skipMissingGap();
            int newSeqNum = buffer.peek().getSeqNum();
            streamSkippedObservers.forEach(observer -> observer.onSkippedTo(newSeqNum));
        }

        MediaFrame mediaFrame = buffer.get();

        return Optional.of(mediaFrame);
    }

}
