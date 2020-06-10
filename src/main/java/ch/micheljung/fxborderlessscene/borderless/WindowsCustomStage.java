package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

class WindowsCustomStage extends CustomStage {

  WindowsCustomStage(ComponentDimensions componentBinding, boolean alpha, boolean blurBehind) {
    WinDef.HWND hwnd = new WinDef.HWND();
    hwnd.setPointer(User32.INSTANCE.GetActiveWindow().getPointer());
    new CustomDecorationWindowProcedure(hwnd, componentBinding, alpha, blurBehind);
  }
}
