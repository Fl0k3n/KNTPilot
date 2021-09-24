package com.example.pilot.ui.utils;

import com.example.pilot.networking.observers.SsRcvdObserver;
import com.example.pilot.utils.ScreenShot;

import java.util.LinkedList;

public class FPSCounter implements SsRcvdObserver, Runnable {
    private long tickTimeNano;
    private LinkedList<Long> timestampsNano;
    private FpsUpdater fpsUpdater;

    public FPSCounter(FpsUpdater updater) {
        this(updater, 450_000_000);
    }

    public FPSCounter(FpsUpdater updater, long tickTimeNano) {
        this.fpsUpdater = updater;
        this.tickTimeNano = tickTimeNano;
        timestampsNano = new LinkedList<>();
    }

    @Override
    public synchronized void onScreenShotRcvd(ScreenShot ss) {
        long now = System.nanoTime();
        timestampsNano.addLast(now);
        remove_old();
    }



    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(tickTimeNano / 1_000_000);
            } catch (InterruptedException e) {
                return;
            }

            int fps;
            synchronized (this) {
                remove_old();
                fps = timestampsNano.size();
            }
            fpsUpdater.updateFPS(fps);
        }
    }

    private void remove_old() {
        // user should synchronize
        long now = System.nanoTime();
        while (timestampsNano.size() > 0 &&
                now - timestampsNano.getFirst() > 1_000_000_000)
            timestampsNano.removeFirst();
    }
}
