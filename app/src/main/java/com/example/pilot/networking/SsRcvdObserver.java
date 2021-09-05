package com.example.pilot.networking;

import com.example.pilot.utils.ScreenShot;

public interface SsRcvdObserver {
    // all functions have to be thread safe

    void onScreenShotRcvd(ScreenShot ss);
}
