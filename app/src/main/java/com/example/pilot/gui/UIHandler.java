package com.example.pilot.gui;

import android.content.Context;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pilot.R;
import com.example.pilot.networking.MessageHandler;
import com.example.pilot.networking.SpecialKeyCode;
import com.example.pilot.networking.SsRcvdObserver;
import com.example.pilot.utils.ConnectionStatusObserver;
import com.example.pilot.utils.ScreenShot;


public class UIHandler extends Handler implements SsRcvdObserver, ConnectionStatusObserver {
    private ImageViewer iv;
    private MessageHandler  messageHandler;
    private AppCompatActivity activity;
    private EditText textInput;
    private int textStart;

    public UIHandler (MessageHandler messageHandler, AppCompatActivity activity) {
        this.messageHandler = messageHandler;
        this.activity = activity;
        this.textStart = 0;

        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        textInput = activity.findViewById(R.id.keyboardInput);

        textInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                System.out.printf("DATA: %d %d %d\n", start, before, count);

                if (textStart + before > start + count) {
                    messageHandler.sendKeyboardInput('\0', SpecialKeyCode.BACKSPACE);
                    // TODO
                }
                else {
                    char pressed = s.charAt(start + count - 1);
//                    System.out.println("PRESSED -> " + pressed);
                    messageHandler.sendKeyboardInput(pressed, SpecialKeyCode.NONE);

                }

                if (s.charAt(start + count - 1) == '\n') {
                    textStart = start;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        ImageView imageView = createImageView();
        initImageViewer(imageView);
    }

    private ImageView createImageView() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        RelativeLayout relativeLayout = activity.findViewById(R.id.top_rel_layout);

        ImageView imageView = new ImageView(activity);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                displayMetrics.widthPixels, (int)(displayMetrics.heightPixels * 0.53));
        params.leftMargin = 0;
        params.topMargin = 0;

        relativeLayout.addView(imageView, params);

        return imageView;
    }

    private void initImageViewer(ImageView imageView) {
        iv = new ImageViewer(activity, imageView) {
            @Override
            public void onSwipe(float real_dx, float real_dy) {
                messageHandler.sendSwipeMessage(real_dx, real_dy);
            }

            @Override
            public void onClick(float x, float y) {
                messageHandler.sendClickMessage(x, y);
            }
        };
    }


    @Override
    public void handleMessage(Message msg) {
        // receives msges from itself but in different thread,
        // this code will be run in main thread and is able to touch gui
        UIMsgCode code = UIMsgCode.fromInteger(msg.what);

        switch(code) {
            case UPDATE_IMAGE:
                this.iv.updateImage((ScreenShot)msg.obj);
                break;
            case FAILED_TO_CONNECT:
                Toast.makeText(activity, "Failed To Connect. " + msg.obj, Toast.LENGTH_LONG).show();
                break;
            case CONNECTION_LOST:
                Toast.makeText(activity, "Connection Lost.", Toast.LENGTH_LONG).show();
                break;
            default:
                throw new RuntimeException("Got unexpected code " + code);
        }

    }

    @Override
    public void onScreenShotRcvd(ScreenShot ss) {
        sendThreadMessage(UIMsgCode.UPDATE_IMAGE, ss);
    }


    @Override
    public void failedToConnect(String errorMsg) {
        sendThreadMessage(UIMsgCode.FAILED_TO_CONNECT, errorMsg);
    }

    @Override
    public void connectionLost() {
        sendThreadMessage(UIMsgCode.CONNECTION_LOST, null);
    }


    private void sendThreadMessage(UIMsgCode code, Object val) {
        Message msg = new Message();
        msg.what = code.ordinal();
        msg.obj = val;
        this.sendMessage(msg);
    }
}
