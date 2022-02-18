package com.example.pilot.networking.tcp;
import android.util.Log;

import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.security.exceptions.AuthenticationException;
import com.example.pilot.security.exceptions.SecurityException;
import com.example.pilot.security.TLSHandler;

import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ConnectionHandler implements Runnable {
    private static String TAG = "ConnectionHandler";

    private final static long DEFAULT_RECONNECT_TIMEOUT_MILLIS = 1000;

    private AtomicLong reconnect_timeout_millis;
    private int port;
    private String ipAddr;

    private Socket serverSocket;

    private AtomicBoolean isConnected;

    private final TLSHandler tlsHandler;

    private final LinkedList<ConnectionStatusObserver> connectionStatusObservers;
    private final Listener listener;

    private final ExecutorService executorService;
    private Future<?> connectionListenerTask;

    @Inject
    public ConnectionHandler(@Named("server ip address") String ipAddr, @Named("server port") int port,
                             TLSHandler tlsHandler, Listener listener,
                             @Named("connection executor") ExecutorService executorService) {
        this.port = port;
        this.ipAddr = ipAddr;
        this.tlsHandler = tlsHandler;
        this.reconnect_timeout_millis = new AtomicLong(DEFAULT_RECONNECT_TIMEOUT_MILLIS);
        this.connectionStatusObservers = new LinkedList<>();
        this.isConnected = new AtomicBoolean(false);
        this.listener = listener;
        this.executorService = executorService;
    }

    public synchronized void setConnectionParams(String ipAddr, int port) {
        this.ipAddr = ipAddr;
        this.port = port;
        reconnect_timeout_millis.set(DEFAULT_RECONNECT_TIMEOUT_MILLIS);
    }

    public synchronized void addConnectionStatusObserver(ConnectionStatusObserver obs) {
        connectionStatusObservers.add(obs);
    }

    public void establishConnection() {
        connectionListenerTask = executorService.submit(this);
    }

    @Override
    public void run() {
        while (true) {
            try (Socket socket = new Socket(ipAddr, port)) {
                reconnect_timeout_millis.set(DEFAULT_RECONNECT_TIMEOUT_MILLIS);
                isConnected.set(true);
                serverSocket = socket;

                tlsHandler.establishSecureChannel(socket);

                connectionStatusObservers.forEach(connectionStatusObserver -> connectionStatusObserver.connectionEstablished(socket));

                listener.listen(socket);
            } catch (IOException e) {
                Log.d(TAG, "failed to connect", e);

                synchronized (this) {
                    if (isConnected.get()) {
                        // connection lost
                        connectionStatusObservers.forEach(connectionStatusObserver -> connectionStatusObserver.connectionLost(serverSocket));
                    } else {
                        // connection was not established
                        connectionStatusObservers.forEach(obs -> obs.failedToConnect(e.getMessage()));
                    }
                    serverSocket = null;
                    isConnected.set(false);
                }
            } catch (SecurityException | AuthenticationException e) {
                // TODO
                establishConnection();
                Log.wtf(TAG, "security failed", e);
                return;
            }

            try {
                waitForNextConnectionTry();
            } catch (InterruptedException consumed) {
                Log.d(TAG, "Connection listener interrupted");
                return;
            }
        }
    }

    private void waitForNextConnectionTry() throws InterruptedException{
        long sleepTime = reconnect_timeout_millis.get();
        Thread.sleep(sleepTime);

        reconnect_timeout_millis.set(Math.min(DEFAULT_RECONNECT_TIMEOUT_MILLIS * 16, sleepTime * 2));
    }

    public synchronized void retryConnection() {
        if (!isConnected.get() && connectionListenerTask != null) {
            // can be connected but listeners were not called yet
            if (connectionListenerTask.cancel(true)) {
                Log.d(TAG, "connection listener cancelled");
            }
            else {
                Log.e(TAG, "failed to cancel listener");
            }
            establishConnection();
        }
    }
}
