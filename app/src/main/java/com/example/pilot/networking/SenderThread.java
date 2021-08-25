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
    private final int HEADER_SIZE;

    public SenderThread(Socket server, BlockingQueue<String> msgQueue, int headerSize) {
        this.HEADER_SIZE = headerSize;
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

                try  {
                    OutputStreamWriter outWriter = new OutputStreamWriter(this.server.getOutputStream(), "UTF-8");
                    BufferedWriter writer = new BufferedWriter(outWriter, BUFF_SIZE);
                    String header = String.format("%1$-" + HEADER_SIZE + "s", msg.length());
                    String fullMsg = header + msg;
                    System.out.println(header);

                    writer.write(fullMsg, 0, fullMsg.length());
                    writer.flush();
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
