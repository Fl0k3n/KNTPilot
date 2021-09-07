from typing import List
import pyautogui


class InputController:
    def click(self, x, y):
        prev_x, prev_y = pyautogui.position()
        pyautogui.click(x, y)
        # pyautogui.moveTo(prev_x, prev_y)

    def press_key(self, key: str, modifiers: List[str]):
        for modifier in modifiers:
            pyautogui.keyDown(modifier)

        if key == '\n':
            pyautogui.press('enter')
        else:
            try:
                pyautogui.press(key)
            except Exception as e:
                print(f"failed to press key ->{key}<-")
                print(e)

        for modifier in reversed(modifiers):
            pyautogui.keyUp(modifier)

    def scroll(self, up: bool):
        pyautogui.scroll(3 if up else -3)
