import pyautogui


class InputController:
    def move_mouse(self, x, y):
        pyautogui.moveTo(x, y)
