package ch.micheljung.fxborderlessscene.borderless;

import ch.micheljung.fxborderlessscene.hittest.point;
import ch.micheljung.fxborderlessscene.hittest.Rect;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;

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

class HitTest {

//  static HitTestResult hitTest(Rect window, point mouse, WinDef.HWND hWnd, int message, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
//    int borderThickness = parameters.getFrameResizeBorderThickness();
//
//    int row = 1;
//    int col = 1;
//    boolean isOnResizeBorder = false;
//    boolean isOnFrameDrag = false;
//
//    int topOffset = parameters.getTitleBarHeight() == 0 ? borderThickness : parameters.getTitleBarHeight();
//    if (mouse.y >= window.top && mouse.y < window.top + topOffset + borderThickness) {
//      // Top Resizing
//      isOnResizeBorder = (mouse.y < (window.top + borderThickness));
//
//      if (!isOnResizeBorder) {
//        isOnFrameDrag = (mouse.y <= window.top + parameters.getTitleBarHeight() + borderThickness)
//          && (mouse.x < (window.right - (parameters.getControlBoxWidth()
//          + parameters.getExtraRightReservedWidth())))
//          && (mouse.x > (window.left + parameters.getIconWidth()
//          + parameters.getExtraLeftReservedWidth()));
//      }
//      // Top Resizing or Caption Moving
//      row = 0;
//    } else if (mouse.y < window.bottom && mouse.y >= window.bottom - borderThickness) {
//      // Bottom Resizing
//      row = 2;
//    }
//
//    if (mouse.x >= window.left && mouse.x < window.left + borderThickness) {
//      // Left Resizing
//      col = 0;
//    } else if (mouse.x < window.right && mouse.x >= window.right - borderThickness) {
//      // Right Resizing
//      col = 2;
//    }
//
//    if (col != 1 && mouse.y > window.top + borderThickness) {
//      // Don't do top left/right resizing for the whole title bar height, just for border thickness
//      row = 1;
//    }
//
//    HitTestResult[][] hitTests = {
//      {TOPLEFT, isOnResizeBorder ? TOP : isOnFrameDrag ? CAPTION : NOWHERE, TOPRIGHT},
//      {LEFT, NOWHERE, RIGHT},
//      {BOTTOMLEFT, BOTTOM, BOTTOMRIGHT},
//    };
//
//    return hitTests[row][col];
//  }
}
