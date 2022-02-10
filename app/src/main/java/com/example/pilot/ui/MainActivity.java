package com.example.pilot.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.example.pilot.R;
import com.example.pilot.networking.MediaReceiver;
import com.example.pilot.security.CertificateVerifier;
import com.example.pilot.security.TCPGuard;
import com.example.pilot.security.TLSHandler;
import com.example.pilot.ui.events.ImageScaleListener;
import com.example.pilot.ui.utils.AudioStreamHandler;
import com.example.pilot.ui.utils.FPSCounter;
import com.example.pilot.ui.utils.SoundPlayer;
import com.example.pilot.ui.views.MainUIHandler;
import com.example.pilot.ui.views.SettingsHandler;
import com.example.pilot.utils.KeyboardModifier;
import com.example.pilot.networking.MessageHandler;
import com.example.pilot.networking.NetworkHandler;
import com.example.pilot.networking.SpecialKeyCode;
import com.example.pilot.utils.commands.ChangeSettingsVisibilityCommand;
import com.example.pilot.utils.PreferencesLoader;
import com.example.pilot.utils.commands.UpdateSettingsCommand;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;


public class MainActivity extends AppCompatActivity {
    private MainUIHandler uiHandler;
    private NetworkHandler networkHandler;
    private MessageHandler messageHandler;
    private ScaleGestureDetector detector;
    private SoundPlayer soundPlayer;
    private PreferencesLoader preferencesLoader;
    private SettingsHandler settingsHandler;
    private AudioStreamHandler audioStreamHandler;
    private MediaReceiver mediaReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("UP*****************************************");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        preferencesLoader = new PreferencesLoader(this);

        saveCAPublicKey(this, preferencesLoader.getCAPublicKeyPath());

        File CAKeyFile = new File(this.getFilesDir(), preferencesLoader.getCAPublicKeyPath());

        TCPGuard tcpGuard = null;
        try {
            tcpGuard = new TCPGuard();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }

        CertificateVerifier certificateVerifier = new CertificateVerifier(CAKeyFile);
        TLSHandler tlsHandler = new TLSHandler(certificateVerifier, tcpGuard);

        networkHandler = new NetworkHandler(preferencesLoader.getIPAddr(), preferencesLoader.getPort(), tlsHandler);
        messageHandler = new MessageHandler(networkHandler);
        uiHandler = new MainUIHandler(messageHandler, this);
        settingsHandler = new SettingsHandler(this);

        soundPlayer = new SoundPlayer();

        // TODO args from somewhere
        audioStreamHandler = new AudioStreamHandler(soundPlayer, 250, 44100,  256);

        mediaReceiver = new MediaReceiver(preferencesLoader.getPort(), audioStreamHandler);

        messageHandler.addAuthStatusObserver(uiHandler);
        messageHandler.addSSRcvdObserver(uiHandler);

        networkHandler.addMsgRcvdObserver(messageHandler);

        networkHandler.addConnectionStatusObserver(mediaReceiver);
        networkHandler.addConnectionStatusObserver(uiHandler);
        networkHandler.addConnectionStatusObserver(soundPlayer);

        initScaleDetector();

        Thread network = new Thread(networkHandler);
        network.start();

        FPSCounter fpsCounter = new FPSCounter(uiHandler);
        messageHandler.addSSRcvdObserver(fpsCounter);

        Thread timer = new Thread(fpsCounter);
        timer.start();

        Thread soundThread = new Thread(soundPlayer);
        soundThread.start();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        menu.findItem(R.id.keyboardBtn)
                .setOnMenuItemClickListener(c -> {
                    uiHandler.changeKeyboardVisibility(false);
                    return true;
                });


        menu.findItem(R.id.monitorBtn).setOnMenuItemClickListener(e -> {
            messageHandler.changeMonitor();
            return true;
        });

        menu.findItem(R.id.WinBtn).setOnMenuItemClickListener(e -> {
           messageHandler.sendKeyboardInput('\0', SpecialKeyCode.WINDOWS_KEY, null);
           return true;
        });

        menu.findItem(R.id.upBtn).setOnMenuItemClickListener(e -> {
            messageHandler.sendScrollMessage(true);
            return true;
        });

