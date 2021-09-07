package com.example.pilot.networking;
import java.util.List;

public interface KeyboardInputHandler {
    void onKeyPressed(char key, SpecialKeyCode code, List<KeyboardModifier> modifiers);
}
