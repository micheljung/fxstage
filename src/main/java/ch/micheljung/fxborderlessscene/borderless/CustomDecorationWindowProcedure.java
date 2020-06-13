package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;

import static com.sun.jna.platform.win32.WinDef.HWND;
import static com.sun.jna.platform.win32.WinDef.LPARAM;
import static com.sun.jna.platform.win32.WinDef.LRESULT;
import static com.sun.jna.platform.win32.WinDef.RECT;
import static com.sun.jna.platform.win32.WinDef.WPARAM;
import static com.sun.jna.platform.win32.WinUser.GWL_WNDPROC;
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

  private static final User32Ex user32Ex = User32Ex.INSTANCE;

  public static final LRESULT LRESULT_ZERO = new LRESULT(0);

  private final ComponentDimensions dimensions;

  private final BaseTSD.LONG_PTR defaultWindowsProcedure;

  CustomDecorationWindowProcedure(ComponentDimensions dimensions, BaseTSD.LONG_PTR defaultWindowsProcedure) {
    this.dimensions = dimensions;
    this.defaultWindowsProcedure = defaultWindowsProcedure;
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

  /** Determines which area is used for resize and which area is used for dragging. */
  private LRESULT hitTest(HWND hWnd) {
    WinDef.POINT mouse = new WinDef.POINT();
    RECT window = new RECT();
    User32.INSTANCE.GetCursorPos(mouse);
    User32.INSTANCE.GetWindowRect(hWnd, window);

    return new LRESULT(HitTest.hitTest(new Rect(window), new Point(mouse), dimensions).windowsValue);
  }
}