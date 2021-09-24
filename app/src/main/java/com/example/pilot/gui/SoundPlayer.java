package com.example.pilot.gui;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

import com.example.pilot.networking.AudioFrameRcvdObserver;
import com.example.pilot.utils.AudioFrame;
import com.example.pilot.utils.BlockingQueue;

public class SoundPlayer implements AudioFrameRcvdObserver, Runnable {
    private BlockingQueue<AudioFrame> buffer;
    private AudioTrack audioTrack;
    private boolean muted;

    public SoundPlayer() {
        this(true, 44100, 512);
    }

    public SoundPlayer(boolean isMuted, int sampleRate, int internalBufferSize) {
        muted = isMuted;
        buffer = new BlockingQueue<>();
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
        buffer.flush();
        audioTrack.flush();
    }

    @Override
    public void onAudioFrameRcvd(AudioFrame frame) {
        buffer.put(frame);
    }

    @Override
    public void run() {
        synchronized (this) {
            audioTrack.flush();
            audioTrack.play();
        }

        while (true) {
            try {
                AudioFrame frame = buffer.get();
                byte[] bytes = frame.getBytes();
                synchronized (this) {
                    audioTrack.write(bytes, 0, bytes.length);
                }
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
