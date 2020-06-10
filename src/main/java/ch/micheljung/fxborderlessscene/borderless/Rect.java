package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.platform.win32.WinDef;

public class Rect {
  int left;
  int top;
  int right;
  int bottom;

  public Rect(int left, int top, int right, int bottom) {
    this.left = left;
    this.top = top;
    this.right = right;
    this.bottom = bottom;
  }

  public Rect(WinDef.RECT rect) {
    left = rect.left;
    right = rect.right;
    top = rect.top;
    bottom = rect.bottom;
  }
}
