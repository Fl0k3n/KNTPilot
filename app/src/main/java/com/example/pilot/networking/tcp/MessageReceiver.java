package com.example.pilot.networking.tcp;

import android.util.Pair;

import com.example.pilot.networking.observers.AuthStatusObserver;
import com.example.pilot.networking.observers.MessageRcvdObserver;
import com.example.pilot.networking.observers.SsRcvdObserver;
import com.example.pilot.utils.ScreenShot;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Base64;
import java.util.LinkedList;

public class MessageReceiver implements MessageRcvdObserver {
    private final LinkedList<SsRcvdObserver> ssRcvdObservers;
    private final LinkedList<AuthStatusObserver> authStatusObservers;

    public MessageReceiver() {
        this.ssRcvdObservers = new LinkedList<>();
        this.authStatusObservers = new LinkedList<>();
    }


    public void addSSRcvdObserver(SsRcvdObserver obs) {
        this.ssRcvdObservers.add(obs);
    }

    public void addAuthStatusObserver(AuthStatusObserver obs) {
        this.authStatusObservers.add(obs);
    }


    @Override
    public void msgRcvd(String jsonData) {
        try {
            Pair<MsgCode, JSONObject> parsed = parseMsg(jsonData);
            handleMessage(parsed.first, parsed.second);
        } catch (JSONException e) {
            System.out.println("Rcvd unparsable json msg");
            System.out.println(jsonData);
            System.out.println(e.getMessage());
        }
    }

    private Pair<MsgCode, JSONObject> parseMsg(String jsonData) throws JSONException {
        JSONObject parsed = new JSONObject(jsonData);
        MsgCode code = MsgCode.fromInteger(parsed.getInt("code"));
        JSONObject value = parsed.getJSONObject("body");
        return new Pair<>(code, value);
    }

    private void handleMessage(MsgCode code, JSONObject value) throws JSONException {
        switch(code) {
            case SSHOT:
                byte[] decoded = Base64.getDecoder().decode(value.getString("image"));
                ScreenShot ss = new ScreenShot(decoded);
                this.ssRcvdObservers.forEach(obs -> obs.onScreenShotRcvd(ss));
                break;
            case AUTH_CHECKED:
                boolean is_granted = value.getBoolean("is_granted");
                authStatusObservers.forEach(is_granted ?
                        AuthStatusObserver::authSucceeded :
                        AuthStatusObserver::authFailed);

                break;
            default:
                throw new RuntimeException("Rcvd unsupported msg code " + code); // TODO
        }

    }
}
