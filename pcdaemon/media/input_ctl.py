from typing import List
import pyautogui


class InputController:
    def click(self, x: float, y: float, button: str):
        prev_x, prev_y = pyautogui.position()
        print("BUTTON: ", button)
        pyautogui.click(x, y, button=button)
        # pyautogui.moveTo(prev_x, prev_y)

    def double_click(self, x: float, y: float, button: str):
        pyautogui.doubleClick(x, y, button=button)

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

    def lock_system(self):
        pass  # TODO
        # pyautogui.keyDown('winleft')
        # pyautogui.press('l')
        # pyautogui.keyUp('winleft')
