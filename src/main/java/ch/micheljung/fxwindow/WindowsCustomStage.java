package ch.micheljung.fxwindow;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import javafx.stage.Stage;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

import static com.sun.jna.platform.win32.WinUser.GWL_WNDPROC;
import static com.sun.jna.platform.win32.WinUser.SWP_FRAMECHANGED;
import static com.sun.jna.platform.win32.WinUser.SWP_NOMOVE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOOWNERZORDER;
import static com.sun.jna.platform.win32.WinUser.SWP_NOSIZE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOZORDER;

@Log4j2
class WindowsCustomStage {

  private static final User32Ex user32Ex = User32Ex.INSTANCE;
  private static final DwmApi dwmApi = DwmApi.INSTANCE;
  private static final WinVer winVer;

  static {
    WinNT.OSVERSIONINFO osVersionInfo = new WinNT.OSVERSIONINFO();
    if (!Kernel32.INSTANCE.GetVersionEx(osVersionInfo)) {
      throw new RuntimeException("Could not read version");
    }

    winVer = new WinVer(osVersionInfo);
  }

  public static void configure(WindowController controller, Features features) {
    WinDef.HWND hwnd = getWindowPointer();

    BaseTSD.LONG_PTR defaultWindowsProcedure = user32Ex.GetWindowLongPtr(hwnd, GWL_WNDPROC);

    user32Ex.SetWindowLongPtr(hwnd, GWL_WNDPROC, new DecorationWindowProcedure(controller, defaultWindowsProcedure, features));

    user32Ex.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0,
      SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED | SWP_NOOWNERZORDER);

    if (features.isUseBlurBehind()) {
      enableBlurBehind(hwnd);
    }
    Stage stage = controller.getStage();
    // Trigger Window.synthesizeViewMoveEvent() to recalculate the stage's position on the window.
    stage.setResizable(!stage.isResizable());
    stage.setResizable(!stage.isResizable());
  }

  private static WinDef.HWND getWindowPointer() {
    WinDef.HWND hwnd = new WinDef.HWND();
    hwnd.setPointer(User32.INSTANCE.GetActiveWindow().getPointer());
    return hwnd;
  }

  private static void enableBlurBehind(WinDef.HWND hwnd) {
    if (winVer.isWindows7()) {
      DwmApi.DwmBlurBehind pBlurBehind = new DwmApi.DwmBlurBehind();
      pBlurBehind.dwFlags = new WinDef.DWORD(DwmApi.DWM_BB_ENABLE | DwmApi.DWM_BB_BLURREGION);
      pBlurBehind.fEnable = new WinDef.BOOL(true);
      pBlurBehind.hRgnBlur = new WinDef.HRGN();

      WinNT.HRESULT result = dwmApi.DwmEnableBlurBehindWindow(hwnd, pBlurBehind);
      if (!Objects.equals(result, WinError.S_OK)) {
        throw new IllegalStateException("Could not enable blur behind");
      }
    } else if (winVer.isWindows10()) {
      DwmApi.AccentPolicy accent = new DwmApi.AccentPolicy();
      accent.accentState = DwmApi.AccentState.ACCENT_ENABLE_BLURBEHIND;
      accent.write();

      User32Ex.WindowCompositionAttributeData data = new User32Ex.WindowCompositionAttributeData();
      data.attribute = DwmApi.WindowCompositionAttribute.DWMWA_ACCENT_POLICY.ordinal();
      data.data = accent.getPointer();
      data.sizeOfData = accent.size();

      WinNT.HRESULT hresult = User32Ex.INSTANCE.SetWindowCompositionAttribute(hwnd, data);
      if (hresult.intValue() != 1) {
        throw new IllegalStateException("Could not call SetWindowCompositionAttribute: " + hresult);
      }
    } else {
      log.debug("Blur behind is not supported on {}", winVer);
    }
  }
}
