package com.example.pilot.ui.utils;

import java.util.concurrent.atomic.AtomicLong;

public class FPSCounter {
    private final double alpha = 0.875;
    private final AtomicLong frameTimeApproxMs;
    private long lastUpdateTimestampMs;
    private final int expected_fps;

    public FPSCounter(int expected_fps) {
        this.expected_fps = expected_fps;
        this.frameTimeApproxMs = new AtomicLong(1 / expected_fps);// estimate perfect at init
        lastUpdateTimestampMs = -1;
    }

    public void onFrameDisplayed() {
        long now = System.currentTimeMillis();

        if(lastUpdateTimestampMs != -1) {
            frameTimeApproxMs.set((long) (alpha * frameTimeApproxMs.get() + (1 - alpha) * (now - lastUpdateTimestampMs)));
        }

        lastUpdateTimestampMs = now;
    }

    public int getFps() {
        try {
            return (int) (1000 / (frameTimeApproxMs.get()));
        } catch (ArithmeticException arithmeticException) {
            // zero division error may happen before transmission kicks off
            return -1;
        }
    }

    public long getFrameTimeApproxMs() {
        return frameTimeApproxMs.get();
    }

    public void reset() {
        frameTimeApproxMs.set(1 / expected_fps);
    }
}
