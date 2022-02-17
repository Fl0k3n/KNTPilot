package com.example.pilot.networking.tcp;

import com.example.pilot.networking.observers.SsRcvdObserver;
import com.example.pilot.utils.ScreenShot;
import com.example.pilot.utils.SpecialKeyCode;
import com.example.pilot.utils.KeyboardModifier;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


public class MessageSender implements AuthSender, SsRcvdObserver {
    private final Sender sender;

    public MessageSender(Sender sender) {
        this.sender = sender;
    }

    private String buildStringMsg(MsgCode code, Object value) throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("code", code.ordinal());
        msg.put("body", value);
        return msg.toString();
    }


    public void sendSwipeMessage(float real_dx, float real_dy) {
        int dx = (int)real_dx, dy = (int)real_dy;
        System.out.printf("SWIPED: %d x %d\n", dx, dy);
        try {
            JSONObject inner = new JSONObject();
            inner.put("dx", dx);
            inner.put("dy", dy);

            sendMsg(MsgCode.MOVE_SCREEN, inner);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendClickMessage(float x, float y) {
        try {
            JSONObject inner = new JSONObject();
            inner.put("x", x);
            inner.put("y", y);

            sendMsg(MsgCode.CLICK, inner);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void sendMsg(MsgCode code, Object value) throws JSONException {
        sender.enqueueJsonMessageRequest(buildStringMsg(code, value));
    }

    public void changeMonitor() {
        try {
            sender.enqueueJsonMessageRequest(buildStringMsg(MsgCode.CHANGE_MONITOR, ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void rescaleImage(float ratio) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("ratio", (double)ratio);
            sender.enqueueJsonMessageRequest(buildStringMsg(MsgCode.RESCALE, jsonObject));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendKeyboardInput(char key, SpecialKeyCode code, List<KeyboardModifier> modifiers) {
        try {
            JSONObject jsonObject = new JSONObject();
            String tmp = "" + key;
            jsonObject.put("key", tmp);
            jsonObject.put("special_code", code.ordinal());

            if (modifiers == null)
                jsonObject.put("key_modes", new LinkedList<>());
            else
                jsonObject.put("key_modes",
                        modifiers.stream().map(Enum::ordinal).collect(Collectors.toList()));

            sender.enqueueJsonMessageRequest(buildStringMsg(MsgCode.KEYBOARD_INPUT, jsonObject));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void sendCredentials(String password) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("password", password);
            sender.enqueueJsonMessageRequest(buildStringMsg(MsgCode.AUTH, jsonObject));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendScrollMessage(boolean up) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("up", up);
            sender.enqueueJsonMessageRequest(buildStringMsg(MsgCode.SCROLL, jsonObject));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendMuteMessage(boolean isMuted) {
        try {
            sender.enqueueJsonMessageRequest(
                    buildStringMsg(isMuted ? MsgCode.MUTE : MsgCode.UNMUTE, ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendMediaSecretChannelAck() {
        try {
            sender.enqueueJsonMessageRequest(buildStringMsg(MsgCode.UDP_SECRET_ACK, ""));
        } catch (JSONException jsonException) {
            jsonException.printStackTrace();
        }
    }

    @Override
    public void onScreenShotRcvd(ScreenShot ss) {
        try {
            sender.enqueueJsonMessageRequest(buildStringMsg(MsgCode.SS_RCVD, ""));
            System.out.println("SS MESSAGE ENQUEUED");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
