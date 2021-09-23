package com.example.pilot.networking;

import com.example.pilot.utils.AudioFrame;

public interface AudioFrameRcvdObserver {
    void onAudioFrameRcvd(AudioFrame frame);
}
