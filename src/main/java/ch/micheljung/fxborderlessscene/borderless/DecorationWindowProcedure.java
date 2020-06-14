package ch.micheljung.fxborderlessscene.borderless;

import ch.micheljung.fxborderlessscene.borderless.User32Ex.NCCALCSIZE_PARAMS;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Window;

import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.CAPTION;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.HTCLIENT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.TOP;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.TOPLEFT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.TOPRIGHT;
import static com.sun.jna.platform.win32.WinDef.HWND;
import static com.sun.jna.platform.win32.WinDef.LPARAM;
import static com.sun.jna.platform.win32.WinDef.LRESULT;
import static com.sun.jna.platform.win32.WinDef.RECT;
import static com.sun.jna.platform.win32.WinDef.WPARAM;
import static com.sun.jna.platform.win32.WinUser.GWL_WNDPROC;
import static com.sun.jna.platform.win32.WinUser.WM_DESTROY;

class DecorationWindowProcedure implements WinUser.WindowProc {

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

  private static final LRESULT WVR_VALIDRECTS = new LRESULT(0x0400);

  public static final int DEFAULT_FRAME_DRAG_HEIGHT = 30;

  private static final User32Ex user32Ex = User32Ex.INSTANCE;

  public static final LRESULT LRESULT_ZERO = new LRESULT(0);

  private final WindowController windowController;

  private final BaseTSD.LONG_PTR defaultWindowsProcedure;
  private WinVer winVer;

  DecorationWindowProcedure(WindowController windowController, BaseTSD.LONG_PTR defaultWindowsProcedure) {
    this.windowController = windowController;
    this.defaultWindowsProcedure = defaultWindowsProcedure;
  }

  @Override
  public LRESULT callback(HWND hwnd, int uMsg, WPARAM wparam, LPARAM lParam) {
    LRESULT lresult;
    switch (uMsg) {
      case WM_NCHITTEST:
        lresult = user32Ex.CallWindowProc(defaultWindowsProcedure, hwnd, uMsg, wparam, lParam);
        if (lresult.intValue() == HitTestResult.HTCLIENT.windowsValue) {
          lresult = hitTest(hwnd);
        }
        return lresult;

      case WM_DESTROY:
        user32Ex.SetWindowLongPtr(hwnd, GWL_WNDPROC, defaultWindowsProcedure);
        return LRESULT_ZERO;

      case WM_NCCALCSIZE:
        if (wparam.intValue() == 1) {
          NCCALCSIZE_PARAMS nCalcSizeParams = new NCCALCSIZE_PARAMS(new Pointer(lParam.longValue()));
          nCalcSizeParams.rgrc[0].top += 0;
//          nCalcSizeParams.rgrc[0].top += 30;
          nCalcSizeParams.rgrc[0].right -= 8;
          nCalcSizeParams.rgrc[0].bottom -= 8;
          nCalcSizeParams.rgrc[0].left += 8;
          nCalcSizeParams.write();
          return WVR_VALIDRECTS;
        }
        return LRESULT_ZERO;

      default:
        return user32Ex.CallWindowProc(defaultWindowsProcedure, hwnd, uMsg, wparam, lParam);
    }
  }

  /** Determines which area is used for resize and which area is used for dragging. */
  private LRESULT hitTest(HWND hWnd) {
    WinDef.POINT mouse = new WinDef.POINT();
    RECT window = new RECT();
    User32.INSTANCE.GetCursorPos(mouse);
    User32.INSTANCE.GetWindowRect(hWnd, window);

    return new LRESULT(hitTest(new Rect(window), new Point(mouse), windowController).windowsValue);
  }

  static HitTestResult hitTest(Rect window, Point mouse, WindowController controller) {
    Window stage = controller.windowRoot.getScene().getWindow();

    Region controlBox = controller.controlBox;
    Region titleBar = controller.titleBar;

    Pane leftMenuBar = controller.leftMenuBar;
    Pane rightMenuBar = controller.rightMenuBar;
    Node icon = controller.icon;

    int top = window.top;
    int left = window.left;
    int right = window.right;
    double frameDragHeight = titleBar != null
      ? titleBar.getHeight() : controlBox != null
      ? controlBox.getHeight() : DEFAULT_FRAME_DRAG_HEIGHT;

    HitTestResult result = HTCLIENT;

    int resizeBorderThickness = controller.getFrameResizeBorderThickness();
    if (mouse.y < top + resizeBorderThickness) {
      if (mouse.x < left + resizeBorderThickness) {
        result = TOPLEFT;
      } else if (mouse.x > right - resizeBorderThickness) {
        result = TOPRIGHT;
      } else {
        result = TOP;
      }
    }

    if (result == HTCLIENT && mouse.y < top + frameDragHeight) {
      result = CAPTION;
    }

    if (result != HTCLIENT && (
      isMouseOn(mouse, controlBox)
        || isMouseOn(mouse, icon)
        || isMouseOn(mouse, leftMenuBar)
        || isMouseOn(mouse, rightMenuBar))) {
      return HTCLIENT;
    }

    return result;
  }

  private static boolean isMouseOn(Point mouse, Node node) {
    // TODO JavaFX thinks that the node is 30 lower on the screen. I'm not yet sure how to fix/compensate it properly
    // FIXME this fact probably also breaks some UI
    return node != null && node.localToScreen(node.getBoundsInLocal()).contains(mouse.x, mouse.y+30);
  }
}