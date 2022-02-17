package com.example.pilot.ui.views;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pilot.R;
import com.example.pilot.networking.tcp.ConnectionHandler;
import com.example.pilot.utils.PreferencesLoader;
import com.example.pilot.utils.commands.ChangeSettingsVisibilityCommand;
import com.example.pilot.utils.commands.Command;
import com.example.pilot.utils.commands.UpdateSettingsCommand;

import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

public class SettingsHandler {
    private final AppCompatActivity activity;
    private final EditText ipAddrInput, portNumberInput;

    @Inject
    public SettingsHandler(@Named("settings activity") AppCompatActivity activity) {
        this.activity = activity;
        ipAddrInput = this.activity.findViewById(R.id.ipAddrInput);
        portNumberInput = this.activity.findViewById(R.id.portNumberInput);
    }

    public void setOnSaveSettingsCommand(Command onSaveSettingsCommand) {
        this.activity.findViewById(R.id.saveSettingsBtn)
                .setOnClickListener(e ->
                    onSaveSettingsCommand.execute());
    }

    public void setOnCloseSettingsCommand(Command onCloseSettingsCommand) {
        this.activity.findViewById(R.id.closeSettingsBtn)
                .setOnClickListener(e -> onCloseSettingsCommand.execute());
    }

    public void setDefaults(String ipAddr, int port) {
        ipAddrInput.setText(ipAddr);
        portNumberInput.setText(String.format(Locale.UK ,"%d", port));
    }

    public String getIpAddr() {
        String ipAddr = ipAddrInput.getText().toString();
        if (ipAddr.equals("") || false) // TODO validate
            return null;
        return ipAddr;
    }

    public Integer getPortNumber() {
        String value = portNumberInput.getText().toString();
        Integer port = Integer.parseInt(value.equals("") ? "0" : value);
        if (port <= 0) // TODO validate
            return null;
        return port;
    }

    public void setupSettings(Menu menu, ConnectionHandler connectionHandler, PreferencesLoader preferencesLoader) {
        MenuItem settingsBtn = menu.findItem(R.id.settingsBtn);

        ChangeSettingsVisibilityCommand closeCmd =
                new ChangeSettingsVisibilityCommand(activity, settingsBtn, true);
        ChangeSettingsVisibilityCommand showCmd =
                new ChangeSettingsVisibilityCommand(activity, settingsBtn, false);
        UpdateSettingsCommand updateCmd =
                new UpdateSettingsCommand(connectionHandler, preferencesLoader, this);

        setOnCloseSettingsCommand(closeCmd);
        setOnSaveSettingsCommand(() -> {
            updateCmd.execute();
            closeCmd.execute();
        });

        settingsBtn.setOnMenuItemClickListener(e -> {
            showCmd.execute();
            return true;
        });

        setDefaults(preferencesLoader.getIPAddr(), preferencesLoader.getPort());
    }
}
