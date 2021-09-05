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
import com.example.pilot.utils.ScreenShot;


public class UIHandler extends Handler implements SsRcvdObserver {
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
        if (msg.what == UIMsgCode.UPDATE_IMAGE.ordinal()) {
            this.iv.updateImage((ScreenShot)msg.obj);
        }
        else {
            //TODO
            System.out.println("UNEXPECTED CODE!!!!!!!!!!!!!!!!");
        }
    }

    @Override
    public void onScreenShotRcvd(ScreenShot ss) {
        Message msg = new Message();
        msg.what = UIMsgCode.UPDATE_IMAGE.ordinal();
        msg.obj = ss;
        this.sendMessage(msg);
    }


}
