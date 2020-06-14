package ch.micheljung.fxborderlessscene.borderless;

import javafx.scene.layout.StackPane;

public class CustomWindowController extends WindowController {

  public StackPane backdrop;

  @Override
  public int getShadowInset() {
    return (int) (windowRoot.getBoundsInParent().getMinX() - windowRoot.getBoundsInLocal().getMinX());
  }
}
