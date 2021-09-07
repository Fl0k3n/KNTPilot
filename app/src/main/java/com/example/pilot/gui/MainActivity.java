package com.example.pilot.gui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.example.pilot.R;
import com.example.pilot.networking.KeyboardModifier;
import com.example.pilot.networking.MessageHandler;
import com.example.pilot.networking.NetworkHandler;
import com.example.pilot.networking.SpecialKeyCode;


public class MainActivity extends AppCompatActivity {
    private UIHandler uiHandler;
    private NetworkHandler networkHandler;
    private MessageHandler messageHandler;
    private ScaleGestureDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        networkHandler = new NetworkHandler();
        messageHandler = new MessageHandler(networkHandler);
        uiHandler = new UIHandler(messageHandler, this);

        messageHandler.addAuthStatusObserver(uiHandler);
        messageHandler.addSSRcvdObserver(uiHandler);

        networkHandler.addMsgRcvdObserver(messageHandler);
        networkHandler.addConnectionStatusObserver(uiHandler);

        initScaleDetector();

        Thread network = new Thread(networkHandler);
        network.start();

        FPSCounter fpsCounter = new FPSCounter(uiHandler);
        messageHandler.addSSRcvdObserver(fpsCounter);

        Thread timer = new Thread(fpsCounter);
        timer.start();
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


        menu.findItem(R.id.CtrlBtn)
                .setOnMenuItemClickListener(e -> modifierHandler(KeyboardModifier.CTRL_KEY));

        menu.findItem(R.id.AltBtn)
                .setOnMenuItemClickListener(e -> modifierHandler(KeyboardModifier.ALT_KEY));

        menu.findItem(R.id.ShiftBtn)
                .setOnMenuItemClickListener(e -> modifierHandler(KeyboardModifier.SHIFT_KEY));

        uiHandler.setMenu(menu);
        uiHandler.changeMenuItemsVisibility(true);

        return super.onCreateOptionsMenu(menu);
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