package com.example.pilot.utils.commands;

import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pilot.R;

public class ChangeSettingsVisibilityCommand implements Command{
    private AppCompatActivity activity;
    private MenuItem settingsBtn;
    private boolean hide;

    public ChangeSettingsVisibilityCommand(AppCompatActivity activity,
                                           MenuItem settingsBtn,
                                           boolean hide) {
        this.activity = activity;
        this.settingsBtn = settingsBtn;
        this.hide = hide;
    }

    @Override
    public void execute() {
        this.activity.findViewById(R.id.settingsLayout).setVisibility(hide ? View.GONE : View.VISIBLE);
        this.activity.findViewById(R.id.authLayout).setVisibility(hide ? View.VISIBLE : View.GONE);
        this.settingsBtn.setVisible(hide);
    }
}
