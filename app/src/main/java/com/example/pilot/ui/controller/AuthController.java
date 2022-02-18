package com.example.pilot.ui.controller;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pilot.R;
import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.networking.tcp.AuthSender;
import com.example.pilot.networking.observers.AuthStatusObserver;
import com.example.pilot.ui.utils.GuiRunner;

import java.net.Socket;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class AuthController implements AuthStatusObserver, ConnectionStatusObserver {
    private final AppCompatActivity activity;
    private final View layout;
    private final Button authBtn;
    private final EditText input;
    private final GuiRunner guiRunner;

    @Inject
    public AuthController(@Named("auth activity") AppCompatActivity activity, AuthSender authSender, GuiRunner guiRunner) {
        this.activity = activity;
        this.layout = activity.findViewById(R.id.authLayout);
        this.input = activity.findViewById(R.id.AuthInput);
        this.guiRunner = guiRunner;

        authBtn = activity.findViewById(R.id.authBtn);
        authBtn.setEnabled(false);

        authBtn.setOnClickListener(e -> {
            String passwd = input.getText().toString();
            if (passwd.length() > 0) {
                authSender.sendCredentials(passwd);
                authBtn.setEnabled(false);
            }
        });
    }

    @Override
    public void authSucceeded() {
        guiRunner.scheduleGuiTask(() -> {
            authBtn.setEnabled(false);
            layout.setVisibility(View.GONE);
            input.setText("");
        });
    }

    @Override
    public void authFailed() {
        guiRunner.scheduleGuiTask(() -> {
            activity.findViewById(R.id.AuthInput).setEnabled(true);
            authBtn.setEnabled(true);
            Toast.makeText(activity, "Authentication Failed.", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void failedToConnect(String errorMsg) {
        //pass
    }

    @Override
    public void connectionEstablished(Socket socket) {
        guiRunner.scheduleGuiTask(() -> {
            authBtn.setEnabled(true);
        });
    }

    @Override
    public void connectionLost(Socket socket) {
        guiRunner.scheduleGuiTask(() -> {
            authBtn.setEnabled(false);
            layout.setVisibility(View.VISIBLE);
        });
    }
}
