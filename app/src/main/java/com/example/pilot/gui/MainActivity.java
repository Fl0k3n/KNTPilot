package com.example.pilot.gui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Message;

import com.example.pilot.R;
import com.example.pilot.networking.MessageHandler;
import com.example.pilot.networking.NetworkHandler;
import com.example.pilot.networking.SsRcvdObserver;
import com.example.pilot.utils.ScreenShot;


public class MainActivity extends AppCompatActivity implements SsRcvdObserver {
    private ImageViewer iv;
    private UIHandler uiHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("Hello world");

        NetworkHandler nh = new NetworkHandler();
        MessageHandler mh = new MessageHandler(nh);

        iv = new ImageViewer(this, findViewById(R.id.sshotView)) {
            @Override
            public void onSwipe(float x0, float y0, float x1, float y1) {
                mh.sendSwipeMessage(x0, y0, x1, y1);
            }
        };

        uiHandler = new UIHandler(iv);

        mh.addSSRcvdObserver(this);
        nh.addMsgRcvdObserver(mh);
        Thread network = new Thread(nh);

        network.start();
    }

    @Override
    public void onScreenShotRcvd(ScreenShot ss) {
        Message msg = new Message();
        msg.what = UIMsgCode.UPDATE_IMAGE.ordinal();
        msg.obj = ss;
        uiHandler.sendMessage(msg);
    }

}