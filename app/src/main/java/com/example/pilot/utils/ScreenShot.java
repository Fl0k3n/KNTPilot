package com.example.pilot.utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class ScreenShot {
    private byte[] bytes;

    public ScreenShot(byte[] bytes) {
        this.bytes = bytes;
    }

    public Bitmap toBitmap() {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
