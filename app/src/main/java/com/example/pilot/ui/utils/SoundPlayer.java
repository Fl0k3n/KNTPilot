package com.example.pilot.ui.utils;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.example.pilot.networking.observers.ConnectionStatusObserver;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SoundPlayer implements ConnectionStatusObserver, Runnable {
    private static final int QUEUE_CAPACITY = 1;
    private BlockingQueue<AudioFrame> buffer;
    private AudioTrack audioTrack;
    private boolean muted;

    public SoundPlayer() {
        this(true, 44100, 384);
    }

    public SoundPlayer(boolean isMuted, int sampleRate, int internalBufferSize) {
        muted = isMuted;
        buffer = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

        // TODO refactor
        audioTrack = new AudioTrack(
                new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build(),
                new AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build(),
                internalBufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    public synchronized boolean isMuted() {
        return muted;
    }

    public synchronized void setMuted(boolean muted) {
        this.muted = muted;
        buffer.clear();
        audioTrack.flush();
    }


    public void enqueueAudioFrame(AudioFrame audioFrame) throws InterruptedException {
        buffer.put(audioFrame);
    }


    @Override
    public void run() {
        synchronized (this) {
            audioTrack.flush();
            audioTrack.play();
        }

        while (true) {
            try {
                AudioFrame frame = buffer.take();
                byte[] bytes = frame.getBytes();
                synchronized (this) {
                    audioTrack.write(bytes, 0, bytes.length);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    @Override
    public void failedToConnect(String errorMsg) {
        //pass
    }

    @Override
    public void connectionEstablished(Socket socket) {
        //pass
    }

    @Override
    public void connectionLost(Socket socket) {
        setMuted(true);
    }
}
