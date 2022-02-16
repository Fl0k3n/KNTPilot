package com.example.pilot.ui.utils;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.networking.udp.MediaCode;
import com.example.pilot.networking.udp.MediaFrame;

import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SoundPlayer implements MediaPlayer, ConnectionStatusObserver, Runnable {
    private static final int QUEUE_CAPACITY = 1;
    private final BlockingQueue<MediaFrame> buffer;
    private final AudioTrack audioTrack;
    private boolean muted;
    private final int sampleRate;
    private final int samplesInSingleFrame;

    public SoundPlayer() {
        this(true, 44100, 384, 256);
    }

    public SoundPlayer(boolean isMuted, int sampleRate, int internalBufferSize, int samplesInSingleFrame) {
        this.muted = isMuted;
        this.sampleRate = sampleRate;
        this.samplesInSingleFrame = samplesInSingleFrame;
        buffer = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

        audioTrack = initAudioPlayer(internalBufferSize);
    }


    private AudioTrack initAudioPlayer(int internalBufferSize) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build();

        AudioFormat format = new AudioFormat.Builder()
                        .setSampleRate(sampleRate)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .build();

        return new AudioTrack(attributes, format,
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


    @Override
    public void run() {
        synchronized (this) {
            audioTrack.flush();
            audioTrack.play();
        }

        while (true) {
            try {
                MediaFrame frame = buffer.take();
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

    @Override
    public void enqueueMediaFrame(MediaFrame mediaFrame) throws InterruptedException {
        buffer.put(mediaFrame);
    }

    @Override
    public float getFrameTimeSpanMs() {
        float samplePeriod = 1.0f / sampleRate * 1000; // in ms

        return samplesInSingleFrame * samplePeriod;
    }

    @Override
    public MediaCode getMediaType() {
        return MediaCode.AUDIO_FRAME;
    }
}
