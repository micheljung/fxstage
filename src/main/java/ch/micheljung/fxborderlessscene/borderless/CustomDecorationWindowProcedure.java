package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.W32APIOptions;

import java.util.Arrays;
import java.util.List;

import static com.sun.jna.platform.win32.WinDef.HWND;
import static com.sun.jna.platform.win32.WinDef.LPARAM;
import static com.sun.jna.platform.win32.WinDef.LRESULT;
import static com.sun.jna.platform.win32.WinDef.RECT;
import static com.sun.jna.platform.win32.WinDef.WPARAM;
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

  private static final int HTTOPLEFT = 13;
  private static final int HTTOP = 12;
  private static final int HTCAPTION = 2;
  private static final int HTTOPRIGHT = 14;
  private static final int HTLEFT = 10;
  private static final int HTNOWHERE = 0;
  private static final int HTRIGHT = 11;
  private static final int HTBOTTOMLEFT = 16;
  private static final int HTBOTTOM = 15;
  private static final int HTBOTTOMRIGHT = 17;
  private static final int HTSYSMENU = 3;

  private static final User32Ex user32Ex = Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);
  private static final NativeLibrary dwmApi = NativeLibrary.getInstance("dwmapi");
  public static final LRESULT LRESULT_ZERO = new LRESULT(0);

  private final ComponentBinding parameters;

  private final BaseTSD.LONG_PTR defaultWindowsProcedure;

  @Structure.FieldOrder({"cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight"})
  public static class Margins extends Structure implements Structure.ByReference {

    public int cxLeftWidth = -1;
    public int cxRightWidth = -1;
    public int cyTopHeight = -1;
    public int cyBottomHeight = -1;
  }

  CustomDecorationWindowProcedure(HWND hwnd, ComponentBinding parameters) {
    this.parameters = parameters;
    defaultWindowsProcedure = user32Ex.SetWindowLongPtr(hwnd, User32Ex.GWLP_WNDPROC, this);

    extendFrameIntoClientArea(hwnd);

    user32Ex.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0,
      SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED | SWP_NOOWNERZORDER);
  }

  private void extendFrameIntoClientArea(HWND hwnd) {
    HRESULT result = (HRESULT) NativeLibrary.getInstance("dwmapi")
      .getFunction("DwmExtendFrameIntoClientArea")
      .invoke(HRESULT.class, new Object[]{hwnd, new Margins()});

    if (result.intValue() != 0) {
      throw new IllegalStateException("Could not call DwmExtendFrameIntoClientArea");
    }
  }

  @Override
  public LRESULT callback(HWND hwnd, int uMsg, WPARAM wparam, LPARAM lparam) {
    LRESULT lresult;
    switch (uMsg) {
      case WM_NCHITTEST:
        // Determines which area is used for resize and which area is used for dragging (including windows snap).
        lresult = hitTest(hwnd, uMsg, wparam, lparam);
        if (lresult.intValue() == LRESULT_ZERO.intValue()) {
          return user32Ex.CallWindowProc(defaultWindowsProcedure, hwnd, uMsg, wparam, lparam);
        }
        return lresult;

      case WM_DESTROY:
        user32Ex.SetWindowLongPtr(hwnd, User32Ex.GWLP_WNDPROC, defaultWindowsProcedure);
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

  private LRESULT hitTest(HWND hWnd, int message, WPARAM wParam, LPARAM lParam) {
    int borderThickness = parameters.getFrameResizeBorderThickness();

    WinDef.POINT mouse = new WinDef.POINT();
    RECT window = new RECT();
    User32.INSTANCE.GetCursorPos(mouse);
    User32.INSTANCE.GetWindowRect(hWnd, window);

    int row = 1;
    int col = 1;
    boolean isOnResizeBorder = false;
    boolean isOnFrameDrag = false;

    int topOffset = parameters.getTitleBarHeight() == 0 ? borderThickness : parameters.getTitleBarHeight();
    if (mouse.y >= window.top && mouse.y < window.top + topOffset + borderThickness) {
      // Top Resizing
      isOnResizeBorder = (mouse.y < (window.top + borderThickness));

      if (!isOnResizeBorder) {
        isOnFrameDrag = (mouse.y <= window.top + parameters.getTitleBarHeight() + borderThickness)
          && (mouse.x < (window.right - (parameters.getControlBoxWidth()
          + parameters.getExtraRightReservedWidth())))
          && (mouse.x > (window.left + parameters.getIconWidth()
          + parameters.getExtraLeftReservedWidth()));
      }
      // Top Resizing or Caption Moving
      row = 0;
    } else if (mouse.y < window.bottom && mouse.y >= window.bottom - borderThickness) {
      // Bottom Resizing
      row = 2;
    }

    if (mouse.x >= window.left && mouse.x < window.left + borderThickness) {
      // Left Resizing
      col = 0;
    } else if (mouse.x < window.right && mouse.x >= window.right - borderThickness) {
      // Right Resizing
      col = 2;
    }

    if (col != 1 && mouse.y > window.top + borderThickness) {
      // Don't do top left/right resizing for the whole title bar height, just for border thickness
      row = 1;
    }

    int[][] hitTests = {
      {HTTOPLEFT, isOnResizeBorder ? HTTOP : isOnFrameDrag ? HTCAPTION : HTNOWHERE, HTTOPRIGHT},
      {HTLEFT, HTNOWHERE, HTRIGHT},
      {HTBOTTOMLEFT, HTBOTTOM, HTBOTTOMRIGHT},
    };

    return new LRESULT(hitTests[row][col]);
  }
}