package com.example.pilot.gui;

import android.os.Handler;
import android.os.Message;

import com.example.pilot.utils.ScreenShot;


public class UIHandler extends Handler {
    private ImageViewer iv;
    public UIHandler (ImageViewer iv) {
        this.iv = iv;
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
}
