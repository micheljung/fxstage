package ch.micheljung.fxborderlessscene.borderless;

import javafx.application.Application;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.junit.Test;

public class BorderlessSceneTest extends Application {

  @Test
  public void test() {
    launch();
  }

  @Override
  public void start(Stage primaryStage) {
    BorderlessScene scene = new BorderlessScene(primaryStage, new Label("Test"), 100, 100);
    primaryStage.setScene(scene);
    primaryStage.show();
  }
}
