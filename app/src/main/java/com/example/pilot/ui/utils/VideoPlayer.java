package com.example.pilot.ui.utils;

import android.util.Log;

import com.example.pilot.networking.udp.MediaCode;
import com.example.pilot.networking.udp.MediaFrame;
import com.example.pilot.utils.ScreenShot;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoPlayer implements MediaPlayer {
    private static final String TAG = "Video Player";
    private static final int QUEUE_CAPACITY = 1;
    private static final int MAX_UNDER_OVER_RUN_DELTA = 30;
    private static final int MIN_UNDER_OVER_RUN_DELTA = 5;

    private final int maxFps;
    private final int maxSleepTimeMs;
    private final GuiRunner guiRunner;
    private final ImageViewController imageViewer;
    private final BlockingQueue<MediaFrame> buffer;
    private final FPSCounter fpsCounter;

    private final ExecutorService executorService;
    private Future<?> playerTask;

    // corectness isn't as crucial as efficiency here
    private volatile int underrunOverrunDelta;

    private int lastDisplayedSeq;

    public VideoPlayer(GuiRunner guiRunner, ImageViewController imageViewer, FPSCounter fpsCounter, int maxFps) {
        this.guiRunner = guiRunner;
        this.imageViewer = imageViewer;
        this.fpsCounter = fpsCounter;
        this.maxFps = maxFps;
        this.maxSleepTimeMs = 1000 / maxFps;
        buffer = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        executorService = Executors.newSingleThreadExecutor();
        lastDisplayedSeq = -1;
        underrunOverrunDelta = 15;
    }


    @Override
    public void enqueueMediaFrame(MediaFrame mediaFrame) throws InterruptedException {
        buffer.put(mediaFrame);
    }

    @Override
    public float getFrameTimeSpanMs() {
        return 1.0f / maxFps * 1000;
    }

    @Override
    public MediaCode getMediaType() {
        return MediaCode.VIDEO_FRAME;
    }

    @Override
    public synchronized void start() {
        fpsCounter.reset();
        lastDisplayedSeq = -1;
        initPlayerTask();
    }

    @Override
    public void onOverrunDetected() {
        int tmp = underrunOverrunDelta;
        if (tmp > MIN_UNDER_OVER_RUN_DELTA)
            underrunOverrunDelta = tmp - 1;
    }

    @Override
    public void onUnderrunDetected() {
        int tmp = underrunOverrunDelta;
        if (tmp < MAX_UNDER_OVER_RUN_DELTA)
            underrunOverrunDelta = tmp + 1;

    }

    @Override
    public synchronized void stop() {
        if (playerTask != null) {
            if (playerTask.cancel(true)) {
                Log.d(TAG,"Video player stopped");
            }
            else {
                Log.w(TAG, "Failed to stop video player");
            }
            playerTask = null;
        }

        buffer.clear();
    }

    private void initPlayerTask() {
        playerTask = executorService.submit(() -> {
            Log.i(TAG,"Video player started");

            try {
                while (true) {
                    MediaFrame frame = buffer.take();

                    displayFrame(frame);

                    waitBeforeNextFrame();
                }
            } catch (InterruptedException consumed) {
                Log.d(TAG,"Video player interrupted, exiting");
            }
        });
    }

    private void displayFrame(MediaFrame mediaFrame) {
        final ScreenShot screenShot = new ScreenShot(mediaFrame.getBytes());
        guiRunner.scheduleGuiTask(() -> {
            int seqNum = mediaFrame.getSeqNum();
            if (lastDisplayedSeq < seqNum) {
                lastDisplayedSeq = seqNum;
                imageViewer.updateImage(screenShot);
            }
        });
    }

    private void waitBeforeNextFrame() throws InterruptedException {
        long estimatedFrameTime = fpsCounter.getFrameTimeApproxMs();
        float sleepTimeCoefficient = 1.0f + underrunOverrunDelta * 0.01f;
        Thread.sleep((long)(Math.min(maxSleepTimeMs, estimatedFrameTime * sleepTimeCoefficient)));
    }
}
