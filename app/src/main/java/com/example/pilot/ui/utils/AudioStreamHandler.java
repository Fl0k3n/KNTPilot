package com.example.pilot.ui.utils;


import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AudioStreamHandler {
    // buffer will hold up to (prefetchMs * BUFFER_SIZE_MULTIPLIER) data
    private static final int BUFFER_SIZE_MULTIPLIER = 4;
    private static final float PREFETCH_TIME_MULTIPLIER = 1.5f;

    private final SoundPlayer soundPlayer;
    private final int sampleRate;
    private final int bufferPrefetchMs;
    private final int samplesInSingleFrame;

    private final int prefetchFrameAmount;
    private volatile boolean prefetchMode;
    private MediaFramesBuffer<AudioFrame> buffer;

    private long recvStartedAt;

    private ReentrantLock consumerLock;
    private Condition consumerCond;
    private Thread audioConsumerThread;

    public AudioStreamHandler(SoundPlayer soundPlayer, int initialBufferPreFetchMs,
                              int sampleRate, int samplesInSingleFrame)
    {
        this.soundPlayer = soundPlayer;
        this.bufferPrefetchMs = initialBufferPreFetchMs;
        this.sampleRate = sampleRate;
        this.samplesInSingleFrame = samplesInSingleFrame;
        this.prefetchMode = true;

        this.consumerLock = new ReentrantLock();
        this.consumerCond = this.consumerLock.newCondition();

        prefetchFrameAmount = computePrefetchSize();

        initAudioConsumerThread();
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
        buffer = new MediaFramesBuffer<>(4 * prefetchFrameAmount);
    }

    public synchronized void addAudioFrame(AudioFrame audioFrame) {
        while (true) {
            try {
                buffer.put(audioFrame);
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


    private float getAudioFrameTimeSpan() {
        float samplePeriod = 1.0f / sampleRate * 1000; // in ms

        return samplesInSingleFrame * samplePeriod;
    }


    private int computePrefetchSize() {
        float audioFrameTimeSpan = getAudioFrameTimeSpan();

        return (int)(bufferPrefetchMs / audioFrameTimeSpan + 1);
    }

    private void initAudioConsumerThread() {
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

                    Optional<AudioFrame> audioFrame = fetchNextAudioFrame();

                    if (audioFrame.isPresent()) {
                        soundPlayer.enqueueAudioFrame(audioFrame.get());
                    }
                    else {
                        System.out.println("BUFFER UNDERRUN");
                    }
                } catch (InterruptedException interruptedException) {
                    System.out.println("Audio consumer interrupted, exiting.");
                    return;
                }
            }
        });

        audioConsumerThread.setDaemon(true);
        audioConsumerThread.start();
    }

    private synchronized Optional<AudioFrame> fetchNextAudioFrame() {
        if (buffer.getFilledSize() == 0) {
            prefetchMode = true;
            recvStartedAt = System.currentTimeMillis();
            buffer.resetSequence();
            return Optional.empty();
        }
        else if (buffer.getConsecutiveFilledSize() == 0) {
            buffer.skipMissingGap();
        }

        AudioFrame audioFrame = buffer.get();

        return Optional.of(audioFrame);
    }
}
