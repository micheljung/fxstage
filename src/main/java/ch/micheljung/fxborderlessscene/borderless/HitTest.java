package ch.micheljung.fxborderlessscene.borderless;

import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.BOTTOM;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.BOTTOMLEFT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.BOTTOMRIGHT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.CAPTION;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.LEFT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.NOWHERE;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.RIGHT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.TOP;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.TOPLEFT;
import static ch.micheljung.fxborderlessscene.borderless.HitTestResult.TOPRIGHT;

public class HitTest {

  static HitTestResult hitTest(Rect window, Point mouse, ComponentDimensions parameters) {
    int borderThickness = parameters.getFrameResizeBorderThickness();

    int row = 1;
    int col = 1;
    boolean isOnResizeBorder = false;
    boolean isOnFrameDrag = false;

    int controlBoxWidth = parameters.getControlBoxWidth();
    int titleBarHeight = parameters.getTitleBarHeight();
    int topOffset = titleBarHeight == 0 ? borderThickness : titleBarHeight;
    // FIXME the location of the right menu bar is not yet considered
    int extraRightReservedWidth = parameters.getExtraRightReservedWidth();
    // FIXME the location of the left menu bar is not yet considered
    int extraLeftReservedWidth = parameters.getExtraLeftReservedWidth();
    // FIXME the location of the icon is not yet considered
    int iconWidth = parameters.getIconWidth();

    if (mouse.y >= window.top && mouse.y < window.top + topOffset + borderThickness) {
      // Top Resizing
      isOnResizeBorder = (mouse.y < (window.top + borderThickness))
        && (mouse.x < window.right - controlBoxWidth - extraRightReservedWidth);

      if (!isOnResizeBorder) {
        isOnFrameDrag = (mouse.y <= window.top + titleBarHeight + borderThickness)
          && (mouse.x < (window.right - (controlBoxWidth + extraRightReservedWidth)))
          && (mouse.x > (window.left + iconWidth + extraLeftReservedWidth));
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

    if (row == 0 && col != 1 && mouse.y > window.top + borderThickness) {
      // Don't do top left/right resizing for the whole title bar height, just for border thickness
      row = 1;
    }

    HitTestResult[][] hitTests = {
      {TOPLEFT, isOnResizeBorder ? TOP : isOnFrameDrag ? CAPTION : NOWHERE, TOPRIGHT},
      {LEFT, NOWHERE, RIGHT},
      {BOTTOMLEFT, BOTTOM, BOTTOMRIGHT},
    };

    return hitTests[row][col];
  }
}
