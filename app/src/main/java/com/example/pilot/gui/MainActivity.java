package com.example.pilot.gui;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Message;
import android.util.DisplayMetrics;
import android.widget.Button;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("Hello world");


        networkHandler = new NetworkHandler();
        messageHandler = new MessageHandler(networkHandler);

        uiHandler = new UIHandler(messageHandler, this);

        messageHandler.addSSRcvdObserver(uiHandler);

        networkHandler.addMsgRcvdObserver(messageHandler);

        Thread network = new Thread(networkHandler);
        network.start();
    }



}