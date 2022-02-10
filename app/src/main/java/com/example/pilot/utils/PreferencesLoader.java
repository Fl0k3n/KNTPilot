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
    private final String CA_PUBLIC_KEY_PATH_KEY = "CA_PUBLIC_KEY_PATH_KEY";

    public PreferencesLoader(Context ctx) {
        this.ctx = ctx;
    }

    private SharedPreferences getSharedPreferences() {
        return ctx.getSharedPreferences(
                ctx.getString(R.string.preference_file_key), Context.MODE_PRIVATE);
    }

    public int getPort()  {
        return getSharedPreferences().getInt(PORT_KEY, Integer.parseInt(BuildConfig.PORT));
    }

    public String getIPAddr() {
        return getSharedPreferences().getString(IP_ADDR_KEY, BuildConfig.IP_ADDR);
    }

    public String getCAPublicKeyPath() {
        return getSharedPreferences().getString(CA_PUBLIC_KEY_PATH_KEY, BuildConfig.CA_PUBLIC_KEY_PATH);
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
