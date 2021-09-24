package com.example.pilot.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.example.pilot.R;
import com.example.pilot.ui.events.ImageScaleListener;
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


public class MainActivity extends AppCompatActivity {
    private MainUIHandler uiHandler;
    private NetworkHandler networkHandler;
    private MessageHandler messageHandler;
    private ScaleGestureDetector detector;
    private SoundPlayer soundPlayer;
    private PreferencesLoader preferencesLoader;
    private SettingsHandler settingsHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        preferencesLoader = new PreferencesLoader(this);
        Pair<String, Integer> connectionParams = preferencesLoader.loadConnectionParams();

        networkHandler = new NetworkHandler(connectionParams.first, connectionParams.second);
        messageHandler = new MessageHandler(networkHandler);
        uiHandler = new MainUIHandler(messageHandler, this);
        settingsHandler = new SettingsHandler(this);

        soundPlayer = new SoundPlayer();

        messageHandler.addAuthStatusObserver(uiHandler);
        messageHandler.addSSRcvdObserver(uiHandler);
        messageHandler.addAudioFrameRcvdObserver(soundPlayer);

        networkHandler.addMsgRcvdObserver(messageHandler);
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

        Pair<String, Integer> params = preferencesLoader.loadConnectionParams();
        settingsHandler.setDefaults(params.first, params.second);
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
}