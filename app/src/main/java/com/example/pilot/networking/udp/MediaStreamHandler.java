package com.example.pilot.networking.udp;


import android.util.Log;

import androidx.annotation.GuardedBy;

import com.example.pilot.ui.utils.MediaPlayer;

import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class MediaStreamHandler {
    private static final String TAG = "Media Stream Handler";
    // buffer will hold up to (prefetchMs * BUFFER_SIZE_MULTIPLIER) data
    // TODO make this adaptive
    private static final int BUFFER_SIZE_MULTIPLIER = 4;
    private static final float PREFETCH_TIME_MULTIPLIER = 1.5f;

    private final MediaPlayer mediaPlayer;
    private final int bufferPrefetchMs;

    private final int prefetchFrameAmount;

    @GuardedBy("consumerLock") private boolean prefetchMode;
    @GuardedBy("consumerLock") private MediaFramesBuffer buffer;

    private final LinkedList<StreamSkippedObserver> streamSkippedObservers;

    private long recvStartedAt;

    private final ReentrantLock consumerLock;
    private final Condition consumerCond;

    private final ExecutorService executorService;
    private Future<?> mediaConsumer;

    public MediaStreamHandler(MediaPlayer mediaPlayer, int initialBufferPreFetchMs) {
        this.mediaPlayer = mediaPlayer;
        this.bufferPrefetchMs = initialBufferPreFetchMs;
        this.prefetchMode = true;

        this.consumerLock = new ReentrantLock();
        this.consumerCond = this.consumerLock.newCondition();

        this.streamSkippedObservers = new LinkedList<>();

        this.executorService = Executors.newSingleThreadExecutor();

        prefetchFrameAmount = computePrefetchSize();
    }

    public void start() {
        restart();
        mediaPlayer.start();
        initMediaConsumer();
    }

    // shouldn't be used when stream is already running
    public void addStreamSkippedObserver(StreamSkippedObserver observer) {
        streamSkippedObservers.add(observer);
    }

    public MediaCode getMediaType() {
        return mediaPlayer.getMediaType();
    }

    private void restart() {
        try {
            consumerLock.lock();
            prefetchMode = true;
            mediaConsumer = null;
            restartBuffer();
            recvStartedAt = System.currentTimeMillis();
            consumerCond.signalAll();
        } finally {
            consumerLock.unlock();
        }
    }

    private void restartBuffer() {
        try {
            consumerLock.lock();
            buffer = new MediaFramesBuffer(4 * prefetchFrameAmount);
        } finally {
            consumerLock.unlock();
        }
    }

    public void addMediaFrame(MediaFrame mediaFrame) throws InterruptedException {
        try {
            consumerLock.lockInterruptibly();

            while (true) {
                try {
                    buffer.put(mediaFrame);
                    break;
                } catch (OverrunException overrunException) {
                    Log.i(TAG, "[" + getMediaType() + "] " + overrunException.getMessage());
                    handleOverrun();
                }
            }

            if (prefetchMode && shouldFinishPrefetch()) {
                Log.i(TAG, "[" + getMediaType() + "] " + "Starting to play");

                prefetchMode = false;
                consumerCond.signalAll();
            }
        } finally {
            consumerLock.unlock();
        }
    }


    // caller should hold buffer lock
    private boolean shouldFinishPrefetch() {
        return buffer.getConsecutiveFilledSize() >= prefetchFrameAmount ||
                System.currentTimeMillis() - recvStartedAt > bufferPrefetchMs * 1.5;
    }

    // caller should hold buffer lock
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

    private void initMediaConsumer() {
         mediaConsumer = executorService.submit(() -> {
             Log.i(TAG, "[" + getMediaType() + "] " +" Consumer started");
             while (true) {
                try {
                    Optional<MediaFrame> mediaFrame;

                    try {
                        consumerLock.lockInterruptibly();
                        while (prefetchMode)
                            consumerCond.await();

                        mediaFrame = fetchNextMediaFrame();
                    } finally {
                        consumerLock.unlock();
                    }

                    if (mediaFrame.isPresent()) {
                        mediaPlayer.enqueueMediaFrame(mediaFrame.get());
                    }
                    else {
                        Log.i(TAG,"[" + getMediaType() + "] " +"BUFFER UNDERRUN");
                    }
                } catch (InterruptedException interruptedException) {
                    Log.d(TAG, "[" + getMediaType() + "] " +"Media consumer interrupted, exiting.");
                    return;
                }
            }
        });
    }

    // caller should hold buffer lock
    private Optional<MediaFrame> fetchNextMediaFrame() {
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

    public void stop() {
        try {
            consumerLock.lock();
            if (mediaConsumer != null) {
                if (!mediaConsumer.cancel(true)){
                    Log.d(TAG, "[" + getMediaType() + "] " + "Failed to cancel media consumer");
                }
                else {
                    Log.d(TAG, "[" + getMediaType() + "] " + "consumer CANCELED");
                }
                mediaConsumer = null;
            }

            mediaPlayer.stop();
            restartBuffer();
        } finally {
            consumerLock.unlock();
        }

    }
}
