package ch.micheljung.fxborderlessscene;

import ch.micheljung.fxborderlessscene.borderless.CustomStage;
import ch.micheljung.fxborderlessscene.borderless.FxWindow;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main {
  public static void main(String[] args) {
    DemoApplication.launch(DemoApplication.class, args);
  }

  public static class DemoApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
      FxWindow fxWindow = CustomStage.configure(primaryStage)
        .useAcrylic(false)
        .blurBehind(false)
        .useNative(true)
        .apply();
    }
  }
}
