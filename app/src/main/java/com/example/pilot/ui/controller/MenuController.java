package com.example.pilot.ui.controller;

import android.view.Menu;
import android.view.MenuItem;

import com.example.pilot.R;
import com.example.pilot.ui.utils.FpsUpdater;
import com.example.pilot.ui.utils.GuiRunner;
import com.example.pilot.utils.KeyboardModifier;
import com.example.pilot.networking.observers.AuthStatusObserver;
import com.example.pilot.networking.observers.ConnectionStatusObserver;

import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class MenuController implements ConnectionStatusObserver, AuthStatusObserver {
    private Menu menu;
    private final HashMap<KeyboardModifier, MenuItem> menuViews;
    private final HashMap<KeyboardModifier, String> constantNames;
    private final FpsUpdater fpsUpdater;
    private final GuiRunner guiRunner;
    private boolean rightMouseOn;

    @Inject
    public MenuController(FpsUpdater fpsUpdater, GuiRunner guiRunner)
    {
        this.guiRunner = guiRunner;
        this.menuViews = new HashMap<>();
        this.constantNames = new HashMap<>();
        this.rightMouseOn = false;

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

    @Override
    public void failedToConnect(String errorMsg) {
        // pass
    }

    @Override
    public void connectionEstablished(Socket socket) {
        // pass
    }

    @Override
    public void connectionLost(Socket socket) {
        guiRunner.scheduleGuiTask(() -> {
            this.menu.findItem(R.id.settingsBtn).setVisible(true);
            changeMenuItemsVisibility(true);
            fpsUpdater.stop();
            setDefaultMenuOptions();
        });
    }


    @Override
    public void authSucceeded() {
        guiRunner.scheduleGuiTask(() -> {
            this.menu.findItem(R.id.settingsBtn).setVisible(false);
            changeMenuItemsVisibility(false);
            fpsUpdater.start(menu.findItem(R.id.fpsValue));
        });
    }


    @Override
    public void authFailed() {
        // pass
    }


    public void changeMenuItemsVisibility(boolean hidden) {
        int[] items = {
                R.id.monitorBtn, R.id.keyboardBtn, R.id.WinBtn, R.id.upBtn, R.id.downBtn,
                R.id.fpsValue, R.id.backspaceBtn, R.id.CtrlBtn, R.id.AltBtn, R.id.ShiftBtn,
                R.id.muteBtn
        };
        Arrays.stream(items).forEach(id -> menu.findItem(id).setVisible(!hidden));
        menu.findItem(R.id.rightMouseON).setVisible(!hidden);
    }

    public void changeModifierLabel(KeyboardModifier modifier, boolean isEnabled) {
        MenuItem item = menuViews.get(modifier);
        item.setTitle(constantNames.get(modifier) + (isEnabled ? " OFF" : " ON"));
    }


    public void changeMuteIcon(boolean isMuted) {
        this.menu.findItem(R.id.muteBtn).setIcon(isMuted ?
                R.drawable.ic_muted : R.drawable.ic_unmuted);
    }

    private void setDefaultMenuOptions() {
        changeMuteIcon(true);
        setRightMouse(false);

        for (KeyboardModifier modifier :
                KeyboardModifier.values()) {
            changeModifierLabel(modifier, false);
        }
    }

    public void setRightMouse(boolean on) {
        rightMouseOn = on;
        menu.findItem(R.id.rightMouseON).setTitle("Right Mouse " + (on ? " OFF" : " ON"));
    }

    public boolean getRightMouseStatus() {
        return rightMouseOn;
    }
}
