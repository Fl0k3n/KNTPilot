package com.example.pilot.ui.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import com.example.pilot.ui.events.ImageSwipeListener;
import com.example.pilot.utils.ScreenShot;

public abstract class ImageViewer {
    private final ImageView view;
    private Integer real_img_w, real_img_h;

    public ImageViewer(Context ctx, ImageView imageView) {
        this.view = imageView;
        ImageViewer self = this;
        real_img_w = real_img_h = null;

        this.view.setOnTouchListener(new ImageSwipeListener(ctx) {

            @Override
            public void onSwipe(float x0, float y0, float x1, float y1) {
                float dx = x1 - x0, dy = y1 - y0;
                float real_dx = toRealX(dx), real_dy = toRealY(dy);
                self.onSwipe(real_dx, real_dy);
            }

            @Override
            public void onClick(float x, float y) {
                float realX = toRealX(x), realY = toRealY(y);
                self.onClick(realX, realY);
            }
        });

    }

    private float toRealX(float viewX) {
        return real_img_w * viewX / view.getWidth();
    }

    private float toRealY(float viewY) {
        return real_img_h * viewY / view.getHeight();
    }

    public void updateImage(ScreenShot ss) {
        this.view.setImageBitmap(ss.toBitmap());
        Drawable d = this.view.getDrawable();
        this.real_img_w = d.getIntrinsicWidth();
        this.real_img_h = d.getIntrinsicHeight();
    }

    // relative deltas, e.g. if real img size is i_x/i_y, rescaled size is r_x/r_y
    // and if detected delta is dx/dy then rel_dx = i_x * dx / r_x; similarly y
    public abstract void onSwipe(float real_dx, float real_dy);

    public abstract void onClick(float x, float y);
}
