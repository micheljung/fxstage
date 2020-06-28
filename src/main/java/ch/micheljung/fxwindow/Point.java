package ch.micheljung.fxwindow;

import com.sun.jna.platform.win32.WinDef;
import lombok.ToString;

@ToString
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
