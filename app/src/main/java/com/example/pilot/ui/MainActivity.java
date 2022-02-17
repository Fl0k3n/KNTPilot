package com.example.pilot.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Application;
import android.content.Context;
import android.media.Image;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.pilot.IOC.DaggerAppComponent;
import com.example.pilot.IOC.MediaModule;
import com.example.pilot.IOC.NetworkingModule;
import com.example.pilot.IOC.SecurityModule;
import com.example.pilot.R;
import com.example.pilot.networking.tcp.Listener;
import com.example.pilot.networking.udp.MediaReceiver;
import com.example.pilot.networking.tcp.MessageReceiver;
import com.example.pilot.networking.tcp.Sender;
import com.example.pilot.ui.events.ImageScaleListener;
import com.example.pilot.networking.udp.MediaStreamHandler;
import com.example.pilot.ui.utils.GuiRunner;
import com.example.pilot.ui.utils.SoundPlayer;
import com.example.pilot.ui.views.MainUIHandler;
import com.example.pilot.ui.views.SettingsHandler;
import com.example.pilot.utils.KeyboardModifier;
import com.example.pilot.networking.tcp.MessageSender;
import com.example.pilot.networking.tcp.ConnectionHandler;
import com.example.pilot.utils.SpecialKeyCode;
import com.example.pilot.utils.commands.ChangeSettingsVisibilityCommand;
import com.example.pilot.utils.PreferencesLoader;
import com.example.pilot.utils.commands.UpdateSettingsCommand;

import javax.inject.Inject;
import javax.inject.Named;

public class MainActivity extends AppCompatActivity implements GuiRunner {
    private ScaleGestureDetector detector;
    private PreferencesLoader preferencesLoader;

    @Inject
    MainUIHandler uiHandler;

    @Inject
    ConnectionHandler connectionHandler;

    @Inject @Named("video stream handler")
    MediaStreamHandler videoStreamHandler;

    @Inject @Named("audio stream handler")
    MediaStreamHandler audioStreamHandler;

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
    SettingsHandler settingsHandler;

    @Inject
    SoundPlayer soundPlayer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        System.out.println("********UP********");

        preferencesLoader = new PreferencesLoader(this);

        setContentView(R.layout.activity_main);

        initIOCModules();

        initMediaStreamHandlers();

        initNetworkObservers();

        initScaleDetector();

        connectionHandler.establishConnection();
    }

    private void initIOCModules() {
        SecurityModule securityModule = new SecurityModule(this.getFilesDir(), preferencesLoader.getCAPublicKeyPath());

        ImageView imageView = createImageView();

        MediaModule mediaModule = new MediaModule(this, imageView, this);

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
        messageReceiver.addAuthStatusObserver(uiHandler);

        connectionHandler.addConnectionStatusObserver(mediaReceiver);
        connectionHandler.addConnectionStatusObserver(uiHandler);
        connectionHandler.addConnectionStatusObserver(sender);

        listener.addMsgRcvdObserver(messageReceiver);
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
            uiHandler.changeMuteIcon(!isMuted);
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

        settingsHandler.setupSettings(menu, connectionHandler, preferencesLoader);

        uiHandler.setMenu(menu);
        uiHandler.changeMenuItemsVisibility(true);

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
                messageSender.rescaleImage(ratio);
            }
        };

        detector = new ScaleGestureDetector(this, listener);
    }

    @Override
    public void scheduleGuiTask(Runnable task) {
        this.runOnUiThread(task);
    }
}