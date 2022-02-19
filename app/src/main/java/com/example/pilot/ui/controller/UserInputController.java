package com.example.pilot.ui.controller;

import com.example.pilot.networking.observers.AuthStatusObserver;
import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.networking.tcp.MessageSender;
import com.example.pilot.networking.udp.MediaReceiver;
import com.example.pilot.networking.udp.MediaStreamHandler;
import com.example.pilot.ui.events.ImageInteractionObserver;
import com.example.pilot.ui.utils.KeyboardInputObserver;
import com.example.pilot.utils.KeyboardModifier;
import com.example.pilot.utils.SpecialKeyCode;

import java.net.Socket;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class UserInputController implements ConnectionStatusObserver, KeyboardInputObserver, AuthStatusObserver, ImageInteractionObserver {
    private final MessageSender messageSender;
    private final MediaReceiver mediaReceiver;
    private final MediaStreamHandler videoStreamHandler;
    private final MediaStreamHandler audioStreamHandler;


    @Inject
    public UserInputController(MessageSender messageSender, MediaReceiver mediaReceiver,
                               @Named("video stream handler") MediaStreamHandler videoStreamHandler,
                               @Named("audio stream handler") MediaStreamHandler audioStreamHandler)
    {
        this.messageSender = messageSender;
        this.mediaReceiver = mediaReceiver;
        this.videoStreamHandler = videoStreamHandler;
        this.audioStreamHandler = audioStreamHandler;

    }

    @Override
    public void onKeyPressed(char key, SpecialKeyCode code, List<KeyboardModifier> modifiers) {
        messageSender.sendKeyboardInput(key, code, modifiers);
    }

    @Override
    public void failedToConnect(String errorMsg) {
        // pass
    }

    @Override
    public void connectionEstablished(Socket socket) {
        // pass
    }

    @Override
    public void connectionLost(Socket socket) {
        videoStreamHandler.stop();
        audioStreamHandler.stop();
    }

    @Override
    public void authSucceeded() {
        videoStreamHandler.start();
    }

    @Override
    public void authFailed() {
        // pass
    }

    @Override
    public void onInteractionDetected() {
        // for better responsiveness ignore already partially stored frames
//        mediaReceiver.clearFragmentBuffer(videoStreamHandler.getMediaType());
    }
}
