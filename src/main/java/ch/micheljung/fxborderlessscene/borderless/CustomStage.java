package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.net.URL;
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
    private boolean useAcrylic;
    private URL fxmlFile;

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

    /**
     * Sets the node that represents the title bar of the application. This does
     * <strong>not</strong> add the title to the window but rather use its bounds for hit testing.
     */
    public Builder titleBar(Node titleBar) {
      componentDimensions.setTitleBar(titleBar);
      return this;
    }

    /**
     * Sets the node that represents the right menu bar of the application. This does
     * <strong>not</strong> add the menu bar to the window but rather use its bounds for hit testing.
     */
    public Builder rightMenuBar(Node rightMenuBar) {
      componentDimensions.setRightMenuBar(rightMenuBar);
      return this;
    }

    /**
     * If {@code true}, native window behavior (like Aero glass, Aero Snap) will be used instead of a cross-platform
     * imitation of it. Currently, this is only supported on Windows systems.
     */
    public Builder useNative(boolean useNative) {
      this.useNative = useNative;
      return this;
    }

    public Builder alpha(boolean alpha) {
      this.alpha = alpha;
      return this;
    }

    /**
     * Whether to use "blur behind". Only supported
     */
    public Builder blurBehind(boolean blurBehind) {
      this.blurBehind = blurBehind;
      return this;
    }

    public Builder useAcrylic(boolean useAcrylic) {
      this.useAcrylic = useAcrylic;
      return this;
    }

    public void apply() {
      if (fxmlLoader == null) {
        fxmlLoader = new FXMLLoader();
      }

      Parent root;
      WindowController controller;
      try {
        fxmlLoader.setLocation(getFxmlFile());
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
      stage.setScene(scene);
//      Scene scene = new Scene(new VBox());
//      scene.setFill(Color.TRANSPARENT);
      stage.setScene(scene);

      if (useWindows()) {
        if(blurBehind){
          stage.initStyle(StageStyle.TRANSPARENT);
        }
        stage.show();
        new WindowsCustomStage(componentDimensions, alpha, blurBehind, useAcrylic);
      } else {
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.show();
        new UndecoratedStage(root, stage, componentDimensions);
      }
    }

    private URL getFxmlFile() {
      if (fxmlFile != null) {
        return fxmlFile;
      }
      if (useWindows()) {
        return CustomStage.class.getResource("/fxml/windows.fxml");
      }
      return CustomStage.class.getResource("/fxml/undecorated.fxml");
    }

    private boolean useWindows() {
      return Platform.isWindows() && useNative;
    }
  }
}
