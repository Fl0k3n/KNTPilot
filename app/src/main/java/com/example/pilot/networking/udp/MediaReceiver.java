package com.example.pilot.networking.udp;

import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.ui.utils.AudioFrame;
import com.example.pilot.ui.utils.AudioStreamHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class MediaReceiver implements Runnable, ConnectionStatusObserver {
    private final static String IP_ADDR = "0.0.0.0";

    private final static int MAX_AUDIO_PACKET_SIZE = 1500;

    private DatagramSocket socket;

    private final AudioStreamHandler audioStreamHandler;

    private Thread rcvrThread;

    private final int port;

    public MediaReceiver(int port, AudioStreamHandler audioStreamHandler) {
        this.port = port;
        this.audioStreamHandler = audioStreamHandler;
    }


    @Override
    public void run() {
        System.out.println("Media listening");
        byte[] buf = new byte[MAX_AUDIO_PACKET_SIZE];
        try {
            while (true) {
                DatagramPacket datagramPacket = new DatagramPacket(buf, MAX_AUDIO_PACKET_SIZE);
                socket.receive(datagramPacket);
                try {
                    int code = datagramPacket.getData()[0];
                    if (code == 1) {
                        handleAudioFrame(datagramPacket);
                    }
                    else {
                        System.out.println("Invalid code " + code);
                    }
                } catch (Exception e) {
                    // TODO
                    System.out.println("Failed to handle datagram from " + datagramPacket.getAddress() + "\n" + datagramPacket);
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void handleAudioFrame(DatagramPacket datagramPacket) {
        AudioFrame audioFrame = decodeAudioFrame(ByteBuffer.wrap(datagramPacket.getData()));

        audioStreamHandler.addAudioFrame(audioFrame);
    }

    private AudioFrame decodeAudioFrame(ByteBuffer byteBuffer) {
        byteBuffer.order(ByteOrder.BIG_ENDIAN);

        byteBuffer.get(); // code
        short size = byteBuffer.getShort();

        byteBuffer.get(); // reserved

        int signedIntSeq = byteBuffer.getInt();
        long seq = signedIntSeq & 0xffffffffL; // convert 4b unsigned int to long

        System.out.println("SEQ: " + seq);

        short offset = byteBuffer.getShort();

        byteBuffer.get(); // reserved
        byteBuffer.get(); // reserved

        AudioFrame audioFrame = new AudioFrame(size, seq);
        audioFrame.putFragment(byteBuffer, offset, size);

        if (!audioFrame.isFullyRecvd())
            throw new RuntimeException("Fragmented frames not supported yet"); //TODO

        return audioFrame;
    }

    @Override
    public void failedToConnect(String errorMsg) {
        // pass
    }

    @Override
    public void connectionEstablished(Socket serverSocket) {
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(IP_ADDR, port));

            rcvrThread = new Thread(this);
            rcvrThread.setDaemon(true);
            rcvrThread.start();
        } catch (SocketException e) {
            System.out.println("Failed to open socket");
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Socket serverSocket) {
        socket.close(); // TODO
    }
}
