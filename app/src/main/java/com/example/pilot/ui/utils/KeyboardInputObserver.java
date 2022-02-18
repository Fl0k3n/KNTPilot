package com.example.pilot.ui.utils;
import com.example.pilot.utils.KeyboardModifier;
import com.example.pilot.utils.SpecialKeyCode;

import java.util.List;

public interface KeyboardInputObserver {
    void onKeyPressed(char key, SpecialKeyCode code, List<KeyboardModifier> modifiers);
}
