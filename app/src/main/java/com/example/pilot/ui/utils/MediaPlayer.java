package com.example.pilot.ui.utils;

import com.example.pilot.networking.udp.MediaCode;
import com.example.pilot.networking.udp.MediaFrame;

public interface MediaPlayer {
    // should block if it can't be enqueue immediately
    void enqueueMediaFrame(MediaFrame mediaFrame) throws InterruptedException;

    float getFrameTimeSpanMs();

    MediaCode getMediaType();

    void stop();

    void start();
}
