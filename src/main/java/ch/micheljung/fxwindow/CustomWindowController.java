package ch.micheljung.fxwindow;

import javafx.scene.layout.StackPane;

public class CustomWindowController extends WindowController {

  public StackPane backdrop;

  @Override
  public int getResizeBorderThickness() {
    return (int) (windowRoot.getBoundsInParent().getMinX() - windowRoot.getBoundsInLocal().getMinX());
  }
}
