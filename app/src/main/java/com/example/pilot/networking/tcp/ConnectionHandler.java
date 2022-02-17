package com.example.pilot.networking.tcp;
import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.security.exceptions.AuthenticationException;
import com.example.pilot.security.exceptions.SecurityException;
import com.example.pilot.security.TLSHandler;

import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ConnectionHandler implements Runnable {
    private final static long DEFAULT_RECONNECT_TIMEOUT_MILLIS = 500;

    private long reconnect_timeout_millis;
    private int port;
    private String ipAddr;

    private Socket serverSocket;

    private boolean isConnected;

    private final TLSHandler tlsHandler;

    private final LinkedList<ConnectionStatusObserver> connectionStatusObservers;
    private final Listener listener;

    private final ExecutorService executorService;

    @Inject
    public ConnectionHandler(@Named("server ip address") String ipAddr, @Named("server port") int port,
                             TLSHandler tlsHandler, Listener listener,
                             @Named("connection executor") ExecutorService executorService) {
        this.port = port;
        this.ipAddr = ipAddr;
        this.tlsHandler = tlsHandler;
        this.reconnect_timeout_millis = DEFAULT_RECONNECT_TIMEOUT_MILLIS;
        this.connectionStatusObservers = new LinkedList<>();
        this.isConnected = false;
        this.listener = listener;
        this.executorService = executorService;
    }

    public synchronized void setConnectionParams(String ipAddr, int port) {
        this.ipAddr = ipAddr;
        this.port = port;
        reconnect_timeout_millis = DEFAULT_RECONNECT_TIMEOUT_MILLIS;
    }

    public synchronized void addConnectionStatusObserver(ConnectionStatusObserver obs) {
        connectionStatusObservers.add(obs);
    }

    public void establishConnection() {
        executorService.submit(this);
    }

    @Override
    public void run() {
        while (true) {
            try (Socket socket = new Socket(ipAddr, port)) {
                reconnect_timeout_millis = DEFAULT_RECONNECT_TIMEOUT_MILLIS;
                serverSocket = socket;
                isConnected = true;

                tlsHandler.establishSecureChannel(socket);

                connectionStatusObservers.forEach(connectionStatusObserver -> connectionStatusObserver.connectionEstablished(socket));

                listener.listen(socket);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
                synchronized (this) {
                    if (isConnected) {
                        // connection lost
                        connectionStatusObservers.forEach(connectionStatusObserver -> connectionStatusObserver.connectionLost(serverSocket));
                    } else {
                        // connection was not established
                        connectionStatusObservers.forEach(obs -> obs.failedToConnect(e.getMessage()));
                    }
                    serverSocket = null;
                    isConnected = false;
                }
            } catch (SecurityException | AuthenticationException e) {
                // TODO
                System.out.println("SECURITY FAILED " + e.getMessage());
                e.printStackTrace();
            }

            try {
                Thread.sleep(reconnect_timeout_millis);
            } catch (InterruptedException e) {
                // TODO reset timeout here
            }

//            synchronized (this) {
//                reconnect_timeout_millis *= 2;
//            }
        }
    }
}
