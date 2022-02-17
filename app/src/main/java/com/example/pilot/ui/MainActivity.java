package com.example.pilot.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.pilot.R;
import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.networking.tcp.Listener;
import com.example.pilot.networking.udp.MediaReceiver;
import com.example.pilot.networking.tcp.MessageReceiver;
import com.example.pilot.networking.tcp.Sender;
import com.example.pilot.security.UDPGuard;
import com.example.pilot.security.certificate.CertificateVerifier;
import com.example.pilot.security.MessageSecurityPreprocessor;
import com.example.pilot.security.TCPGuard;
import com.example.pilot.security.TLSHandler;
import com.example.pilot.ui.events.ImageScaleListener;
import com.example.pilot.networking.udp.MediaStreamHandler;
import com.example.pilot.ui.utils.FPSCounter;
import com.example.pilot.ui.utils.FpsUpdater;
import com.example.pilot.ui.utils.GuiRunner;
import com.example.pilot.ui.utils.SoundPlayer;
import com.example.pilot.ui.utils.VideoPlayer;
import com.example.pilot.ui.views.ImageViewer;
import com.example.pilot.ui.views.MainUIHandler;
import com.example.pilot.ui.views.SettingsHandler;
import com.example.pilot.utils.KeyboardModifier;
import com.example.pilot.networking.tcp.MessageSender;
import com.example.pilot.networking.tcp.ConnectionHandler;
import com.example.pilot.utils.SpecialKeyCode;
import com.example.pilot.utils.commands.ChangeSettingsVisibilityCommand;
import com.example.pilot.utils.PreferencesLoader;
import com.example.pilot.utils.commands.UpdateSettingsCommand;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity implements GuiRunner {
    private MainUIHandler uiHandler;
    private ConnectionHandler connectionHandler;
    private MessageSender messageSender;
    private ScaleGestureDetector detector;
    private SoundPlayer soundPlayer;
    private PreferencesLoader preferencesLoader;
    private SettingsHandler settingsHandler;
    private MediaStreamHandler audioStreamHandler;
    private MediaReceiver mediaReceiver;
    private MessageReceiver messageReceiver;
    private Sender sender;
    private Listener listener;
    private VideoPlayer videoPlayer;
    private MediaStreamHandler videoStreamHandler;
    private FPSCounter fpsCounter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("UP*****************************************");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        preferencesLoader = new PreferencesLoader(this);

        saveCAPublicKey(this, preferencesLoader.getCAPublicKeyPath());

        File CAKeyFile = new File(this.getFilesDir(), preferencesLoader.getCAPublicKeyPath());

        TCPGuard tcpGuard = null;
        UDPGuard udpGuard = null;
        try {
            tcpGuard = new TCPGuard();
            udpGuard = new UDPGuard();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return;
        }
        CertificateVerifier certificateVerifier = new CertificateVerifier(CAKeyFile);
        TLSHandler tlsHandler = new TLSHandler(certificateVerifier, tcpGuard);

        MessageSecurityPreprocessor tcpPreprocessor = new MessageSecurityPreprocessor(tcpGuard);
        MessageSecurityPreprocessor udpPreprocessor = new MessageSecurityPreprocessor(udpGuard);

        fpsCounter = new FPSCounter(30);
        FpsUpdater fpsUpdater = new FpsUpdater(this, fpsCounter, 250, TimeUnit.MILLISECONDS);

        mediaReceiver = new MediaReceiver(preferencesLoader.getPort(), udpPreprocessor, Executors.newSingleThreadExecutor());


        sender = new Sender(tcpPreprocessor);
        listener = new Listener(tcpPreprocessor);
        connectionHandler = new ConnectionHandler(preferencesLoader.getIPAddr(), preferencesLoader.getPort(), tlsHandler,
                listener, Executors.newSingleThreadExecutor());
        messageSender = new MessageSender(sender);
        settingsHandler = new SettingsHandler(this);

        messageReceiver = new MessageReceiver(mediaReceiver, messageSender);

        ImageViewer imageViewer = createImageViewer();

        soundPlayer = new SoundPlayer();
        videoPlayer = new VideoPlayer(this, imageViewer, fpsCounter, 30);

        // TODO args from somewhere

        audioStreamHandler = new MediaStreamHandler(soundPlayer, 256);
        videoStreamHandler = new MediaStreamHandler(videoPlayer, 512);

        uiHandler = new MainUIHandler(messageSender, fpsUpdater, this, this, videoStreamHandler, audioStreamHandler);

        mediaReceiver.addMediaStreamHandler(audioStreamHandler, false);
        mediaReceiver.addMediaStreamHandler(videoStreamHandler, true);

        messageReceiver.addAuthStatusObserver(uiHandler);

        connectionHandler.addConnectionStatusObserver(mediaReceiver);
        connectionHandler.addConnectionStatusObserver(uiHandler);
        connectionHandler.addConnectionStatusObserver(sender);

        listener.addMsgRcvdObserver(messageReceiver);

        initScaleDetector();

        Thread network = new Thread(connectionHandler);
        network.start();
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
                new UpdateSettingsCommand(connectionHandler, preferencesLoader, settingsHandler);

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

    private ImageViewer createImageViewer() {
        return new ImageViewer(this, createImageView()) {
            @Override
            public void onSwipe(float real_dx, float real_dy) {
                messageSender.sendSwipeMessage(real_dx, real_dy);
            }

            @Override
            public void onClick(float x, float y) {
                messageSender.sendClickMessage(x, y);
            }
        };
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

    @Override
    public void scheduleGuiTask(Runnable task) {
        this.runOnUiThread(task);
    }
}