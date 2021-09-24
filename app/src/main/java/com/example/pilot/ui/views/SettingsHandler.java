package com.example.pilot.ui.views;

import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.example.pilot.R;
import com.example.pilot.utils.commands.Command;
import java.util.Locale;

public class SettingsHandler {
    private AppCompatActivity activity;
    private EditText ipAddrInput, portNumberInput;
    public SettingsHandler(AppCompatActivity activity) {
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
}
