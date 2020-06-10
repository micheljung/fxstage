package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.io.IOException;

public abstract class CustomStage {

  public static Builder configure(Stage stage) {
    return new Builder(stage);
  }

  public static class Builder {
    private final ComponentBinding componentBinding;
    private final Stage stage;
    private FXMLLoader fxmlLoader;

    public Builder(Stage stage) {
      this.stage = stage;
      componentBinding = new ComponentBinding();
    }

    public Builder fxmlLoader(FXMLLoader fxmlLoader) {
      this.fxmlLoader = fxmlLoader;
      return this;
    }

    public Builder icon(Node icon) {
      componentBinding.setIcon(icon);
      return this;
    }

    public Builder leftMenuBar(Node leftMenuBar) {
      componentBinding.setLeftMenuBar(leftMenuBar);
      return this;
    }

    public Builder titleBar(Node titleBar) {
      componentBinding.setTitleBar(titleBar);
      return this;
    }

    public Builder rightMenuBar(Node rightMenuBar) {
      componentBinding.setRightMenuBar(rightMenuBar);
      return this;
    }

    public void apply() {
      if (fxmlLoader == null) {
        fxmlLoader = new FXMLLoader();
      }

      Pane root;
      WindowController controller;
      try {
        fxmlLoader.setLocation(CustomStage.class.getResource("/fxml/windows-window.fxml"));
        root = fxmlLoader.load();
        controller = fxmlLoader.getController();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      componentBinding.setControlBox(controller.getControlBox());

      if (Platform.isWindows()) {
        new WindowsCustomStage(root, stage, componentBinding);
      } else {
//        new LinuxCustomStage(root, stage);
      }
    }
  }
}
