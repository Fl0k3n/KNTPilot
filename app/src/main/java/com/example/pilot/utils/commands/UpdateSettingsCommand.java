package com.example.pilot.utils.commands;

import android.util.Pair;

import com.example.pilot.ui.views.SettingsHandler;
import com.example.pilot.networking.NetworkHandler;
import com.example.pilot.utils.PreferencesLoader;

public class UpdateSettingsCommand implements Command {
    private final NetworkHandler networkHandler;
    private final PreferencesLoader preferencesLoader;
    private final SettingsHandler settingsHandler;

    public UpdateSettingsCommand(NetworkHandler handler,
                                 PreferencesLoader preferencesLoader,
                                 SettingsHandler settingsHandler) {
        this.networkHandler = handler;
        this.preferencesLoader = preferencesLoader;
        this.settingsHandler = settingsHandler;
    }

    @Override
    public void execute() {
        String ipAddr = settingsHandler.getIpAddr();
        Integer port = settingsHandler.getPortNumber();
        Pair<String, Integer> oldParams = preferencesLoader.loadConnectionParams();

        if (ipAddr != null)
            preferencesLoader.saveIpAddr(ipAddr);
        else
            ipAddr = oldParams.first;

        if (port != null)
            preferencesLoader.savePort(port);
        else
            port = oldParams.second;


        networkHandler.setConnectionParams(ipAddr, port);
    }
}
