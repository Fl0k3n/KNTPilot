package com.example.pilot.ui.views;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pilot.R;
import com.example.pilot.ui.AuthHandler;
import com.example.pilot.ui.utils.FpsUpdater;
import com.example.pilot.ui.utils.KeyboardController;
import com.example.pilot.ui.utils.UIMsgCode;
import com.example.pilot.ui.utils.KeyboardInputHandler;
import com.example.pilot.utils.KeyboardModifier;
import com.example.pilot.networking.MessageHandler;
import com.example.pilot.networking.SpecialKeyCode;
import com.example.pilot.networking.observers.SsRcvdObserver;
import com.example.pilot.networking.observers.AuthStatusObserver;
import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.utils.ScreenShot;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;


public class MainUIHandler extends Handler implements
        SsRcvdObserver, ConnectionStatusObserver,
        AuthStatusObserver, FpsUpdater, KeyboardInputHandler {
    private ImageViewer iv;
    private final MessageHandler  messageHandler;
    private final AppCompatActivity activity;
    private final AuthHandler authHandler;
    private Menu menu;
    private MenuItem fpsBox = null;
    private final KeyboardController keyboardController;
    private final HashMap<KeyboardModifier, MenuItem> menuViews;
    private final HashMap<KeyboardModifier, String> constantNames;

    public MainUIHandler(MessageHandler messageHandler, AppCompatActivity activity) {
        this.messageHandler = messageHandler;
        this.activity = activity;
        this.menuViews = new HashMap<>();
        this.constantNames = new HashMap<>();

        authHandler = new AuthHandler(activity, messageHandler);

        EditText textInput = activity.findViewById(R.id.keyboardInput);
        keyboardController = new KeyboardController(this, textInput);


        ImageView imageView = createImageView();
        initImageViewer(imageView);
    }


    public void setMenu(Menu menu) {
        this.menu = menu;

        Integer[] modifierIds = {R.id.ShiftBtn, R.id.AltBtn, R.id.CtrlBtn};
        KeyboardModifier[] modifierKeys = {KeyboardModifier.SHIFT_KEY, KeyboardModifier.ALT_KEY, KeyboardModifier.CTRL_KEY};
        String[] names = {"Shift", "Alt", "Ctrl"};
        for (int i=0; i<modifierIds.length; i++) {
            menuViews.put(modifierKeys[i], menu.findItem(modifierIds[i]));
            constantNames.put(modifierKeys[i], names[i]);
        }
    }

    private ImageView createImageView() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);

        RelativeLayout relativeLayout = activity.findViewById(R.id.top_rel_layout);

        ImageView imageView = new ImageView(activity);
        imageView.setScaleType(ImageView.ScaleType.FIT_XY);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                displayMetrics.widthPixels, (int)(displayMetrics.heightPixels * 0.53));
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


    @Override
    public void handleMessage(Message msg) {
        // receives msges from itself but in different thread,
        // this code will be run in main thread and is able to touch gui
        UIMsgCode code = UIMsgCode.fromInteger(msg.what);

        switch(code) {
            case UPDATE_IMAGE:
                this.iv.updateImage((ScreenShot)msg.obj);
                break;
            case FAILED_TO_CONNECT:
                Toast.makeText(activity, "Failed To Connect. " + msg.obj, Toast.LENGTH_LONG).show();
                break;
            case CONNECTION_LOST:
                Toast.makeText(activity, "Connection Lost.", Toast.LENGTH_LONG).show();
                hidePilotView();
                authHandler.connectionLost();
                setDefaultMenuOptions();
                break;
            case CONNECTION_ESTB:
                authHandler.connectionEstablished();;
                break;
            case AUTH_STATUS:
                if ((Boolean)msg.obj) {
                    showPilotView();
                    authHandler.authSucceeded();
                }
                else
                    authHandler.authFailed();
                // failed auth is handled is AuthHandler
                break;
            case UPDATE_FPS:
                if (fpsBox != null)
                    fpsBox.setTitle("FPS: " + msg.obj);
                else if(menu != null)
                    try {
                        fpsBox = menu.findItem(R.id.fpsValue);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                break;
            default:
                throw new RuntimeException("Got unexpected code " + code);
        }

    }

    @Override
    public void onScreenShotRcvd(ScreenShot ss) {
        sendThreadMessage(UIMsgCode.UPDATE_IMAGE, ss);
        this.messageHandler.sendSSRcvdMessage();
    }


    @Override
    public void failedToConnect(String errorMsg) {
        sendThreadMessage(UIMsgCode.FAILED_TO_CONNECT, errorMsg);
    }

    @Override
    public void connectionEstablished() {
        sendThreadMessage(UIMsgCode.CONNECTION_ESTB, null);
    }

    @Override
    public void connectionLost() {
        sendThreadMessage(UIMsgCode.CONNECTION_LOST, null);
    }


    private void sendThreadMessage(UIMsgCode code, Object val) {
        Message msg = new Message();
        msg.what = code.ordinal();
        msg.obj = val;
        this.sendMessage(msg);
    }

    @Override
    public void authSucceeded() {
        sendThreadMessage(UIMsgCode.AUTH_STATUS, true);
    }


    @Override
    public void authFailed() {
        sendThreadMessage(UIMsgCode.AUTH_STATUS, false);
    }

    private void showPilotView() {
        activity.findViewById(R.id.pilotLayout).setVisibility(View.VISIBLE);
        this.menu.findItem(R.id.settingsBtn).setVisible(false);
        changeKeyboardVisibility(false);
        changeMenuItemsVisibility(false);
//        activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private void hidePilotView() {
        activity.findViewById(R.id.pilotLayout).setVisibility(View.GONE);
        this.menu.findItem(R.id.settingsBtn).setVisible(true);
        changeKeyboardVisibility(true);
        changeMenuItemsVisibility(true);
    }

    public void changeKeyboardVisibility(boolean hidden) {
        EditText keyboard = activity.findViewById(R.id.keyboardInput);
        // TODO only showing works
        if (hidden)
            keyboard.clearFocus();
        else
            keyboard.requestFocus();

        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(keyboard, hidden ? InputMethodManager.HIDE_IMPLICIT_ONLY : InputMethodManager.SHOW_IMPLICIT);
    }

    public void changeMenuItemsVisibility(boolean hidden) {
        int[] items = {
                R.id.monitorBtn, R.id.keyboardBtn, R.id.WinBtn, R.id.upBtn, R.id.downBtn,
                R.id.fpsValue, R.id.backspaceBtn, R.id.CtrlBtn, R.id.AltBtn, R.id.ShiftBtn,
                R.id.muteBtn
        };
        Arrays.stream(items).forEach(id -> menu.findItem(id).setVisible(!hidden));
    }

    @Override
    public void updateFPS(int fps) {
        sendThreadMessage(UIMsgCode.UPDATE_FPS, fps);
    }

    @Override
    public void onKeyPressed(char key, SpecialKeyCode code, List<KeyboardModifier> modifiers) {
        messageHandler.sendKeyboardInput(key, code, modifiers);
    }

    public void changeKeyboardModifier(KeyboardModifier modifier) {
        boolean isEnabled = keyboardController.isEnabled(modifier);
        keyboardController.setKeyboardModifier(modifier, !isEnabled);
        MenuItem item = menuViews.get(modifier);
        item.setTitle(constantNames.get(modifier) + (isEnabled ? " ON" : " OFF"));
    }

    public void changeMuteIcon(boolean isMuted) {
        this.menu.findItem(R.id.muteBtn).setIcon(isMuted ?
                R.drawable.ic_muted : R.drawable.ic_unmuted);
    }

    private void setDefaultMenuOptions() {
        changeMuteIcon(true);
        for (KeyboardModifier modifier :
                KeyboardModifier.values()) {
            if (keyboardController.isEnabled(modifier))
                changeKeyboardModifier(modifier);
        }
    }
}
