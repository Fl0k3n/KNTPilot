package com.example.pilot.networking.udp;

import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.networking.tcp.MsgCode;
import com.example.pilot.security.MessageSecurityPreprocessor;
import com.example.pilot.security.exceptions.AuthenticationException;
import com.example.pilot.security.exceptions.SecurityException;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;

public class MediaReceiver implements ConnectionStatusObserver {
    private final static String IP_ADDR = "0.0.0.0";

    private final int port;
    private final static int MAX_DATAGRAM_SIZE = 1500;
    private DatagramSocket socket;

    private final MessageSecurityPreprocessor securityPreprocessor;

    private final MediaStreamHandler[] mediaStreamHandlers;
    private final FragmentAssembler[] fragmentAssemblers;

    private final ExecutorService executorService;
    private Future<?> receiverTask;


    @Inject
    public MediaReceiver(@Named("client udp port") int port,
                         @Named("UDP preprocessor") MessageSecurityPreprocessor securityPreprocessor,
                         @Named("receiver executor") ExecutorService executorService)
    {
        this.port = port;
        this.securityPreprocessor = securityPreprocessor;

        int arraySize = getArraySize();

        mediaStreamHandlers = new MediaStreamHandler[arraySize];
        fragmentAssemblers  = new FragmentAssembler[arraySize];

        this.executorService = executorService;
    }

    private int getArraySize() {
        MsgCode[] codes = MsgCode.values();
        return codes[codes.length - 1].ordinal() + 1;
    }

    public void addMediaStreamHandler(MediaStreamHandler mediaStreamHandler, boolean requiresFragmentation) {
        int arrayIdx = mediaStreamHandler.getMediaType().ordinal();
        mediaStreamHandlers[arrayIdx] = mediaStreamHandler;

        FragmentAssembler fragmentAssembler = new FragmentAssembler(requiresFragmentation);

        if (requiresFragmentation)
            mediaStreamHandler.addStreamSkippedObserver(fragmentAssembler);

        fragmentAssemblers[arrayIdx] = fragmentAssembler;
    }


    private FragmentAssembler getFragmentAssembler(MediaCode code) {
        return fragmentAssemblers[code.ordinal()];
    }

    private MediaStreamHandler getStreamHandler(MediaCode code) {
        return mediaStreamHandlers[code.ordinal()];
    }

    public void initReceiverTask() {
        receiverTask = executorService.submit(() -> {
            System.out.println("MediaReceiver listening");
            byte[] buf = new byte[MAX_DATAGRAM_SIZE];
            try {
                while (true) {
                    DatagramPacket datagramPacket = new DatagramPacket(buf, MAX_DATAGRAM_SIZE);
                    socket.receive(datagramPacket);

                    try {
                        byte[] packet = preprocessPacket(datagramPacket);
                        MediaCode code = MediaFrame.extractCode(packet);
                        FragmentAssembler assembler = getFragmentAssembler(code);

                        Optional<MediaFrame> mediaFrame = assembler.handleDatagram(packet, packet.length);

                        if (mediaFrame.isPresent()) {
                            getStreamHandler(code).addMediaFrame(mediaFrame.get());
                        }
                    } catch (InterruptedException consumed) {
                        System.out.println("media receiver Interrupted, exiting");
                        return;
                    } catch (AuthenticationException e) {
                        // probably udp error, ignore this frame
                        System.out.println("Auth failed for Udp message " + e.getMessage());
                        e.printStackTrace();
                    } catch (SecurityException e) {
                        System.out.println("Security error while receiving, terminating. " + e.getMessage());
                        e.printStackTrace();
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("Failed to handle datagram from " + datagramPacket.getAddress() + "\n" + e.getMessage());
                    }
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                if (Thread.currentThread().isInterrupted()) {
                    System.out.println("media receiver interrupted, exiting");
                }
            }
        });
    }

    private byte[] preprocessPacket(DatagramPacket datagramPacket) throws AuthenticationException, SecurityException {
        int length = datagramPacket.getLength();
        byte[] TLSPacket = new byte[length];
        System.arraycopy(datagramPacket.getData(), 0, TLSPacket, 0, length);
        return securityPreprocessor.preprocessReceived(TLSPacket);
    }


    @Override
    public void failedToConnect(String errorMsg) {
        // pass
    }

    @Override
    public synchronized void connectionEstablished(Socket serverSocket) {
        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(IP_ADDR, port));

            initReceiverTask();
        } catch (SocketException e) {
            System.out.println("Failed to open socket");
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void connectionLost(Socket serverSocket) {
        if (socket != null) {
            socket.close();
            resetFragmentAssemblers();

            if (receiverTask != null) {
                if (receiverTask.cancel(true)) {
                    System.out.println("MediaReceiver stopped");
                }
                receiverTask = null;
            }

        }
    }

    public void setMediaTransportKey(byte[] decoded) {
        this.securityPreprocessor.setKey(decoded);
    }

    private void resetFragmentAssemblers() {
        for (FragmentAssembler assembler: fragmentAssemblers) {
            if (assembler != null)
                assembler.clearBuffer();
        }
    }
}
