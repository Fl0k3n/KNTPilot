package com.example.pilot.ui.utils;

import android.content.Context;
import android.widget.ImageView;

import com.example.pilot.networking.tcp.MessageSender;
import com.example.pilot.ui.controller.MenuController;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ImageViewController extends ImageViewer{
    private final MessageSender messageSender;
    private final MenuController menuController;

    @Inject
    public ImageViewController(Context ctx, ImageView imageView, MessageSender messageSender, MenuController menuController) {
        super(ctx, imageView);
        this.messageSender = messageSender;
        this.menuController = menuController;
    }

    @Override
    public void onSwipe(float real_dx, float real_dy) {
        messageSender.sendSwipeMessage(real_dx, real_dy);
    }

    @Override
    public void onClick(float x, float y) {
        messageSender.sendClickMessage(x, y, menuController.getRightMouseStatus());
    }

    @Override
    public void onDoubleClick(float x, float y) {
        messageSender.sendDoubleClickMessage(x, y, menuController.getRightMouseStatus());
    }
}
