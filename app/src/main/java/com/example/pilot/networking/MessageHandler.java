package com.example.pilot.networking;

import android.util.Pair;

import com.example.pilot.utils.ScreenShot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;


public class MessageHandler implements MessageRcvdObserver {
    private final LinkedList<SsRcvdObserver> ssRcvdObservers;
    private final Sender sender;

    public MessageHandler(Sender sender) {
        this.sender = sender;
        this.ssRcvdObservers = new LinkedList<>();
    }

    public void addSSRcvdObserver(SsRcvdObserver obs) {
        this.ssRcvdObservers.add(obs);
    }

    private Pair<MsgCode, String> parseMsg(String jsonData) throws JSONException {
        JSONObject parsed = new JSONObject(jsonData);
        MsgCode code = MsgCode.fromInteger(parsed.getInt("code"));
        String value = parsed.getString("body");
        return new Pair<>(code, value);
    }

    private String buildStringMsg(MsgCode code, Object value) throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("code", code.ordinal());
        msg.put("body", value);
        return msg.toString();
    }

    public void msgRcvd(String jsonData) {
        try {
            Pair<MsgCode, String> parsed = parseMsg(jsonData);
            handleMessage(parsed.first, parsed.second);
        } catch (JSONException e) {
            System.out.println("Rcvd unparsable json msg");
            System.out.println(jsonData);
            System.out.println(e.getMessage());
        }
    }

    private void handleMessage(MsgCode code, String value) {
        if (code == MsgCode.SSHOT) {
            byte[] decoded = Base64.getDecoder().decode(value);
            ScreenShot ss = new ScreenShot(decoded);
            this.ssRcvdObservers.forEach(obs -> obs.onScreenShotRcvd(ss));
        }
    }

    public void sendSwipeMessage(float x0, float y0, float x1, float y1) {
        int dx = (int)(x1 - x0), dy = (int)(y1 - y0);

        try {
            JSONObject inner = new JSONObject();
            inner.put("dx", dx);
            inner.put("dy", dy);

            String jsonMsg = buildStringMsg(MsgCode.MOVE_SCREEN, inner);
            sender.enqueueJsonMessageRequest(jsonMsg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
