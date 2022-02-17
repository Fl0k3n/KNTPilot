package com.example.pilot.networking.tcp;

import com.example.pilot.networking.observers.MessageRcvdObserver;
import com.example.pilot.security.MessageSecurityPreprocessor;
import com.example.pilot.security.exceptions.AuthenticationException;
import com.example.pilot.security.exceptions.SecurityException;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class Listener {
    private final LinkedList<MessageRcvdObserver> msgRcvdObservers;
    private final MessageSecurityPreprocessor preprocessor;

    @Inject
    public Listener(@Named("TCP preprocessor") MessageSecurityPreprocessor preprocessor) {
        this.preprocessor = preprocessor;
        this.msgRcvdObservers = new LinkedList<>();
    }

    public synchronized void addMsgRcvdObserver(MessageRcvdObserver obs) {
        msgRcvdObservers.add(obs);
    }

    private byte[] recv(InputStream stream, int size) throws IOException {
        int size_read = 0;
        byte[] buff = new byte[size];

        while (size_read < size) {
            int rd = stream.read(buff, size_read, size - size_read);
            if (rd == -1)
                throw new ConnectException("Connection lost, stream returned -1");
            size_read += rd;
        }

        return buff;
    }


    private String recvMessage(InputStream stream) throws IOException, AuthenticationException, SecurityException {
        byte[] basicHeader = recv(stream, preprocessor.getBasicHeaderSize());
        byte[] msg = recv(stream, preprocessor.getMessageSize(basicHeader));

        byte[] fullMsg = ByteBuffer.allocate(basicHeader.length + msg.length)
                .put(basicHeader)
                .put(msg)
                .array();

        byte[] preprocessedMsg = preprocessor.preprocessReceived(fullMsg);

        return new String(preprocessedMsg, StandardCharsets.UTF_8);
    }


    // blocks as long as connection is established, when connection is lost IOException is thrown
    public void listen(Socket socket) throws IOException, AuthenticationException, SecurityException {
        InputStream stream = socket.getInputStream();

        while (true) {
            String rcvd = recvMessage(stream);
            synchronized (this) {
                this.msgRcvdObservers.forEach(obs -> obs.onMessageReceived(rcvd));
            }
        }
    }
}
