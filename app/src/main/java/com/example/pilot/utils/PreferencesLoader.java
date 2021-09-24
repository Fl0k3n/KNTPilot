package com.example.pilot.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Pair;

import com.example.pilot.BuildConfig;
import com.example.pilot.R;

public class PreferencesLoader {
    private Context ctx;
    private final String PORT_KEY = "PORT";
    private final String IP_ADDR_KEY = "IP_ADDR";

    public PreferencesLoader(Context ctx) {
        this.ctx = ctx;
    }

    public Pair<String, Integer> loadConnectionParams() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        String ip = sharedPreferences.getString(IP_ADDR_KEY, BuildConfig.IP_ADDR);
        Integer port = sharedPreferences.getInt(PORT_KEY, Integer.parseInt(BuildConfig.PORT));
        return new Pair<>(ip, port);
    }

    private SharedPreferences getSharedPreferences() {
        return ctx.getSharedPreferences(
                ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }

    public void savePort(int port) {
        SharedPreferences sharedPreferences = getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(PORT_KEY, port);
        editor.apply();
    }

    public void saveIpAddr(String ipAddr) {
        SharedPreferences sharedPreferences = getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(IP_ADDR_KEY, ipAddr);
        editor.apply();
    }
}
