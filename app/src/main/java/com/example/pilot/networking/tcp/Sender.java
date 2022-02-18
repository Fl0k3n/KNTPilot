package com.example.pilot.networking.tcp;

import android.util.Log;

import androidx.annotation.GuardedBy;

import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.security.MessageSecurityPreprocessor;
import com.example.pilot.security.exceptions.SecurityException;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class Sender implements ConnectionStatusObserver {
    private static final String TAG = "Sender";
    private static final int SEND_QUEUE_CAPACITY = 16;

    private final BlockingQueue<String> jsonMessages;
    private final MessageSecurityPreprocessor preprocessor;

    @GuardedBy("this") private Thread senderThread;

    @Inject
    public Sender(@Named("TCP preprocessor") MessageSecurityPreprocessor preprocessor) {
        this.jsonMessages = new LinkedBlockingQueue<>(SEND_QUEUE_CAPACITY);
        this.preprocessor = preprocessor;
    }


    void enqueueJsonMessageRequest(String jsonData) {
        try {
            jsonMessages.put(jsonData);
        } catch (InterruptedException consumed) {
            // consume
        }
    }

    private Runnable createSenderTask(OutputStream outputStream) {
        return () -> {
            while (true) {
                try {
                    String msg = jsonMessages.take();

                    try  {
                        Log.d(TAG, "Sending: " + msg);
                        byte[] data = preprocessor.preprocessToSend(msg.getBytes());
                        outputStream.write(data);
                    } catch (IOException e) {
                        // this thread shouldn't handle this
                        Log.d(TAG, "Sender interrupted");
                        return;
                    } catch (SecurityException e) {
                        Log.wtf(TAG, "Security failed when trying to send message", e);
                        return;
                    }

                } catch (InterruptedException consumed) {
                    break;
                }
            }
        };
    }

    @Override
    public void failedToConnect(String errorMsg) {
        // pass
    }

    @Override
    public void connectionEstablished(Socket socket) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            Runnable senderTask = createSenderTask(outputStream);
            synchronized (this) {
                senderThread = new Thread(senderTask);
                senderThread.start();
            }
        } catch (IOException ignored) {
            // ignore here
        }
    }

    @Override
    public synchronized void connectionLost(Socket socket) {
        senderThread.interrupt();
        try {
            senderThread.join();
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
        jsonMessages.clear();
    }
}