        menu.findItem(R.id.downBtn).setOnMenuItemClickListener(e -> {
            messageHandler.sendScrollMessage(false);
            return true;
        });

        menu.findItem(R.id.backspaceBtn).setOnMenuItemClickListener(e -> {
            messageHandler.sendKeyboardInput('\0', SpecialKeyCode.BACKSPACE, null);
           return true;
        });

        menu.findItem(R.id.muteBtn).setOnMenuItemClickListener(e -> {
            boolean isMuted = soundPlayer.isMuted();
            audioStreamHandler.restart(); // TODO
            soundPlayer.setMuted(!isMuted);
            uiHandler.changeMuteIcon(!isMuted);
            messageHandler.sendMuteMessage(!isMuted);
            return true;
        });


        menu.findItem(R.id.CtrlBtn)
                .setOnMenuItemClickListener(e -> modifierHandler(KeyboardModifier.CTRL_KEY));

        menu.findItem(R.id.AltBtn)
                .setOnMenuItemClickListener(e -> modifierHandler(KeyboardModifier.ALT_KEY));

        menu.findItem(R.id.ShiftBtn)
                .setOnMenuItemClickListener(e -> modifierHandler(KeyboardModifier.SHIFT_KEY));

        setupSettings(menu);

        uiHandler.setMenu(menu);
        uiHandler.changeMenuItemsVisibility(true);

        return super.onCreateOptionsMenu(menu);
    }

    private void setupSettings(Menu menu) {
        MenuItem settingsBtn = menu.findItem(R.id.settingsBtn);

        ChangeSettingsVisibilityCommand closeCmd =
                new ChangeSettingsVisibilityCommand(this, settingsBtn, true);
        ChangeSettingsVisibilityCommand showCmd =
                new ChangeSettingsVisibilityCommand(this, settingsBtn, false);
        UpdateSettingsCommand updateCmd =
                new UpdateSettingsCommand(networkHandler, preferencesLoader, settingsHandler);

        settingsHandler.setOnCloseSettingsCommand(closeCmd);
        settingsHandler.setOnSaveSettingsCommand(() -> {
            updateCmd.execute();
            closeCmd.execute();
        });

        settingsBtn.setOnMenuItemClickListener(e -> {
            showCmd.execute();
            return true;
        });

        settingsHandler.setDefaults(preferencesLoader.getIPAddr(), preferencesLoader.getPort());
    }

    private boolean modifierHandler(KeyboardModifier modifier) {
        uiHandler.changeKeyboardModifier(modifier);
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        detector.onTouchEvent(motionEvent);
        return true;
    }

    private void initScaleDetector() {
        ImageScaleListener listener = new ImageScaleListener() {
            @Override
            public void scaled(float ratio) {
                messageHandler.rescaleImage(ratio);
            }
        };

        detector = new ScaleGestureDetector(this, listener);
    }

    private void saveCAPublicKey(Context ctx, String path) {
        // TODO
        String key = "-----BEGIN PUBLIC KEY-----\n" +
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAx1317n8rtVa0lcj5GERl\n" +
                "FZ4CtW+oWLP/Frqt8lh9E+yQSR+jV5Wj/1yfrV8ybfzU4d+KdR1pejQD8r1mGp6/\n" +
                "0cxrpoRszE6x5H1ZYUBQFvG6Pqdvi/WEp2t9lLrgTCoUs+7KXRb1WUhnB4afGpoW\n" +
                "a9FcqQOluQTi9YPTAEsrzp0u6HqtZUXx52784fvCc78smcTERaRnd5nt01Z6Sz0a\n" +
                "lsjYtnkH2ZycIYHTsql8uHvHmrogIO4IJvstv7YO4k8/AeVvWENS07BnTS2tnNhu\n" +
                "5URWIS+ybs4L9owFxweKUw5MUnBIKRwehQEa47L1naBIXSwkWdNja+VXvy6sXtRl\n" +
                "UwIDAQAB\n" +
                "-----END PUBLIC KEY-----";

        File keyFile = new File(ctx.getFilesDir(), path);
        try (FileOutputStream fileOutputStream = new FileOutputStream(keyFile);
             DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream)) {
            dataOutputStream.writeBytes(key);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("failed to save key");
        }
    }
}