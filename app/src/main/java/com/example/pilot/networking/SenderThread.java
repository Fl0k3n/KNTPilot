package com.example.pilot.networking;

import com.example.pilot.security.MessageSecurityPreprocessor;
import com.example.pilot.security.SecurityException;
import com.example.pilot.utils.BlockingQueue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class SenderThread extends Thread {
    private boolean keepSending;
    private final Socket server;
    private final BlockingQueue<String> jsonMessages;
    private final MessageSecurityPreprocessor preprocessor;
    private final int BUFF_SIZE = 8 * 1024;

    public SenderThread(Socket server, BlockingQueue<String> msgQueue, MessageSecurityPreprocessor preprocessor) {
        this.keepSending = true;
        this.server = server;
        this.jsonMessages = msgQueue;
        this.preprocessor = preprocessor;
    }

    public synchronized void stopSending() {
        this.keepSending = false;
    }

    @Override
    public void run() {
        try {
            OutputStream stream = this.server.getOutputStream();
            while (true) {
                try {
                    String msg = jsonMessages.get();

                    synchronized (this) {
                        if (!this.keepSending)
                            return;
                    }

                    try  {
                        System.out.println("******************************");
                        System.out.println("sending: " + msg);
                        byte[] data = preprocessor.preprocessToSend(msg.getBytes());
                        stream.write(data);
                        stream.flush();
                    } catch (IOException e) {
                        // this thread shouldn't handle this
                        e.printStackTrace();
                        return;
                    } catch (SecurityException e) {
                        // TODO
                        e.printStackTrace();
                        return;
                    }

                } catch (InterruptedException e) {
                    // TODO
                    e.printStackTrace();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
