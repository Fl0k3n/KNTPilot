package com.example.pilot.networking.observers;

public interface ConnectionStatusObserver {
    // all functions have to be thread safe

    void failedToConnect(String errorMsg);
    void connectionEstablished();
    void connectionLost();
}
