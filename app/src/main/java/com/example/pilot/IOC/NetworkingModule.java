package com.example.pilot.IOC;

import com.example.pilot.networking.tcp.AuthSender;
import com.example.pilot.networking.tcp.MessageSender;
import com.example.pilot.networking.udp.MediaStreamHandler;
import com.example.pilot.ui.utils.SoundPlayer;
import com.example.pilot.ui.utils.VideoPlayer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module
public class NetworkingModule {
    private static final int AUDIO_PREFETCH_MS = 128;
    private static final int VIDEO_PREFETCH_MS = 128;


    private final String serverIpAddr;
    private final int serverPort;
    private final int clientUDPPort;

    public NetworkingModule(String serverIpAddr, int serverPort, int clientUDPPort)
    {
        this.serverPort = serverPort;
        this.serverIpAddr = serverIpAddr;
        this.clientUDPPort = clientUDPPort;
    }


    @Provides
    @Named("server ip address")
    public String provideServerIpAddress() {
        return serverIpAddr;
    }

    @Provides
    @Named("server port")
    public int provideServerPort() {
        return serverPort;
    }

    @Provides
    @Named("client udp port")
    public int provideClientUdpPort() {
        return clientUDPPort;
    }

    @Provides
    @Named("connection executor")
    public ExecutorService provideConnectionExecutorService() {
        return Executors.newSingleThreadExecutor();
    }


    @Provides
    @Named("receiver executor")
    public ExecutorService provideMediaReceiverExecutorService() {
        return Executors.newSingleThreadExecutor();
    }

    @Provides
    @Singleton
    @Named("video stream handler")
    public MediaStreamHandler provideVideoStreamHandler(VideoPlayer videoPlayer) {
        return new MediaStreamHandler(videoPlayer, VIDEO_PREFETCH_MS);
    }

    @Provides
    @Singleton
    @Named("audio stream handler")
    public MediaStreamHandler provideAudioStreamHandler(SoundPlayer soundPlayer) {
        return new MediaStreamHandler(soundPlayer, AUDIO_PREFETCH_MS);
    }

    @Provides
    public AuthSender provideAuthSender(MessageSender messageSender) {
        return messageSender;
    }
}
