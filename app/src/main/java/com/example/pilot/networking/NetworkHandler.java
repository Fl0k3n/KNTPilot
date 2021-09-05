package com.example.pilot.networking;
import com.example.pilot.utils.BlockingQueue;
import com.example.pilot.utils.ConnectionStatusObserver;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

public class NetworkHandler implements Runnable, Sender {
    private final int PORT = 9559;
//    private final String IP_ADDR = "192.168.0.167";
    private final String IP_ADDR = "10.0.2.2";
    private Socket socket = null;
    private InputStream socketIn = null;
    private SenderThread sender = null;
    private boolean tryToReconnect = true;
    private final long DEFAULT_RECONNECT_TIMEOUT_MILLIS = 500;
    private long reconnect_timeout_millis = DEFAULT_RECONNECT_TIMEOUT_MILLIS;

    private final LinkedList<MessageRcvdObserver> msgRcvdObservers;
    private final LinkedList<ConnectionStatusObserver> connectionStatusObservers;
    private final BlockingQueue<String> messageQueue;

    private final int HEADER_SIZE = 10; // bytes
    private final int CHUNK_SIZE = 8 * 1024; // bytes

    public NetworkHandler() {
        this.msgRcvdObservers = new LinkedList<>();
        this.connectionStatusObservers = new LinkedList<>();
        this.messageQueue = new BlockingQueue<>();
    }


    public synchronized void addMsgRcvdObserver(MessageRcvdObserver obs) {
        msgRcvdObservers.add(obs);
    }

    public synchronized void addConnectionStatusObserver(ConnectionStatusObserver obs) {
        connectionStatusObservers.add(obs);
    }

    private String recv(int size) throws IOException {
        int size_read = 0;
        byte[] buff = new byte[size];

        while (size_read < size) {
            int rd = this.socketIn.read(buff, size_read, size - size_read);
            if (rd == -1)
                throw new ConnectException("Connection lost, stream returned -1");
            size_read += rd;
        }

        return new String(buff, StandardCharsets.UTF_8);
    }


    private String recvMessage() throws IOException {
        int length = Integer.parseInt(recv(HEADER_SIZE).trim());
        String msg = recv(length);
//        System.out.println("LENGTH: " + length + " READ: " + msg.length());
        return msg;
    }

    @Override
    public void run() {
        while (true) {
            try (Socket socket = new Socket(IP_ADDR, PORT);
                 InputStream stream = new BufferedInputStream(socket.getInputStream(), CHUNK_SIZE)) {
                reconnect_timeout_millis = DEFAULT_RECONNECT_TIMEOUT_MILLIS;
                System.out.println("Connected");
                this.socket = socket;
                this.socketIn = stream;
                this.sender = new SenderThread(socket, messageQueue, HEADER_SIZE);
                sender.setDaemon(true);
                sender.start();
//            benchmark();
                listen();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
                synchronized (this) {
                    if (this.sender != null) {
                        // connection lost
                        this.sender.stopSending();
                        this.sender = null;
                        connectionStatusObservers.forEach(ConnectionStatusObserver::connectionLost);
                    } else {
                        // connection was not established
                        connectionStatusObservers.forEach(obs -> obs.failedToConnect(e.getMessage()));
                    }
                    this.socket = null;
                }
            }

            synchronized (this) {
                if (!tryToReconnect)
                    break;
            }

            try {
                Thread.sleep(reconnect_timeout_millis);
                reconnect_timeout_millis *= 2;
            } catch (InterruptedException e) {
                synchronized (this) {
                    if(!tryToReconnect)
                        break;
                }
            }
        }
    }

    private void listen() throws IOException {
        while (true) {
            String rcvd = recvMessage();
            synchronized (this) {
                this.msgRcvdObservers.forEach(obs -> obs.msgRcvd(rcvd));
            }
        }
    }

    private void benchmark() throws IOException {
        long start = System.nanoTime();
        long bm_time = 1_000_000_000; //nano
        int fps = 0;

        while (System.nanoTime() - start < bm_time) {
            fps++;
            recvMessage();
        }

        System.out.println("FPS: " + fps);
    }

    @Override
    public void enqueueJsonMessageRequest(String jsonData) {
        messageQueue.put(jsonData);
    }
}
