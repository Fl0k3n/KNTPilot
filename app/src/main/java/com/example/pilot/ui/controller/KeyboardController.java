package com.example.pilot.ui.controller;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pilot.networking.observers.AuthStatusObserver;
import com.example.pilot.networking.observers.ConnectionStatusObserver;
import com.example.pilot.ui.utils.GuiRunner;
import com.example.pilot.ui.utils.KeyboardInputObserver;
import com.example.pilot.utils.KeyboardModifier;
import com.example.pilot.utils.SpecialKeyCode;

import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class KeyboardController implements ConnectionStatusObserver, AuthStatusObserver {
    private final HashMap<KeyboardModifier, Boolean> modes;
    private final EditText keyboard;
    private final LinkedList<KeyboardInputObserver> inputObservers;
    private int textStart;
    private final AppCompatActivity activity;
    private final GuiRunner guiRunner;

    @Inject
    public KeyboardController(GuiRunner guiRunner,
                              @Named("keyboard") EditText keyboard,
                              @Named("main activity") AppCompatActivity activity)
    {
        this.guiRunner = guiRunner;
        this.keyboard = keyboard;
        this.activity = activity;
        this.inputObservers = new LinkedList<>();
        this.textStart = 0;

        modes = new HashMap<>();

        setupKeyboardModifiers();
        setupKeyboardInputListener();
    }

    private void setupKeyboardModifiers() {
        KeyboardModifier[] codes = {KeyboardModifier.ALT_KEY,
                KeyboardModifier.CTRL_KEY, KeyboardModifier.SHIFT_KEY};
        Arrays.stream(codes).forEach(code -> modes.put(code, false));
    }

    public void addKeyboardInputObserver(KeyboardInputObserver observer) {
        this.inputObservers.add(observer);
    }


    private void setupKeyboardInputListener() {
        keyboard.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // suggestion triggered change (?)
                if (before == count)
                    return;

                if(before > count || textStart > start) {
                    notifyKeyPressed('\0', SpecialKeyCode.BACKSPACE, null);
                }
                else {
                    char pressed = s.charAt(start + count - 1);
                    notifyKeyPressed(pressed, SpecialKeyCode.NONE, getEnabledKeys());

                }

                if (count == 0) {
                    textStart = 0;
                }
                else if (count > 0 && s.charAt(start + count - 1) == '\n') {
                    textStart = start + count;
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }


    public void changeKeyboardVisibility(boolean hidden) {
        // TODO only showing works
        if (hidden)
            keyboard.clearFocus();
        else
            keyboard.requestFocus();

        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(keyboard, hidden ? InputMethodManager.HIDE_IMPLICIT_ONLY : InputMethodManager.SHOW_IMPLICIT);
    }

    private void notifyKeyPressed(char key, SpecialKeyCode code, List<KeyboardModifier> modifiers) {
        inputObservers.forEach(keyboardInputObserver -> keyboardInputObserver.onKeyPressed(key, code, modifiers));
    }

    public void setKeyboardModifier(KeyboardModifier key, boolean enabled) {
        modes.put(key, enabled);
    }

    public List<KeyboardModifier> getEnabledKeys() {
        return modes.entrySet().stream()
                .filter(Map.Entry::getValue) //only enabled
                .map(Map.Entry::getKey) //get codes
                .collect(Collectors.toList());
    }

    public boolean isEnabled(KeyboardModifier modifier) {
        return modes.get(modifier);
    }

    private void disableAll() {
        for (KeyboardModifier keyboardModifier : KeyboardModifier.values()) {
            setKeyboardModifier(keyboardModifier, false);
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
            changeKeyboardVisibility(true);
            disableAll();
        });
    }

    @Override
    public void authSucceeded() {
        guiRunner.scheduleGuiTask(() -> changeKeyboardVisibility(false));
    }

    @Override
    public void authFailed() {
        // pass
    }
}
