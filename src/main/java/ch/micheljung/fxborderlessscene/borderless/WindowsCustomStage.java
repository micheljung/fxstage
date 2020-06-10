package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import java.util.function.Function;

class WindowsCustomStage extends CustomStage {

  WindowsCustomStage(Pane root, Stage stage, ComponentBinding componentBinding) {
    this(root, stage, componentBinding, r -> new Scene(r, 0, 0));
  }

  WindowsCustomStage(Pane root, Stage stage, ComponentBinding componentBinding, Function<Pane, Scene> sceneFactory) {
    if (stage.getScene() != null) {
      throw new IllegalStateException("No scene must be set");
    }

    Scene scene = sceneFactory.apply(root);
    stage.setScene(scene);
    stage.show();

    WinDef.HWND hwnd = new WinDef.HWND();
    hwnd.setPointer(User32.INSTANCE.GetActiveWindow().getPointer());
    new CustomDecorationWindowProcedure(hwnd, componentBinding);
  }
}
