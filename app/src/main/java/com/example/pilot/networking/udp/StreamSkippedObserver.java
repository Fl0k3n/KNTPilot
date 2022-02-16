package com.example.pilot.networking.udp;

public interface StreamSkippedObserver {
    void onSkippedTo(int seqNum);
}
