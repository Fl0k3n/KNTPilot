package com.example.pilot.gui;

import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pilot.R;
import com.example.pilot.networking.MessageHandler;
import com.example.pilot.networking.SsRcvdObserver;
import com.example.pilot.utils.ScreenShot;


public class UIHandler extends Handler implements SsRcvdObserver {
    private ImageViewer iv;
    private MessageHandler  messageHandler;
    private AppCompatActivity activity;

    public UIHandler (MessageHandler messageHandler, AppCompatActivity activity) {
        this.messageHandler = messageHandler;
        this.activity = activity;

        ImageView imageView = createImageView();
        initImageViewer(imageView);
        connectChangeMonitorBtn();
    }

    private ImageView createImageView() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        RelativeLayout relativeLayout = activity.findViewById(R.id.top_rel_layout);

        ImageView imageView = new ImageView(activity);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                displayMetrics.widthPixels, displayMetrics.heightPixels / 2);
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

    private void connectChangeMonitorBtn() {
        Button btn = activity.findViewById(R.id.change_monitor_btn);
        btn.setOnClickListener(e -> messageHandler.changeMonitor());
    }

    @Override
    public void handleMessage(Message msg) {
        if (msg.what == UIMsgCode.UPDATE_IMAGE.ordinal()) {
            this.iv.updateImage((ScreenShot)msg.obj);
        }
        else {
            System.out.println("UNEXPECTED CODE !!!!!!!!!!!!!!!! TODO");
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
