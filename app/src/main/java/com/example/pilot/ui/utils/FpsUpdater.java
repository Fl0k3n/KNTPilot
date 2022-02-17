package com.example.pilot.ui.utils;

import android.view.MenuItem;

import java.util.concurrent.TimeUnit;

public class FpsUpdater {
    private final GuiRunner guiRunner;
    private final FPSCounter fpsCounter;
    private long updateEveryMs;
    private Thread updaterThread;

    public FpsUpdater(GuiRunner guiRunner, FPSCounter fpsCounter, long updateEvery, TimeUnit timeUnit) {
        this.guiRunner = guiRunner;
        this.fpsCounter = fpsCounter;
        updaterThread = null;

        this.updateEveryMs = TimeUnit.MILLISECONDS.convert(updateEvery, timeUnit);
        if (this.updateEveryMs < 250)
            this.updateEveryMs = 250;
    }

    public void start(MenuItem fpsBox) {
        updaterThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(updateEveryMs);
                } catch (InterruptedException consumed) {
                    return;
                }

                int fps = fpsCounter.getFps();
                guiRunner.scheduleGuiTask(() -> fpsBox.setTitle("FPS: " + fps));

                if (Thread.currentThread().isInterrupted())
                    return;
            }
        });

        updaterThread.start();
    }

    public void stop() {
        if (updaterThread != null) {
            updaterThread.interrupt();
            updaterThread = null;
        }
    }
}
