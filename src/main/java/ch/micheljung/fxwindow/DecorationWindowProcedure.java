package ch.micheljung.fxwindow;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinUser;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

import static com.sun.jna.platform.win32.WinDef.HWND;
import static com.sun.jna.platform.win32.WinDef.LPARAM;
import static com.sun.jna.platform.win32.WinDef.LRESULT;
import static com.sun.jna.platform.win32.WinDef.RECT;
import static com.sun.jna.platform.win32.WinDef.WPARAM;
import static com.sun.jna.platform.win32.WinUser.GWL_WNDPROC;
import static com.sun.jna.platform.win32.WinUser.WM_DESTROY;

class DecorationWindowProcedure implements WinUser.WindowProc {

  // Strong references need to be kept to prevent garbage collection
  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private static final List<DecorationWindowProcedure> references = new ArrayList<>();

  /**
   * Sent when the size and position of a window's client area must be calculated. By processing this message, an
   * application can control the content of the window's client area when the size or position of the window changes.
   */
  private static final int WM_NCCALCSIZE = 0x0083;
  private static final int WM_ENTERSIZEMOVE = 0x0231;

  /**
   * Sent to a window when the size or position of the window is about to change. An application can use this message to
   * override the window's default maximized size and position, or its default minimum or maximum tracking size.
   */
  private static final int WM_GETMINMAXINFO = 0x0024;

  /** Sent to an icon when the user requests that the window be restored to its previous size and position. */
  private static final int WM_QUERYOPEN = 0x0013;

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
  private final Features features;
  private WinDef.POINT ptMaxSize;
  private WinDef.POINT ptMaxPosition;

  DecorationWindowProcedure(WindowController windowController, BaseTSD.LONG_PTR defaultWindowsProcedure, Features features) {
    this.windowController = windowController;
    this.defaultWindowsProcedure = defaultWindowsProcedure;
    this.features = features;

    references.add(this);
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
        references.remove(this);
        return LRESULT_ZERO;

      case WM_NCCALCSIZE:
        if (wparam.intValue() != 0) {
          User32Ex.NCCALCSIZE_PARAMS nCalcSizeParams = new User32Ex.NCCALCSIZE_PARAMS(new Pointer(lParam.longValue()));
          int resizeBorderThickness = windowController.getResizeBorderThickness();
          // Window is maximized in which case the stage goes off-screen at the top. To avoid that, we set top to 0
          // but this must only be done when the window is actually being maximized. When the user drags to restore,
          // 'top' usually is still negative but in that case 'top' must not be set to 0.
          if (isMaximized(nCalcSizeParams.rgrc[0])) {
            nCalcSizeParams.rgrc[0].top = Math.max(nCalcSizeParams.rgrc[0].top, 0);
          }
          nCalcSizeParams.rgrc[0].right -= resizeBorderThickness;
          nCalcSizeParams.rgrc[0].bottom -= resizeBorderThickness;
          nCalcSizeParams.rgrc[0].left += resizeBorderThickness;
          nCalcSizeParams.write();
          return WVR_VALIDRECTS;
        }
        return LRESULT_ZERO;

      case WM_GETMINMAXINFO:
        lresult = user32Ex.CallWindowProc(defaultWindowsProcedure, hwnd, uMsg, wparam, lParam);
        User32Ex.MinMaxInfo minMaxInfo = new User32Ex.MinMaxInfo(new Pointer(lParam.longValue()));
        minMaxInfo.read();
        ptMaxSize = minMaxInfo.ptMaxSize;
        ptMaxPosition = minMaxInfo.ptMaxPosition;
        return lresult;

      default:
        return user32Ex.CallWindowProc(defaultWindowsProcedure, hwnd, uMsg, wparam, lParam);
    }
  }

  // I have no idea how to properly detect whether it's maximized because the taskbar will interfere
  private boolean isMaximized(RECT rect) {
    if (ptMaxPosition == null) {
      return false;
    }
    short count = 0;
    count += (rect.top == ptMaxPosition.y) ? 1 : 0;
    count += (rect.left == ptMaxPosition.x) ? 1 : 0;
    count += ((rect.right - ptMaxPosition.x) == ptMaxSize.x) ? 1 : 0;
    count += ((rect.bottom - ptMaxPosition.y) == ptMaxSize.y) ? 1 : 0;
    return count > 2;
  }

  /** Determines which area is used for resize and which area is used for dragging. */
  private LRESULT hitTest(HWND hWnd) {
    WinDef.POINT mouse = new WinDef.POINT();
    RECT window = new RECT();
    User32.INSTANCE.GetCursorPos(mouse);
    User32.INSTANCE.GetWindowRect(hWnd, window);

    return new LRESULT(hitTest(new Rect(window), new Point(mouse), windowController, features.isAllowTopResize()).windowsValue);
  }

  static HitTestResult hitTest(Rect window, Point mouse, WindowController controller, boolean allowTopResize) {
    Region controlBox = controller.controlBox;
    Region titleBar = controller.getTitleBar();

    Node icon = controller.getIcon();

    int top = window.top;
    int left = window.left;
    int right = window.right;
    double frameDragHeight = (titleBar != null
      ? titleBar.getHeight() : controlBox != null
      ? controlBox.getHeight() : DEFAULT_FRAME_DRAG_HEIGHT) * Screen.getPrimary().getOutputScaleY();

    HitTestResult result = HitTestResult.HTCLIENT;

    int resizeBorderThickness = (int) (controller.getResizeBorderThickness() * Screen.getPrimary().getOutputScaleX());
    int topResizeBorderThickness = (int) (resizeBorderThickness - 3 * Screen.getPrimary().getOutputScaleX());
    if (mouse.y < top + topResizeBorderThickness) {
      if (mouse.x < left + resizeBorderThickness) {
        result = HitTestResult.TOPLEFT;
      } else if (mouse.x > right - resizeBorderThickness) {
        result = HitTestResult.TOPRIGHT;
      } else if (allowTopResize) {
        result = HitTestResult.TOP;
      }
    }

    // Maximized, 'top' is a negative value which needs to be compensated here
    int topInset = ((Stage) controller.windowRoot.getScene().getWindow()).isMaximized() ? -top : 0;
    if (result == HitTestResult.HTCLIENT && mouse.y <= top + topInset + frameDragHeight) {
      result = HitTestResult.CAPTION;
    }

    if (result != HitTestResult.HTCLIENT && (
      isMouseOn(mouse, controlBox, topInset)
        || isMouseOn(mouse, icon, topInset)
        || controller.getNonCaptionNodes().stream().anyMatch(node -> isMouseOn(mouse, node, topInset)))) {
      return HitTestResult.HTCLIENT;
    }

    return result;
  }

  private static boolean isMouseOn(Point mouse, Node node, int topInset) {
    if (node == null || !node.isManaged()) {
      return false;
    }
    Bounds bounds = node.localToScreen(node.getBoundsInLocal());
    if (bounds == null) {
      return false;
    }

    double scaledTopInset = topInset / Screen.getPrimary().getOutputScaleY();
    double scaledMouseX = mouse.x / Screen.getPrimary().getOutputScaleX();
    double scaledMouseY = mouse.y / Screen.getPrimary().getOutputScaleY();

    // Can't use bounds.contains() because it deals with doubles not integers, which causes rounding issues
    return scaledMouseX >= bounds.getMinX() && scaledMouseX <= bounds.getMaxX()
      && scaledMouseY >= bounds.getMinY() + scaledTopInset && scaledMouseY <= bounds.getMaxY() + scaledTopInset;
  }
}