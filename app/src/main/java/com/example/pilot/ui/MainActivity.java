package com.example.pilot.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.example.pilot.IOC.DaggerAppComponent;
import com.example.pilot.IOC.MediaModule;
import com.example.pilot.IOC.NetworkingModule;
import com.example.pilot.IOC.SecurityModule;
import com.example.pilot.R;
import com.example.pilot.networking.observers.AuthStatusObserver;
import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.networking.tcp.Listener;
import com.example.pilot.networking.udp.MediaReceiver;
import com.example.pilot.networking.tcp.MessageReceiver;
import com.example.pilot.networking.tcp.Sender;
import com.example.pilot.ui.events.ImageScaleListener;
import com.example.pilot.networking.udp.MediaStreamHandler;
import com.example.pilot.ui.utils.GuiRunner;
import com.example.pilot.ui.utils.ImageViewController;
import com.example.pilot.ui.controller.KeyboardController;
import com.example.pilot.ui.utils.SoundPlayer;
import com.example.pilot.ui.controller.AuthController;
import com.example.pilot.ui.controller.MenuController;
import com.example.pilot.ui.controller.SettingsController;
import com.example.pilot.ui.controller.UserInputController;
import com.example.pilot.utils.KeyboardModifier;
import com.example.pilot.networking.tcp.MessageSender;
import com.example.pilot.networking.tcp.ConnectionHandler;
import com.example.pilot.utils.SpecialKeyCode;
import com.example.pilot.utils.PreferencesLoader;


import java.net.Socket;

import javax.inject.Inject;
import javax.inject.Named;

public class MainActivity extends AppCompatActivity implements GuiRunner, ConnectionStatusObserver, AuthStatusObserver {
    private static final String TAG = "MainActivity";

    private ScaleGestureDetector detector;
    private PreferencesLoader preferencesLoader;
    private Button retryConnectionBtn;

    @Inject
    MenuController menuController;

    @Inject
    ConnectionHandler connectionHandler;

    @Inject @Named("video stream handler")
    MediaStreamHandler videoStreamHandler;

    @Inject @Named("audio stream handler")
    MediaStreamHandler audioStreamHandler;

    @Inject
    AuthController authController;

    @Inject
    MediaReceiver mediaReceiver;

    @Inject
    MessageReceiver messageReceiver;

    @Inject
    Sender sender;

    @Inject
    MessageSender messageSender;

    @Inject
    Listener listener;

    @Inject
    SettingsController settingsController;

    @Inject
    SoundPlayer soundPlayer;

    @Inject
    KeyboardController keyboardController;

    @Inject
    UserInputController userInputController;

