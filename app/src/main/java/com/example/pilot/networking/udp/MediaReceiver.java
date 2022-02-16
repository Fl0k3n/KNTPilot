package com.example.pilot.networking.udp;

import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.ui.utils.MediaStreamHandler;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Optional;

public class MediaReceiver implements Runnable, ConnectionStatusObserver {
    private final static String IP_ADDR = "0.0.0.0";

    private final int port;
    private final static int MAX_DATAGRAM_SIZE = 1500;
    private DatagramSocket socket;

    private final ArrayList<MediaStreamHandler> mediaStreamHandlers;
    private final ArrayList<FragmentAssembler> fragmentAssemblers;

    private Thread rcvrThread;


    public MediaReceiver(int port) {
        this.port = port;

        int expectedStreamCount = MediaCode.values().length;

        mediaStreamHandlers = new ArrayList<>(expectedStreamCount);
        fragmentAssemblers = new ArrayList<>(expectedStreamCount);
    }

    public void addMediaStreamHandler(MediaStreamHandler mediaStreamHandler, boolean requiresFragmentation) {
        int arrayIdx = mediaStreamHandler.getMediaType().ordinal();
        mediaStreamHandlers.add(arrayIdx, mediaStreamHandler);

        FragmentAssembler fragmentAssembler = new FragmentAssembler(requiresFragmentation);

        if (requiresFragmentation)
            mediaStreamHandler.addStreamSkippedObserver(fragmentAssembler);

        fragmentAssemblers.add(arrayIdx, fragmentAssembler);
    }


    private FragmentAssembler getFragmentAssembler(MediaCode code) {
        return fragmentAssemblers.get(code.ordinal());
    }

    private MediaStreamHandler getStreamHandler(MediaCode code) {
        return mediaStreamHandlers.get(code.ordinal());
    }


    @Override
    public void run() {
        System.out.println("Media listening");
        byte[] buf = new byte[MAX_DATAGRAM_SIZE];
        try {
            while (true) {
                DatagramPacket datagramPacket = new DatagramPacket(buf, MAX_DATAGRAM_SIZE);
                socket.receive(datagramPacket);
                try {
                    MediaCode code = MediaFrame.extractCode(datagramPacket.getData());
                    FragmentAssembler assembler = getFragmentAssembler(code);

                    assembler.handleDatagram(datagramPacket)
                            .ifPresent(mediaFrame -> getStreamHandler(code).addMediaFrame(mediaFrame));
                } catch (Exception e) {
                    // TODO
                    e.printStackTrace();
                    System.out.println("Failed to handle datagram from " + datagramPacket.getAddress() + "\n" + e.getMessage());
                }
            }
        } catch (IOException exception) {
            exception.printStackTrace();
        }
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
