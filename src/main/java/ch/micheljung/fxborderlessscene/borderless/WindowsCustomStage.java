package ch.micheljung.fxborderlessscene.borderless;

import ch.micheljung.fxborderlessscene.borderless.DwmApi.AccentState;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

import static com.sun.jna.platform.win32.WinUser.GWL_WNDPROC;
import static com.sun.jna.platform.win32.WinUser.SWP_FRAMECHANGED;
import static com.sun.jna.platform.win32.WinUser.SWP_NOMOVE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOOWNERZORDER;
import static com.sun.jna.platform.win32.WinUser.SWP_NOSIZE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOZORDER;

@Log4j2
class WindowsCustomStage extends CustomStage {

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

  WindowsCustomStage(ComponentDimensions componentBinding, boolean alpha, boolean blurBehind, boolean useAcrylic) {
    WinDef.HWND hwnd = new WinDef.HWND();
    hwnd.setPointer(User32.INSTANCE.GetActiveWindow().getPointer());

    BaseTSD.LONG_PTR defaultWindowsProcedure = user32Ex.GetWindowLongPtr(hwnd, GWL_WNDPROC);

    user32Ex.SetWindowLongPtr(hwnd, GWL_WNDPROC,
      new CustomDecorationWindowProcedure(componentBinding, defaultWindowsProcedure));

    user32Ex.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0,
      SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED | SWP_NOOWNERZORDER);

    if (winVer.isWindows7() || winVer.isWindows10() && winVer.getBuild() >= WinVer.WIN_1909_BUILD) {
      // Starting with Windows 10, Version 1909, settings any margin here will create a drop shadow
      // Before Windows 10, Version 1909, setting any margin creates undesired results because the function was meant for Win7 Aero glass.

    }

    if (blurBehind) {
      if (winVer.isWindows7()) {
        enableBlurBehind(hwnd);
      } else if (winVer.isWindows10()) {
//        enableBlurBehind(hwnd);

        if (true/*windowDecorationsEnabled*/) {
          // HACK: When opaque (opacity 255), there is a trail whenever
          // the transparent window is moved. By reducing it to 254,
          // the window is rendered properly.
//          byte opacity = (byte) 254;
//
//          // The color key can be any value except for black (0x0).
//          int color_key = 0x0030c100;
//
//          user32Ex.SetLayeredWindowAttributes(hwnd, color_key, opacity, WinUser.LWA_ALPHA);
        }
      }

      // See https://stackoverflow.com/questions/34414751/dwmextendframeintoclientarea-strange-behaviour-on-windows-10
      if (winVer.getBuild() >= WinVer.WIN_1909_BUILD) {
//        extendFrameIntoClientArea(hwnd, new DwmApi.Margins(-1, -1, -1, -1));
      }
    }
  }
//
//    user32Ex.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0,
//      SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED | SWP_NOOWNERZORDER);

  public void setWindowAlpha(WinDef.HWND hWnd, float alpha) {
    User32 user = User32.INSTANCE;
    int flags = user.GetWindowLong(hWnd, WinUser.GWL_EXSTYLE);
    byte opacity = (byte) ((int) (255 * alpha) & 0xFF);
    if (false /*usingUpdateLayeredWindow(w)*/) {
      // If already using UpdateLayeredWindow, continue to
      // do so
      WinUser.BLENDFUNCTION blend = new WinUser.BLENDFUNCTION();
      blend.SourceConstantAlpha = opacity;
      blend.AlphaFormat = WinUser.AC_SRC_ALPHA;
      user.UpdateLayeredWindow(hWnd, null, null, null, null,
        null, 0, blend,
        WinUser.ULW_ALPHA);
    } else if (alpha == 1f) {
      flags &= ~WinUser.WS_EX_LAYERED;
      user.SetWindowLong(hWnd, WinUser.GWL_EXSTYLE, flags);
    } else {
      flags |= WinUser.WS_EX_LAYERED;
      user.SetWindowLong(hWnd, WinUser.GWL_EXSTYLE, flags);
      user.SetLayeredWindowAttributes(hWnd, 0, opacity, WinUser.LWA_ALPHA);
    }
  }

  /** Only works if window decorations are disabled. */
  private void extendFrameIntoClientArea(WinDef.HWND hwnd, DwmApi.Margins margins) {
    WinNT.HRESULT hresult = dwmApi.DwmExtendFrameIntoClientArea(hwnd, margins);
    if (!Objects.equals(hresult, WinError.S_OK)) {
      throw new IllegalStateException("Could not call DwmExtendFrameIntoClientArea");
    }
  }

  private void enableBlurBehind(WinDef.HWND hwnd) {
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
      accent.accentState = AccentState.ACCENT_ENABLE_BLURBEHIND;
      accent.write();

      User32Ex.WindowCompositionAttributeData data = new User32Ex.WindowCompositionAttributeData();
      data.attribute = DwmApi.WindowCompositionAttribute.DWMWA_ACCENT_POLICY;
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

  private static class WinVer {

    static final int WIN_1803_BUILD = 17134;
    static final int WIN_1809_BUILD = 17763;
    static final int WIN_1909_BUILD = 18363;

    private final WinNT.OSVERSIONINFO osVersionInfo;

    WinVer(WinNT.OSVERSIONINFO osVersionInfo) {
      this.osVersionInfo = osVersionInfo;
    }

    int getBuild() {
      return osVersionInfo.dwBuildNumber.intValue();
    }

    boolean isWindows7() {
      return osVersionInfo.dwMajorVersion.intValue() == 6 && osVersionInfo.dwMinorVersion.intValue() == 1;
    }

    boolean isWindows8() {
      return osVersionInfo.dwMajorVersion.intValue() == 6 && osVersionInfo.dwMinorVersion.intValue() == 2;
    }

    boolean isWindows10() {
      return osVersionInfo.dwMajorVersion.intValue() == 10 && osVersionInfo.dwMinorVersion.intValue() == 0;
    }

    @Override
    public String toString() {
      return String.format("Windows %s.%s Build %s",
        osVersionInfo.dwMajorVersion, osVersionInfo.dwMinorVersion, osVersionInfo.dwBuildNumber);
    }
  }
}
