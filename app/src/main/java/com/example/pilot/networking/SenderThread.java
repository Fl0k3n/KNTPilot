package com.example.pilot.networking;

import com.example.pilot.utils.BlockingQueue;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class SenderThread extends Thread {
    private boolean keepSending;
    private final Socket server;
    private final BlockingQueue<String> jsonMessages;
    private final int BUFF_SIZE = 8 * 1024;

    public SenderThread(Socket server, BlockingQueue<String> msgQueue) {
        this.keepSending = true;
        this.server = server;
        this.jsonMessages = msgQueue;
    }

    public synchronized void stopSending() {
        this.keepSending = false;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String msg = jsonMessages.get();
                synchronized (this) {
                    if (!this.keepSending)
                        return;
                }

                // TODO compare performance with these streams/writers cached
                try (OutputStream outputStream = this.server.getOutputStream();
                     OutputStreamWriter outWriter = new OutputStreamWriter(outputStream, "UTF-8");
                     BufferedWriter writer = new BufferedWriter(outWriter, BUFF_SIZE)) {
                    writer.write(msg, 0, msg.length());
                } catch (IOException e) {
                    // this thread shouldn't handle this
                    e.printStackTrace();
                    return;
                }

            } catch (InterruptedException e) {
                // TODO shouldn't be interrupted tho
                e.printStackTrace();
                break;
            }
        }
    }
}
