package com.example.pilot.gui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.example.pilot.R;
import com.example.pilot.networking.MessageHandler;
import com.example.pilot.networking.NetworkHandler;
import com.example.pilot.networking.SsRcvdObserver;
import com.example.pilot.utils.ScreenShot;


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

        messageHandler.addSSRcvdObserver(uiHandler);

        networkHandler.addMsgRcvdObserver(messageHandler);

        initScaleDetector();

        Thread network = new Thread(networkHandler);
        network.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        menu.findItem(R.id.showKeyboard)
                .setOnMenuItemClickListener(c -> {
                    EditText keyboard = findViewById(R.id.keyboardInput);
                    keyboard.requestFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    imm.showSoftInput(keyboard, InputMethodManager.SHOW_IMPLICIT);
                    return true;
                });


        menu.findItem(R.id.change_monitor_btn).setOnMenuItemClickListener(e -> {
            messageHandler.changeMonitor();
            return true;
        });

        return super.onCreateOptionsMenu(menu);
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