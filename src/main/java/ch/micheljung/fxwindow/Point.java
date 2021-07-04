package ch.micheljung.fxwindow;

import javafx.geometry.Point2D;
import lombok.ToString;

@ToString
public class Point {
  int x;
  int y;

  public Point(int x, int y) {
    this.x = x;
    this.y = y;
  }

  public Point(Point2D mousePosition) {
    x = (int) mousePosition.getX();
    y = (int) mousePosition.getY();
  }
}
