package com.example.pilot.ui.views;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pilot.R;
import com.example.pilot.networking.tcp.AuthSender;
import com.example.pilot.networking.observers.AuthStatusObserver;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class AuthHandler implements AuthStatusObserver {
    private final AppCompatActivity activity;
    private final View layout;
    private final Button authBtn;
    private final EditText input;

    @Inject
    public AuthHandler(@Named("auth activity") AppCompatActivity activity, AuthSender authSender) {
        this.activity = activity;
        this.layout = activity.findViewById(R.id.authLayout);
        this.input = activity.findViewById(R.id.AuthInput);

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



    public void connectionEstablished() {
        authBtn.setEnabled(true);
    }

    public void connectionLost() {
        authBtn.setEnabled(false);
        layout.setVisibility(View.VISIBLE);
    }

    @Override
    public void authSucceeded() {
        authBtn.setEnabled(false);
        layout.setVisibility(View.GONE);
        input.setText("");
    }

    @Override
    public void authFailed() {
        activity.findViewById(R.id.AuthInput).setEnabled(true);
        authBtn.setEnabled(true);
        Toast.makeText(activity, "Authentication Failed.", Toast.LENGTH_LONG).show();
    }
}
