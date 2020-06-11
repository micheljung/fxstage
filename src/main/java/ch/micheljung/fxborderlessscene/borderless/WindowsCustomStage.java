package ch.micheljung.fxborderlessscene.borderless;

import ch.micheljung.fxborderlessscene.borderless.DwmApi.AccentState;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;

import java.util.Objects;

import static com.sun.jna.platform.win32.WinUser.GWL_WNDPROC;
import static com.sun.jna.platform.win32.WinUser.SWP_FRAMECHANGED;
import static com.sun.jna.platform.win32.WinUser.SWP_NOMOVE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOOWNERZORDER;
import static com.sun.jna.platform.win32.WinUser.SWP_NOSIZE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOZORDER;

class WindowsCustomStage extends CustomStage {

  private static final User32Ex user32Ex = User32Ex.INSTANCE;
  private static final DwmApi dwmApi = DwmApi.INSTANCE;

  WindowsCustomStage(ComponentDimensions componentBinding, boolean alpha, boolean blurBehind, boolean useAcrylic) {
    WinDef.HWND hwnd = new WinDef.HWND();
    hwnd.setPointer(User32.INSTANCE.GetActiveWindow().getPointer());

    BaseTSD.LONG_PTR defaultWindowsProcedure = user32Ex.GetWindowLongPtr(hwnd, GWL_WNDPROC);

    user32Ex.SetWindowLongPtr(hwnd, GWL_WNDPROC,
      new CustomDecorationWindowProcedure(componentBinding, defaultWindowsProcedure));

    user32Ex.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0,
      SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED | SWP_NOOWNERZORDER);

    WinVer winVer = getVersion();

    if (winVer.isWindows7() || winVer.isWindows10() && winVer.getBuild() >= WinVer.WIN_1909_BUILD) {
      // Starting with Windows 10, Version 1909, settings any margin here will create a drop shadow
      // Before Windows 10, Version 1909, setting any margin creates undesired results because the function was meant for Win7 Aero glass.
      // See https://stackoverflow.com/questions/34414751/dwmextendframeintoclientarea-strange-behaviour-on-windows-10
      extendFrameIntoClientArea(hwnd, winVer, new DwmApi.Margins(0, 0, 0, -1));
    }
    if (blurBehind) {
      if (winVer.isWindows10() && winVer.getBuild() < WinVer.WIN_1909_BUILD) {
        enableBlurBehind(hwnd);
      }
      if (winVer.isWindows10() && winVer.getBuild() >= WinVer.WIN_1909_BUILD) {
        setAero10(hwnd, winVer);
      }
    }

    user32Ex.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0,
      SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED | SWP_NOOWNERZORDER);
  }

  private void extendFrameIntoClientArea(WinDef.HWND hwnd, WinVer winVer, DwmApi.Margins margins) {
    WinNT.HRESULT hresult = dwmApi.DwmExtendFrameIntoClientArea(hwnd, margins);
    if (!Objects.equals(hresult, WinError.S_OK)) {
      throw new IllegalStateException("Could not call DwmExtendFrameIntoClientArea");
    }
  }

  private WinVer getVersion() {
    WinNT.OSVERSIONINFO osVersionInfo = new WinNT.OSVERSIONINFO();

    if (!Kernel32.INSTANCE.GetVersionEx(osVersionInfo)) {
      throw new RuntimeException("Could not read version");
    }

    return new WinVer(osVersionInfo);
  }

  private void enableBlurBehind(WinDef.HWND hwnd) {
    DwmApi.DwmBlurBehind pBlurBehind = new DwmApi.DwmBlurBehind();
    pBlurBehind.dwFlags = new WinDef.DWORD(DwmApi.DWM_BB_ENABLE | DwmApi.DWM_BB_BLURREGION | DwmApi.DWM_BB_TRANSITIONONMAXIMIZED);
    pBlurBehind.fEnable = new WinDef.BOOL(true);
    pBlurBehind.hRgnBlur = new WinDef.HRGN();
    pBlurBehind.fTransitionOnMaximized = new WinDef.BOOL(true);

    WinNT.HRESULT result = dwmApi.DwmEnableBlurBehindWindow(hwnd, pBlurBehind);
    if (!Objects.equals(result, WinError.S_OK)) {
      throw new IllegalStateException("Could not enable blur behind");
    }
  }

  private void setAero10(WinDef.HWND hwnd, WinVer winVer) {
    DwmApi.AccentPolicy accent = new DwmApi.AccentPolicy();

    // TODO check which feature is desired by the user
//    if (winVer.getBuild() < WinVer.WIN_1803_BUILD) {
      accent.accentState = AccentState.ACCENT_ENABLE_BLURBEHIND;
//    } else if (winVer.getBuild() < WinVer.WIN_1809_BUILD) {
//      accent.accentState = AccentState.ACCENT_ENABLE_ACRYLICBLURBEHIND;
//    } else {
//      accent.accentState = AccentState.ACCENT_ENABLE_HOSTBACKDROP;
//    }
    accent.write();

    User32Ex.WindowCompositionAttributeData data = new User32Ex.WindowCompositionAttributeData();
    data.attribute = DwmApi.WindowCompositionAttribute.DWMWA_ACCENT_POLICY;
    data.data = accent.getPointer();
    data.sizeOfData = accent.size();

    WinNT.HRESULT hresult = User32Ex.INSTANCE.SetWindowCompositionAttribute(hwnd, data);
    if (hresult.intValue() != 1) {
      throw new IllegalStateException("Could not call SetWindowCompositionAttribute: " + hresult);
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
  }
}
