package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.platform.win32.WinDef;

public class Point {
  int x;
  int y;

  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public Point(WinDef.POINT point) {
    x = point.x;
    y = point.y;
  }
}
