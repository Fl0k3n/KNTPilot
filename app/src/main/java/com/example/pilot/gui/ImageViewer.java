package com.example.pilot.gui;

import android.content.Context;
import android.widget.ImageView;

import com.example.pilot.utils.ScreenShot;

public abstract class ImageViewer {
    private ImageView view;

    public ImageViewer(Context ctx, ImageView imageView) {
        this.view = imageView;
        ImageViewer self = this;
        this.view.setOnTouchListener(new ImageSwipeListener(ctx) {

            @Override
            public void onSwipe(float x0, float y0, float x1, float y1) {
                self.onSwipe(x0, y0, x1, y1);
            }
        });
    }

    public void updateImage(ScreenShot ss) {
        this.view.setImageBitmap(ss.toBitmap());
    }

    public abstract void onSwipe(float x0, float y0, float x1, float y1);
}
