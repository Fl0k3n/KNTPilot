package com.example.pilot.networking.observers;

import java.net.Socket;

public interface ConnectionStatusObserver {
    // all functions have to be thread safe

    void failedToConnect(String errorMsg);
    void connectionEstablished(Socket socket);
    void connectionLost(Socket socket);
}
