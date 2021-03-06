package com.example.pilot.ui.events;

import android.view.ScaleGestureDetector;

public abstract class ImageScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    @Override
    public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
        scaled(scaleGestureDetector.getScaleFactor());
        return true;
    }

    public abstract void scaled(float ratio);
}
