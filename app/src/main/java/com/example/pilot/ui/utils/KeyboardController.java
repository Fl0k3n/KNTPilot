package com.example.pilot.ui.utils;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.example.pilot.utils.KeyboardModifier;
import com.example.pilot.utils.SpecialKeyCode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

public class KeyboardController {
    private final HashMap<KeyboardModifier, Boolean> modes;
    private final KeyboardInputHandler inputHandler;
    private final EditText keyboard;
    private int textStart;

    public KeyboardController(KeyboardInputHandler inputHandler, EditText keyboard) {
        this.inputHandler = inputHandler;
        this.keyboard = keyboard;
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
//                System.out.printf("DATA: %d %d %d\n", start, before, count);

                if(before > count || textStart > start) {
                    inputHandler.onKeyPressed('\0', SpecialKeyCode.BACKSPACE, null);
                }
                else {
                    char pressed = s.charAt(start + count - 1);
//                    System.out.println("PRESSED -> " + pressed);
                    inputHandler.onKeyPressed(pressed, SpecialKeyCode.NONE, getEnabledKeys());

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

}
