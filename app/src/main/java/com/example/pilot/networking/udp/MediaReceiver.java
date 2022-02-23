package com.example.pilot.networking.udp;

import android.util.Log;

import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.networking.tcp.MsgCode;
import com.example.pilot.security.MessageSecurityPreprocessor;
import com.example.pilot.security.exceptions.AuthenticationException;
import com.example.pilot.security.exceptions.SecurityException;
import com.example.pilot.ui.utils.FPSCounter;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class MediaReceiver implements ConnectionStatusObserver {
    private final static String TAG = "Media Receiver";
    private final static String IP_ADDR = "0.0.0.0";

    private int port;
    private final static int MAX_DATAGRAM_SIZE = 1500;
    private DatagramSocket socket;

    private final MessageSecurityPreprocessor securityPreprocessor;

    private final MediaStreamHandler[] mediaStreamHandlers;
    private final FragmentAssembler[] fragmentAssemblers;

    private final ExecutorService executorService;
    private Future<?> receiverTask;

    private final FPSCounter fpsCounter;


    @Inject
    public MediaReceiver(@Named("client udp port") int port,
                         @Named("UDP preprocessor") MessageSecurityPreprocessor securityPreprocessor,
                         @Named("receiver executor") ExecutorService executorService,
                         FPSCounter fpsCounter)
    {
        this.port = port;
        this.securityPreprocessor = securityPreprocessor;
        this.fpsCounter = fpsCounter;

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
            Log.d(TAG, "Listenning");
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
                            if (code == MediaCode.VIDEO_FRAME)
                                fpsCounter.onFrameReceived();
                            getStreamHandler(code).addMediaFrame(mediaFrame.get());
                        }
                    } catch (InterruptedException consumed) {
                        Log.d(TAG, "media receiver Interrupted, exiting");
                        return;
                    } catch (AuthenticationException e) {
                        // probably udp error, ignore this frame
                        Log.w(TAG, "Auth failed for Udp message ", e);
                    } catch (SecurityException e) {
                        Log.e(TAG,"Security error while receiving, terminating. ", e);
                        return;
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to handle datagram from " + datagramPacket.getAddress(), e);
                    }
                }
            } catch (IOException exception) {
                if (Thread.currentThread().isInterrupted()) {
                    Log.d(TAG, "media receiver interrupted, exiting");
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
            Log.d(TAG, "listening at " + IP_ADDR + ":" + port);

            initReceiverTask();
        } catch (SocketException e) {
            Log.wtf(TAG, "Failed to open socket", e);
        }
    }

    @Override
    public synchronized void connectionLost(Socket serverSocket) {
        if (socket != null) {
            socket.close();
            resetFragmentAssemblers();

            if (receiverTask != null) {
                if (receiverTask.cancel(true)) {
                    Log.i(TAG, "Media Receiver stopped");
                }
                else {
                    Log.w(TAG, "Failed to cancel receiver");
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

    public void clearFragmentBuffer(MediaCode code) {
        getFragmentAssembler(code).clearBuffer();
        fpsCounter.forceSlowDownTo(5);
    }
}
