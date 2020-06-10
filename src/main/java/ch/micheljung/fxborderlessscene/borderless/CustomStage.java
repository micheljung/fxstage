package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.function.Function;

public abstract class CustomStage {

  public static Builder configure(Stage stage) {
    return new Builder(stage);
  }

  public static class Builder {
    private final ComponentDimensions componentDimensions;
    private final Stage stage;
    private FXMLLoader fxmlLoader;
    private boolean useNative;
    private Function<Parent, Scene> sceneFactory = root -> new Scene(root, 0, 0);
    private boolean blurBehind;
    private boolean alpha;

    public Builder(Stage stage) {
      this.stage = stage;
      componentDimensions = new ComponentDimensions();
    }

    /** Configures the FXML loader to be used when loading the window's FXML. */
    public Builder fxmlLoader(FXMLLoader fxmlLoader) {
      this.fxmlLoader = fxmlLoader;
      return this;
    }

    /**
     * Sets the node that represents the application icon in the top left corner of the application. This does
     * <strong>not</strong> add the icon to the window but rather use its bounds for hit testing.
     */
    public Builder icon(Node icon) {
      componentDimensions.setIcon(icon);
      return this;
    }

    /**
     * Sets the node that represents the left menu bar of the application. This does
     * <strong>not</strong> add the menu bar to the window but rather use its bounds for hit testing.
     */
    public Builder leftMenuBar(Node leftMenuBar) {
      componentDimensions.setLeftMenuBar(leftMenuBar);
      return this;
    }

    public Builder titleBar(Node titleBar) {
      componentDimensions.setTitleBar(titleBar);
      return this;
    }

    public Builder rightMenuBar(Node rightMenuBar) {
      componentDimensions.setRightMenuBar(rightMenuBar);
      return this;
    }

    public Builder useNative(boolean useNative) {
      this.useNative = useNative;
      return this;
    }

    public Builder alpha(boolean alpha) {
      this.alpha = alpha;
      return this;
    }

    public Builder blurBehind(boolean blurBehind) {
      this.blurBehind = blurBehind;
      return this;
    }

    public void apply() {
      if (fxmlLoader == null) {
        fxmlLoader = new FXMLLoader();
      }

      boolean useWindows = Platform.isWindows() && this.useNative;

      String fxmlFile;
      if (useWindows) {
        fxmlFile = "/fxml/windows.fxml";
      } else {
        fxmlFile = "/fxml/undecorated.fxml";
      }

      Parent root;
      WindowController controller;
      try {
        fxmlLoader.setLocation(CustomStage.class.getResource(fxmlFile));
        root = fxmlLoader.load();
        controller = fxmlLoader.getController();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      componentDimensions.setControlBox(controller.getControlBox());

      if (stage.getScene() != null) {
        throw new IllegalStateException("No scene must be set");
      }

      Scene scene = sceneFactory.apply(root);
      scene.setFill(Color.color(0, 0, 0, 0.01));
      stage.setScene(scene);

      if (useWindows) {
        stage.initStyle(StageStyle.DECORATED);
        stage.show();
        new WindowsCustomStage(componentDimensions, alpha, blurBehind);
      } else {
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.show();
        new UndecoratedStage(root, stage, componentDimensions);
      }
    }
  }
}
