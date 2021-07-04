package ch.micheljung.fxwindow;

import javafx.stage.Stage;

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

  public Rect(Stage stage) {
    left = (int) stage.getX();
    right = (int) (stage.getX() + stage.getWidth());
    top = (int) stage.getY();
    bottom = (int) (stage.getX() + stage.getHeight());
  }
}
