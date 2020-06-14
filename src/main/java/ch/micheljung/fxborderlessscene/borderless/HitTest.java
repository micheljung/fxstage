package ch.micheljung.fxborderlessscene.borderless;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.Window;

import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.BOTTOM;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.BOTTOMLEFT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.BOTTOMRIGHT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.CAPTION;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.HTCLIENT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.LEFT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.NOWHERE;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.RIGHT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.TOP;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.TOPLEFT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.TOPRIGHT;

public class HitTest {

  public static final int DEFAULT_FRAME_DRAG_HEIGHT = 30;

  static HitTestResult hitTest(Rect window, Point mouse, WindowController controller) {
    Window stage = controller.windowRoot.getScene().getWindow();

    int borderThickness = controller.getFrameResizeBorderThickness();

    int row = 1;
    int col = 1;
    boolean isOnResizeBorder = false;
    boolean isOnFrameDrag = false;

    Region controlBox = controller.controlBox;
    Region titleBar = controller.titleBar;
    int shadowInset = controller.getShadowInset();

    Pane leftMenuBar = controller.leftMenuBar;
    Pane rightMenuBar = controller.rightMenuBar;
    Node icon = controller.icon;

    // FIXME this might behave differently native vs. custom, because of shadow
    int top = window.top;
    int right = window.right;
    int left = window.left;
    double frameDragHeight = titleBar != null
      ? titleBar.getHeight() : controlBox != null
      ? controlBox.getHeight() : DEFAULT_FRAME_DRAG_HEIGHT;

    if (mouse.y >= top && mouse.y <= top + borderThickness + frameDragHeight) {
      // Top Resizing
      isOnResizeBorder = (mouse.y <= (top + borderThickness))
        && !isOn(mouse, controlBox, stage);

      if (!isOnResizeBorder) {
        isOnFrameDrag = mouse.y <= top + borderThickness + frameDragHeight + shadowInset;
      }
      // Top Resizing or Caption Moving
      row = 0;
    } else {
      int bottom = window.bottom - shadowInset;
      if (mouse.y < bottom && mouse.y >= bottom - borderThickness) {
        // Bottom Resizing
        row = 2;
      }
    }

    if (mouse.x >= left && mouse.x < left + borderThickness) {
      // Left Resizing
      col = 0;
    } else if (mouse.x < right && mouse.x >= right - borderThickness) {
      // Right Resizing
      col = 2;
    }

    if (row == 0 && col != 1 && mouse.y > top + borderThickness) {
      // Don't do top left/right resizing for the whole title bar height, just for border thickness
      row = 1;
    }

    if (isOn(mouse, controlBox, stage)
      || isOn(mouse, icon, stage)
      || isOn(mouse, leftMenuBar, stage)
      || isOn(mouse, rightMenuBar, stage)) {
      // Don't do any resizing or dragging on interactive areas
      row = 1;
      col = 1;
    }

    if (row == 0 && col != 1 && mouse.y > top + borderThickness) {
      // Don't do top left/right resizing for the whole title bar height, just for border thickness
      row = 1;
    }

    HitTestResult[][] hitTests = {
      {TOPLEFT, isOnResizeBorder ? TOP : isOnFrameDrag ? CAPTION : NOWHERE, TOPRIGHT},
      {LEFT, HTCLIENT, RIGHT},
      {BOTTOMLEFT, BOTTOM, BOTTOMRIGHT},
    };

    return hitTests[row][col];
  }

  private static boolean isOn(Point mouse, Node controlBox, Window stage) {
    return controlBox != null && controlBox.getBoundsInParent().contains(
      mouse.x - stage.getX(),
      mouse.y - stage.getY()
    );
  }
}
