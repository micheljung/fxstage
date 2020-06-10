package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.BOOL;
import com.sun.jna.platform.win32.WinDef.DWORD;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.WinUser;

import static com.sun.jna.platform.win32.WinDef.HWND;
import static com.sun.jna.platform.win32.WinDef.LPARAM;
import static com.sun.jna.platform.win32.WinDef.LRESULT;
import static com.sun.jna.platform.win32.WinDef.RECT;
import static com.sun.jna.platform.win32.WinDef.WPARAM;
import static com.sun.jna.platform.win32.WinUser.GWL_WNDPROC;
import static com.sun.jna.platform.win32.WinUser.SWP_FRAMECHANGED;
import static com.sun.jna.platform.win32.WinUser.SWP_NOMOVE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOOWNERZORDER;
import static com.sun.jna.platform.win32.WinUser.SWP_NOSIZE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOZORDER;
import static com.sun.jna.platform.win32.WinUser.WM_DESTROY;

/** Custom callback and hit testing function. */
class CustomDecorationWindowProcedure implements WinUser.WindowProc {

  /**
   * Sent when the size and position of a window's client area must be calculated. By processing this message, an
   * application can control the content of the window's client area when the size or position of the window changes.
   */
  private static final int WM_NCCALCSIZE = 0x0083;

  /**
   * Sent to a window in order to determine what part of the window corresponds to a particular screen coordinate. This
   * can happen, for example, when the cursor moves, when a mouse button is pressed or released, or in response to a
   * call to a function such as WindowFromPoint. If the mouse is not captured, the message is sent to the window beneath
   * the cursor. Otherwise, the message is sent to the window that has captured the mouse.
   */
  private static final int WM_NCHITTEST = 0x0084;

  private static final int WM_ACTIVATE = 0x0006;

  private static final int WA_ACTIVE = 1;
  private static final int WA_CLICKACTIVE = 2;
  private static final int WA_INACTIVE = 0;

  private static final User32Ex user32Ex = User32Ex.INSTANCE;

  public static final LRESULT LRESULT_ZERO = new LRESULT(0);
  private static final DwmApi dwmApi = DwmApi.INSTANCE;

  private final ComponentDimensions dimensions;

  private final BaseTSD.LONG_PTR defaultWindowsProcedure;
  private final boolean alpha;
  private final boolean blurBehind;

  CustomDecorationWindowProcedure(HWND hwnd, ComponentDimensions dimensions, boolean alpha, boolean blurBehind) {
    this.dimensions = dimensions;
    this.alpha = alpha;
    this.blurBehind = blurBehind;

    defaultWindowsProcedure = user32Ex.SetWindowLongPtr(hwnd, GWL_WNDPROC, this);

    enableBlurBehind(hwnd);
    if (blurBehind && isBlurBehindAvailable()) {
      enableBlurBehind(hwnd);
    }

    WinDef.BOOLByReference boolByReference = new WinDef.BOOLByReference();
    WinDef.LPVOID pvAttribute = new WinDef.LPVOID(boolByReference.getPointer());
    dwmApi.DwmGetWindowAttribute(hwnd, new DWORD(DwmApi.WindowCompositionAttribute.DWMWA_NCRENDERING_ENABLED), pvAttribute, new DWORD(BOOL.SIZE));

    if (boolByReference.getValue().booleanValue()) {
      System.out.println("Enabled");
    }

    if (isWindows7()) {
      extendFrameIntoClientArea(hwnd);
      if (blurBehind) {
        enableBlurBehind(hwnd);
      }
    }
    if (isWindows10()) {
//      setAero10(hwnd);
    }


    user32Ex.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0,
      SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED | SWP_NOOWNERZORDER);
  }

  private boolean isWindows7() {
    // FIXME implement
    return false;
  }

  private boolean isWindows10() {
    // FIXME implement
    return true;
  }

  private boolean isBlurBehindAvailable() {
    BOOL pfEnabled = new BOOL();
    dwmApi.DwmIsCompositionEnabled(pfEnabled);
    return pfEnabled.booleanValue();
  }

  private void enableBlurBehind(HWND hwnd) {
    DwmApi.DwmBlurBehind pBlurBehind = new DwmApi.DwmBlurBehind();
    pBlurBehind.dwFlags = new DWORD(DwmApi.DWM_BB_ENABLE);
    pBlurBehind.fEnable = new BOOL(true);

    dwmApi.DwmEnableBlurBehindWindow(hwnd, pBlurBehind);
  }

  @Override
  public LRESULT callback(HWND hwnd, int uMsg, WPARAM wparam, LPARAM lparam) {
    LRESULT lresult;
    switch (uMsg) {
      case WM_NCHITTEST:
        lresult = hitTest(hwnd);
        if (lresult.intValue() == LRESULT_ZERO.intValue()) {
          return user32Ex.CallWindowProc(defaultWindowsProcedure, hwnd, uMsg, wparam, lparam);
        }
        return lresult;

      case WM_DESTROY:
        user32Ex.SetWindowLongPtr(hwnd, GWL_WNDPROC, defaultWindowsProcedure);
        return LRESULT_ZERO;

      case WM_NCCALCSIZE:
        // This will cause windows not to draw the non-client area and thereby effectively making all the window our client area
        if (wparam.intValue() == 1) {
          return LRESULT_ZERO;
        }
        // Falls through

      default:
        return user32Ex.CallWindowProc(defaultWindowsProcedure, hwnd, uMsg, wparam, lparam);
    }
  }

  private void extendFrameIntoClientArea(HWND hwnd) {
    DwmApi.Margins pMarInset = new DwmApi.Margins();
    if (isWindows7()) {
      pMarInset.cxLeftWidth = 1;
    } else if (isWindows10()) {
      pMarInset.cxLeftWidth = 0;
    }

    HRESULT hresult = dwmApi.DwmExtendFrameIntoClientArea(hwnd, pMarInset);
    if (hresult.intValue() != 0) {
      throw new IllegalStateException("Could not call DwmExtendFrameIntoClientArea");
    }
  }

//  private void setAero10(HWND hwnd) {
//    // FIXME somehow on Win10 1809 this doesn't work
//    if (false) {
//      return;
//    }
//
//    DwmApi.AccentPolicy accent = new DwmApi.AccentPolicy();
//    accent.accentState = DwmApi.AccentState.ACCENT_ENABLE_BLURBEHIND;
//    accent.write();
//
//    User32Ex.WindowCompositionAttributeData data = new User32Ex.WindowCompositionAttributeData();
//    data.attribute = DwmApi.WindowCompositionAttribute.WCA_ACCENT_POLICY;
//    data.data = accent.getPointer();
//    data.sizeOfData = accent.size();
//
//    HRESULT hresult = user32Ex.SetWindowCompositionAttribute(hwnd, data);
//    if (hresult.intValue() != 0) {
//      throw new IllegalStateException("Could not call SetWindowCompositionAttribute: " + hresult.intValue());
//    }
//  }

  /** Determines which area is used for resize and which area is used for dragging. */
  private LRESULT hitTest(HWND hWnd) {
    WinDef.POINT mouse = new WinDef.POINT();
    RECT window = new RECT();
    User32.INSTANCE.GetCursorPos(mouse);
    User32.INSTANCE.GetWindowRect(hWnd, window);

    return new LRESULT(HitTest.hitTest(new Rect(window), new Point(mouse), dimensions).windowsValue);
  }
}