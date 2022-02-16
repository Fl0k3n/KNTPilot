package com.example.pilot.ui.utils;

import com.example.pilot.networking.observers.SsRcvdObserver;
import com.example.pilot.networking.udp.MediaCode;
import com.example.pilot.networking.udp.MediaFrame;
import com.example.pilot.ui.views.ImageViewer;
import com.example.pilot.utils.ScreenShot;

import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class VideoPlayer implements MediaPlayer, Runnable {
    private static final int QUEUE_CAPACITY = 1;

    private final int fps;
    private final GuiRunner guiRunner;
    private final ImageViewer imageViewer;
    private final BlockingQueue<MediaFrame> buffer;

    private LinkedList<SsRcvdObserver> ssRcvdObservers;

    public VideoPlayer(GuiRunner guiRunner, ImageViewer imageViewer, int fps) {
        this.guiRunner = guiRunner;
        this.imageViewer = imageViewer;
        this.fps = fps;
        buffer = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
        ssRcvdObservers = new LinkedList<>();
    }


    public void addSSRcvdObserver(SsRcvdObserver ssRcvdObserver) {
        this.ssRcvdObservers.add(ssRcvdObserver);
    }


    @Override
    public void enqueueMediaFrame(MediaFrame mediaFrame) throws InterruptedException {
        buffer.put(mediaFrame);
    }

    @Override
    public float getFrameTimeSpanMs() {
        return 1.0f / fps * 1000;
    }

    @Override
    public MediaCode getMediaType() {
        return MediaCode.VIDEO_FRAME;
    }

    @Override
    public void run() {
        float frameTimeSpanMs = getFrameTimeSpanMs();
        long lastDisplayTime = getTimestampMs();
        try {
            while (true) {
                MediaFrame frame = buffer.take();
                long timestamp = getTimestampMs();
                long deltaT = timestamp - lastDisplayTime;
                System.out.println("deltaT = " + deltaT);

                if (deltaT < frameTimeSpanMs) {
                    Thread.sleep(deltaT);
                }

                displayFrame(frame);
                lastDisplayTime = getTimestampMs();
            }
        } catch (InterruptedException consumed) {
            System.out.println("Video player interrupted, exiting");
        }
    }

    private void displayFrame(MediaFrame mediaFrame) {
        final ScreenShot screenShot = new ScreenShot(mediaFrame.getBytes());
        guiRunner.scheduleGuiTask(() -> imageViewer.updateImage(screenShot));
        System.out.println("Display scheduled");
        ssRcvdObservers.forEach(ssRcvdObserver -> ssRcvdObserver.onScreenShotRcvd(screenShot));
    }

    private long getTimestampMs() {
        return System.currentTimeMillis();
    }
}
