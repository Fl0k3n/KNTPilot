import pyautogui


class InputController:
    def click(self, x, y):
        prev_x, prev_y = pyautogui.position()
        pyautogui.click(x, y)
        pyautogui.moveTo(prev_x, prev_y)

    def press_key(self, key: str):
        if key == '\n':
            pyautogui.press('enter')
        else:
            try:
                pyautogui.press(key)
            except Exception as e:
                print(f"failed to press key ->{key}<-")
                print(e)
