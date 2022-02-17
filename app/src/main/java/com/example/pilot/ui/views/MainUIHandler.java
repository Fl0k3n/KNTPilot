package com.example.pilot.ui.views;

import android.content.Context;
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
import com.example.pilot.networking.udp.MediaStreamHandler;
import com.example.pilot.ui.utils.FpsUpdater;
import com.example.pilot.ui.utils.GuiRunner;
import com.example.pilot.ui.utils.KeyboardController;
import com.example.pilot.ui.utils.KeyboardInputHandler;
import com.example.pilot.utils.KeyboardModifier;
import com.example.pilot.networking.tcp.MessageSender;
import com.example.pilot.utils.SpecialKeyCode;
import com.example.pilot.networking.observers.AuthStatusObserver;
import com.example.pilot.networking.observers.ConnectionStatusObserver;

import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;


@Singleton
public class MainUIHandler implements ConnectionStatusObserver, AuthStatusObserver, KeyboardInputHandler {
    private final MessageSender messageSender;
    private final AppCompatActivity activity;
    private final AuthHandler authHandler;
    private Menu menu;
    private final KeyboardController keyboardController;
    private final HashMap<KeyboardModifier, MenuItem> menuViews;
    private final HashMap<KeyboardModifier, String> constantNames;
    private final FpsUpdater fpsUpdater;
    private final GuiRunner guiRunner;
    private final MediaStreamHandler videoStreamHandler;
    private final MediaStreamHandler audioStreamHandler;


    @Inject
    public MainUIHandler(MessageSender messageSender, FpsUpdater fpsUpdater,
                         @Named("main activity") AppCompatActivity activity,
                         GuiRunner guiRunner,
                         @Named("video stream handler") MediaStreamHandler videoStreamHandler,
                         @Named("audio stream handler") MediaStreamHandler audioStreamHandler,
                         AuthHandler authHandler)
    {
        this.messageSender = messageSender;
        this.activity = activity;
        this.guiRunner = guiRunner;
        this.videoStreamHandler = videoStreamHandler;
        this.audioStreamHandler = audioStreamHandler;
        this.authHandler = authHandler;
        this.menuViews = new HashMap<>();
        this.constantNames = new HashMap<>();

        EditText textInput = activity.findViewById(R.id.keyboardInput);
        keyboardController = new KeyboardController(this, textInput);

        this.fpsUpdater = fpsUpdater;
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


    private void showToast(String msg, int toastLength) {
        Toast.makeText(activity, msg, toastLength).show();
    }

    @Override
    public void failedToConnect(String errorMsg) {
        guiRunner.scheduleGuiTask(() -> showToast("Failed To Connect.\n" + errorMsg, Toast.LENGTH_SHORT));
    }

    @Override
    public void connectionEstablished(Socket socket) {
        guiRunner.scheduleGuiTask(authHandler::connectionEstablished);
    }

    @Override
    public void connectionLost(Socket socket) {
        videoStreamHandler.stop();
        audioStreamHandler.stop();

        guiRunner.scheduleGuiTask(() -> {
            showToast("Connection Lost.", Toast.LENGTH_LONG);
            hidePilotView();
            fpsUpdater.stop();
            authHandler.connectionLost();
            setDefaultMenuOptions();
        });
    }


    @Override
    public void authSucceeded() {
        videoStreamHandler.start();

        guiRunner.scheduleGuiTask(() -> {
            showPilotView();
            fpsUpdater.start(menu.findItem(R.id.fpsValue));
            authHandler.authSucceeded();
        });
    }


    @Override
    public void authFailed() {
        guiRunner.scheduleGuiTask(authHandler::authFailed);
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
    public void onKeyPressed(char key, SpecialKeyCode code, List<KeyboardModifier> modifiers) {
        messageSender.sendKeyboardInput(key, code, modifiers);
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
