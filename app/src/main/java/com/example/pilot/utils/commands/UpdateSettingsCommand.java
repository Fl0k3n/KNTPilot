package com.example.pilot.utils.commands;

import com.example.pilot.ui.views.SettingsHandler;
import com.example.pilot.networking.tcp.ConnectionHandler;
import com.example.pilot.utils.PreferencesLoader;

public class UpdateSettingsCommand implements Command {
    private final ConnectionHandler connectionHandler;
    private final PreferencesLoader preferencesLoader;
    private final SettingsHandler settingsHandler;

    public UpdateSettingsCommand(ConnectionHandler handler,
                                 PreferencesLoader preferencesLoader,
                                 SettingsHandler settingsHandler) {
        this.connectionHandler = handler;
        this.preferencesLoader = preferencesLoader;
        this.settingsHandler = settingsHandler;
    }

    @Override
    public void execute() {
        String ipAddr = settingsHandler.getIpAddr();
        Integer port = settingsHandler.getPortNumber();

        if (ipAddr != null)
            preferencesLoader.saveIpAddr(ipAddr);
        else
            ipAddr = preferencesLoader.getIPAddr();

        if (port != null)
            preferencesLoader.savePort(port);
        else
            port = preferencesLoader.getPort();


        connectionHandler.setConnectionParams(ipAddr, port);
    }
}
