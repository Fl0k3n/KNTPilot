package com.example.pilot.utils.commands;

import com.example.pilot.ui.controller.SettingsController;
import com.example.pilot.networking.tcp.ConnectionHandler;
import com.example.pilot.utils.PreferencesLoader;

public class UpdateSettingsCommand implements Command {
    private final ConnectionHandler connectionHandler;
    private final PreferencesLoader preferencesLoader;
    private final SettingsController settingsController;

    public UpdateSettingsCommand(ConnectionHandler handler,
                                 PreferencesLoader preferencesLoader,
                                 SettingsController settingsController) {
        this.connectionHandler = handler;
        this.preferencesLoader = preferencesLoader;
        this.settingsController = settingsController;
    }

    @Override
    public void execute() {
        String ipAddr = settingsController.getIpAddr();
        Integer port = settingsController.getPortNumber();

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
