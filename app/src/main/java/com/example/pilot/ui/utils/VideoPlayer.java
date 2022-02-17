package com.example.pilot.ui.utils;

import com.example.pilot.networking.observers.SsRcvdObserver;
import com.example.pilot.networking.udp.MediaCode;
import com.example.pilot.networking.udp.MediaFrame;
import com.example.pilot.ui.views.ImageViewer;
import com.example.pilot.utils.ScreenShot;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoPlayer implements MediaPlayer {
    private static final int QUEUE_CAPACITY = 1;

    private final int max_fps;
    private final long perfectFrameTimeMs;
    private final GuiRunner guiRunner;
    private final ImageViewer imageViewer;
    private final BlockingQueue<MediaFrame> buffer;
    private final FPSCounter fpsCounter;

    private final ExecutorService executorService;
    private Future<?> playerTask;


    public VideoPlayer(GuiRunner guiRunner, ImageViewer imageViewer, FPSCounter fpsCounter, int max_fps) {
        this.guiRunner = guiRunner;
        this.imageViewer = imageViewer;
        this.fpsCounter = fpsCounter;
        this.max_fps = max_fps;
        this.perfectFrameTimeMs = 1000 / max_fps;
        buffer = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        executorService = Executors.newSingleThreadExecutor();
    }


    @Override
    public void enqueueMediaFrame(MediaFrame mediaFrame) throws InterruptedException {
        buffer.put(mediaFrame);
    }

    @Override
    public float getFrameTimeSpanMs() {
        return 1.0f / max_fps * 1000;
    }

    @Override
    public MediaCode getMediaType() {
        return MediaCode.VIDEO_FRAME;
    }

    @Override
    public synchronized void start() {
        fpsCounter.reset();
        initPlayerTask();
    }

    @Override
    public synchronized void stop() {
        if (playerTask != null) {
            if (playerTask.cancel(true)) {
                System.out.println("Video player stopped");
            }
            playerTask = null;
        }

        buffer.clear();
    }

    private void initPlayerTask() {
        playerTask = executorService.submit(() -> {
            System.out.println("Video player started");
            try {
                while (true) {
                    MediaFrame frame = buffer.take();

                    displayFrame(frame);

                    waitBeforeNextFrame();
                }
            } catch (InterruptedException consumed) {
                System.out.println("Video player interrupted, exiting");
            }
        });
    }

    private void displayFrame(MediaFrame mediaFrame) {
        final ScreenShot screenShot = new ScreenShot(mediaFrame.getBytes());
        guiRunner.scheduleGuiTask(() -> imageViewer.updateImage(screenShot));
        fpsCounter.onFrameDisplayed();
    }

    private void waitBeforeNextFrame() throws InterruptedException {
        long estimatedFrameTime = fpsCounter.getFrameTimeApproxMs();
        Thread.sleep(perfectFrameTimeMs);
//        if (estimatedFrameTime > perfectFrameTimeMs)
//            Thread.sleep(estimatedFrameTime - 3 * perfectFrameTimeMs / 4);
//        else
//            Thread.sleep(perfectFrameTimeMs / 2);
    }
}