    @Inject
    ImageViewController imageViewController;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "********UP********");

        initPreferencesLoader();

        initUIControls();

        initIOCModules();

        initMediaStreamHandlers();

        initNetworkObservers();

        initInteractionObservers();

        connectionHandler.establishConnection();
    }

    private void initPreferencesLoader() {
        preferencesLoader = new PreferencesLoader(this);
    }

    private void initUIControls() {
        setContentView(R.layout.activity_main);
         retryConnectionBtn = findViewById(R.id.retryConnectionBtn);

         retryConnectionBtn.setEnabled(false);

         retryConnectionBtn.setOnClickListener(e -> {
             retryConnectionBtn.setEnabled(false);
             connectionHandler.retryConnection();
         });
    }

    private void initInteractionObservers() {
        initScaleDetector();
        keyboardController.addKeyboardInputObserver(userInputController);
        imageViewController.addImageInteractionObserver(userInputController);
    }

    private void initIOCModules() {
        SecurityModule securityModule = new SecurityModule(this.getFilesDir(), preferencesLoader.getCAPublicKeyPath());

        ImageView imageView = createImageView();
        EditText keyboard = findViewById(R.id.keyboardInput);
        MediaModule mediaModule = new MediaModule(this, imageView, this, keyboard);

        String serverIpAddr = preferencesLoader.getIPAddr();
        int serverPort = preferencesLoader.getPort();
        int clientMediaPort = serverPort;

        NetworkingModule networkingModule = new NetworkingModule(serverIpAddr, serverPort, clientMediaPort);

        DaggerAppComponent
                .builder()
                .securityModule(securityModule)
                .mediaModule(mediaModule)
                .networkingModule(networkingModule)
                .build()
                .inject(this);
    }

    private void initMediaStreamHandlers() {
        mediaReceiver.addMediaStreamHandler(audioStreamHandler, false);
        mediaReceiver.addMediaStreamHandler(videoStreamHandler, true);
    }

    private void initNetworkObservers() {
        messageReceiver.addAuthStatusObserver(menuController);
        messageReceiver.addAuthStatusObserver(authController);
        messageReceiver.addAuthStatusObserver(keyboardController);
        messageReceiver.addAuthStatusObserver(userInputController);
        messageReceiver.addAuthStatusObserver(this);

        connectionHandler.addConnectionStatusObserver(mediaReceiver);
        connectionHandler.addConnectionStatusObserver(menuController);
        connectionHandler.addConnectionStatusObserver(sender);
        connectionHandler.addConnectionStatusObserver(authController);
        connectionHandler.addConnectionStatusObserver(keyboardController);
        connectionHandler.addConnectionStatusObserver(userInputController);
        connectionHandler.addConnectionStatusObserver(this);

        listener.addMsgRcvdObserver(messageReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        menu.findItem(R.id.keyboardBtn)
                .setOnMenuItemClickListener(c -> {
                    keyboardController.changeKeyboardVisibility(false);
                    return true;
                });


        menu.findItem(R.id.monitorBtn).setOnMenuItemClickListener(e -> {
            messageSender.changeMonitor();
            return true;
        });

        menu.findItem(R.id.WinBtn).setOnMenuItemClickListener(e -> {
           messageSender.sendKeyboardInput('\0', SpecialKeyCode.WINDOWS_KEY, null);
           return true;
        });

        menu.findItem(R.id.upBtn).setOnMenuItemClickListener(e -> {
            messageSender.sendScrollMessage(true);
            return true;
        });

        menu.findItem(R.id.downBtn).setOnMenuItemClickListener(e -> {
            messageSender.sendScrollMessage(false);
            return true;
        });

        menu.findItem(R.id.backspaceBtn).setOnMenuItemClickListener(e -> {
            messageSender.sendKeyboardInput('\0', SpecialKeyCode.BACKSPACE, null);
           return true;
        });

        menu.findItem(R.id.muteBtn).setOnMenuItemClickListener(e -> {
            boolean isMuted = soundPlayer.isMuted();
            menuController.changeMuteIcon(!isMuted);
            messageSender.sendMuteMessage(!isMuted);

            if (isMuted)
                audioStreamHandler.start();
            else
                audioStreamHandler.stop();

            return true;
        });


        menu.findItem(R.id.CtrlBtn)
                .setOnMenuItemClickListener(e -> modifierHandler(KeyboardModifier.CTRL_KEY));

        menu.findItem(R.id.AltBtn)
                .setOnMenuItemClickListener(e -> modifierHandler(KeyboardModifier.ALT_KEY));

        menu.findItem(R.id.ShiftBtn)
                .setOnMenuItemClickListener(e -> modifierHandler(KeyboardModifier.SHIFT_KEY));

        menu.findItem(R.id.rightMouseON)
                .setOnMenuItemClickListener(e -> {
                    menuController.setRightMouse(!menuController.getRightMouseStatus());
                    return true;
                });

        settingsController.setupSettings(menu, connectionHandler, preferencesLoader);

        menuController.setMenu(menu);
        menuController.changeMenuItemsVisibility(true);

        return super.onCreateOptionsMenu(menu);
    }


    private ImageView createImageView() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        RelativeLayout relativeLayout = findViewById(R.id.top_rel_layout);

        ImageView imageView = new ImageView(this);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                displayMetrics.widthPixels, (int)(displayMetrics.heightPixels * 0.53));
        params.leftMargin = 0;
        params.topMargin = 0;

        relativeLayout.addView(imageView, params);

        return imageView;
    }


    private boolean modifierHandler(KeyboardModifier modifier) {
        boolean isEnabled = keyboardController.isEnabled(modifier);
        keyboardController.setKeyboardModifier(modifier, !isEnabled);
        menuController.changeModifierLabel(modifier, !isEnabled);
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
                messageSender.rescaleImage(ratio);
            }
        };

        detector = new ScaleGestureDetector(this, listener);
    }


    @Override
    public void scheduleGuiTask(Runnable task) {
        this.runOnUiThread(task);
    }

    private void showToast(String msg, int toastLength) {
        Toast.makeText(this, msg, toastLength).show();
    }

    @Override
    public void failedToConnect(String errorMsg) {
        scheduleGuiTask(() -> {
            showToast("Failed To Connect.\n" + errorMsg, Toast.LENGTH_SHORT);
            retryConnectionBtn.setEnabled(true);
        });
    }

    @Override
    public void connectionEstablished(Socket socket) {
        scheduleGuiTask(() -> {
            retryConnectionBtn.setEnabled(false);
        });
    }

    @Override
    public void connectionLost(Socket socket) {
        scheduleGuiTask(() -> {
            findViewById(R.id.pilotLayout).setVisibility(View.GONE);
            showToast("Connection Lost.", Toast.LENGTH_LONG);;
        });
    }

    @Override
    public void authSucceeded() {
        scheduleGuiTask(() -> findViewById(R.id.pilotLayout).setVisibility(View.VISIBLE));
    }

    @Override
    public void authFailed() {
        // pass
    }
}