package com.example.pilot.utils;

public interface ConnectionStatusObserver {
    // all functions have to be thread safe

    void failedToConnect();
    void connectionLost();
}
