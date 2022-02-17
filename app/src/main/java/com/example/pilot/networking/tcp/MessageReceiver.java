package com.example.pilot.networking.tcp;

import com.example.pilot.networking.observers.AuthStatusObserver;
import com.example.pilot.networking.observers.MessageRcvdObserver;
import com.example.pilot.networking.udp.MediaReceiver;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;
import java.util.LinkedList;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MessageReceiver implements MessageRcvdObserver {
    private final LinkedList<AuthStatusObserver> authStatusObservers;
    private final MediaReceiver mediaReceiver;
    private final MessageSender messageSender;

    private static class JsonMessage {
        public final MsgCode code;
        public final JSONObject value;

        public JsonMessage(MsgCode code, JSONObject value) {
            this.code = code;
            this.value = value;
        }

        @Override
        public String toString() {
            return "JsonMessage{" +
                    "code=" + code +
                    ", value=" + value +
                    '}';
        }
    }

    @Inject
    public MessageReceiver(MediaReceiver mediaReceiver, MessageSender messageSender) {
        this.mediaReceiver = mediaReceiver;
        this.messageSender = messageSender;
        this.authStatusObservers = new LinkedList<>();
    }


    public void addAuthStatusObserver(AuthStatusObserver obs) {
        this.authStatusObservers.add(obs);
    }

    @Override
    public void onMessageReceived(String jsonData) {
        try {
            JsonMessage message = parseMsg(jsonData);
            handleMessage(message);
        } catch (JSONException e) {
            System.out.println("Rcvd unparsable json msg");
            System.out.println(jsonData);
            System.out.println(e.getMessage());
        }
    }

    private JsonMessage parseMsg(String jsonData) throws JSONException {
        JSONObject parsed = new JSONObject(jsonData);
        MsgCode code = MsgCode.fromInteger(parsed.getInt("code"));
        JSONObject value = parsed.getJSONObject("body");
        return new JsonMessage(code, value);
    }

    private void handleMessage(JsonMessage message) throws JSONException {
        switch(message.code) {
            case AUTH_CHECKED:
                handleAuthMessage(message.value);
                break;
            case UDP_SECRET:
                handleUdpSecret(message.value);
                break;
            default:
                throw new IllegalArgumentException("Rcvd unsupported msg code\n" + message);
        }
    }

    private void handleAuthMessage(JSONObject value) throws JSONException {
        boolean is_granted = value.getBoolean("is_granted");
        System.out.println("GOT AUTH: " + is_granted);
        authStatusObservers.forEach(is_granted ?
                AuthStatusObserver::authSucceeded :
                AuthStatusObserver::authFailed);

    }

    private void handleUdpSecret(JSONObject value) throws JSONException {
        byte[] decoded = Base64.getDecoder().decode(value.getString("secret"));
        mediaReceiver.setMediaTransportKey(decoded);
        messageSender.sendMediaSecretChannelAck();
    }

}
