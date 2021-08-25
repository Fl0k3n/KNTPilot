package com.example.pilot.networking;

import com.example.pilot.utils.ScreenShot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;
import java.util.LinkedList;


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

    public void msgRcvd(String jsonData) {
        try {
            JSONObject parsed = new JSONObject(jsonData);
            MsgCode code = MsgCode.fromInteger(parsed.getInt("code"));
            String value = parsed.getString("body");
            handleMessage(code, value);
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

    }

}
