package com.example.pilot.ui.events;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public abstract class ImageSwipeListener implements View.OnTouchListener {
    private final GestureDetector gestureDetector;

    public ImageSwipeListener(Context ctx) {
        gestureDetector = new GestureDetector(ctx, new GestureListener());
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int SWIPE_THRESHOLD = 100;
        private static final int SWIPE_VELOCITY_THRESHOLD = 100;

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if ((Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) ||
                    (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD)) {
                onSwipe(e1.getX(), e1.getY(), e2.getX(), e2.getY());
                result = true;
            }

            return result;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent event) {
            System.out.printf("tapped XY = (%f, %f)\n", event.getX(), event.getY());
            onClick(event.getX(), event.getY());
            return true;
        }

        // TODO
//        @Override
//        public boolean onDoubleTap(MotionEvent event) {
//            return true;
//        }
    }

    public abstract void onSwipe(float x0, float y0, float x1, float y1);
    public abstract void onClick(float x, float y);
}