package ch.micheljung.fxwindow;

import javafx.geometry.Bounds;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import static ch.micheljung.fxwindow.HitTestResult.BOTTOM;
import static ch.micheljung.fxwindow.HitTestResult.BOTTOMLEFT;
import static ch.micheljung.fxwindow.HitTestResult.BOTTOMRIGHT;
import static ch.micheljung.fxwindow.HitTestResult.CAPTION;
import static ch.micheljung.fxwindow.HitTestResult.HTCLIENT;
import static ch.micheljung.fxwindow.HitTestResult.LEFT;
import static ch.micheljung.fxwindow.HitTestResult.NOWHERE;
import static ch.micheljung.fxwindow.HitTestResult.RIGHT;
import static ch.micheljung.fxwindow.HitTestResult.TOP;
import static ch.micheljung.fxwindow.HitTestResult.TOPLEFT;
import static ch.micheljung.fxwindow.HitTestResult.TOPRIGHT;

/**
 * Hit testing for stages whose visible window ({@link WindowController#windowRoot}) may be surrounded by a transparent
 * shadow area, as used by {@link LinuxCustomStage} and {@link UndecoratedStage}. The shadow area is the resize area. If
 * it's thinner than {@link #MIN_RESIZE_BORDER}, the resize area extends into the visible window.
 */
final class FrameHitTest {

  /** Minimum width of the resize area, measured from the stage's edge. Matters if the shadow area is thinner. */
  private static final double MIN_RESIZE_BORDER = 8;
  /** Length of the corner resize areas along the edges, measured from the visible window's corners. */
  private static final double CORNER_SIZE = 16;

  private FrameHitTest() {
  }

  /** Tests the specified screen coordinates. */
  static HitTestResult hitTest(Stage stage, WindowController controller, double x, double y, boolean allowTopResize) {
    Region windowRoot = controller.windowRoot;
    Bounds frame = windowRoot.localToScreen(windowRoot.getLayoutBounds());
    if (frame == null) {
      return NOWHERE;
    }

    HitTestResult result = NOWHERE;
    if (stage.isResizable() && !stage.isMaximized() && !stage.isFullScreen()) {
      result = resizeEdge(stage, frame, x, y, allowTopResize);
    }

    if (result == NOWHERE && frame.contains(x, y)) {
      result = y < frame.getMinY() + getCaptionHeight(controller) ? CAPTION : HTCLIENT;
    }

    if (result != HTCLIENT && result != NOWHERE && (
      isMouseOn(x, y, controller.controlBox)
        || isMouseOn(x, y, controller.getIcon())
        || controller.getNonCaptionNodes().stream().anyMatch(node -> isMouseOn(x, y, node)))) {
      return HTCLIENT;
    }
    return result;
  }

  private static HitTestResult resizeEdge(Stage stage, Bounds frame, double x, double y, boolean allowTopResize) {
    double left = stage.getX();
    double top = stage.getY();
    double right = left + stage.getWidth();
    double bottom = top + stage.getHeight();

    boolean onLeft = x < Math.max(frame.getMinX(), left + MIN_RESIZE_BORDER);
    boolean onRight = !onLeft && x >= Math.min(frame.getMaxX(), right - MIN_RESIZE_BORDER);
    boolean onTop = allowTopResize && y < Math.max(frame.getMinY(), top + MIN_RESIZE_BORDER);
    boolean onBottom = !onTop && y >= Math.min(frame.getMaxY(), bottom - MIN_RESIZE_BORDER);

    if (onTop || onBottom) {
      // Extend corners along the horizontal edges
      onLeft |= x < frame.getMinX() + CORNER_SIZE;
      onRight |= !onLeft && x >= frame.getMaxX() - CORNER_SIZE;
    } else if (onLeft || onRight) {
      // Extend corners along the vertical edges
      onTop = allowTopResize && y < frame.getMinY() + CORNER_SIZE;
      onBottom = !onTop && y >= frame.getMaxY() - CORNER_SIZE;
    }

    if (onTop) {
      return onLeft ? TOPLEFT : onRight ? TOPRIGHT : TOP;
    }
    if (onBottom) {
      return onLeft ? BOTTOMLEFT : onRight ? BOTTOMRIGHT : BOTTOM;
    }
    return onLeft ? LEFT : onRight ? RIGHT : NOWHERE;
  }

  private static double getCaptionHeight(WindowController controller) {
    Region titleBar = controller.getTitleBar();
    if (titleBar != null) {
      return titleBar.getHeight();
    }
    if (controller.controlBox != null) {
      return controller.controlBox.getHeight();
    }
    return UndecoratedStage.DEFAULT_FRAME_DRAG_HEIGHT;
  }

  static boolean isResize(HitTestResult hitTestResult) {
    return resizeCursor(hitTestResult) != null;
  }

  /** Returns the cursor for the specified resize area, or {@code null} if it isn't one. */
  static Cursor resizeCursor(HitTestResult hitTestResult) {
    switch (hitTestResult) {
      case TOPLEFT:
        return Cursor.NW_RESIZE;
      case TOP:
        return Cursor.N_RESIZE;
      case TOPRIGHT:
        return Cursor.NE_RESIZE;
      case LEFT:
        return Cursor.W_RESIZE;
      case RIGHT:
        return Cursor.E_RESIZE;
      case BOTTOMLEFT:
        return Cursor.SW_RESIZE;
      case BOTTOM:
        return Cursor.S_RESIZE;
      case BOTTOMRIGHT:
        return Cursor.SE_RESIZE;
      default:
        return null;
    }
  }

  private static boolean isMouseOn(double x, double y, Node node) {
    if (node == null || !node.isManaged() || !node.isVisible()) {
      return false;
    }
    Bounds bounds = node.localToScreen(node.getBoundsInLocal());
    return bounds != null && bounds.contains(x, y);
  }
}
