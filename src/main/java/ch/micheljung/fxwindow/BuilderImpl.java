package ch.micheljung.fxwindow;

import com.sun.jna.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.IOException;
import java.net.URL;
import java.util.function.Function;

class BuilderImpl implements StageCreator {
  private final Stage stage;
  private FXMLLoader fxmlLoader;
  private boolean useNative = true;
  private Function<Parent, Scene> sceneFactory = root -> new Scene(root, 0, 0);
  private URL windowFxml;
  private boolean allowMinimize = true;
  private boolean allowTopResize = true;
  private boolean applyStyleSheet = true;
  private Window owner;
  private Modality modality;
  private Region content;

  public BuilderImpl(Stage stage) {
    this.stage = stage;
  }

  @Override
  public BuilderImpl withFxmlLoader(FXMLLoader fxmlLoader) {
    this.fxmlLoader = fxmlLoader;
    return this;
  }

  @Override
  public BuilderImpl withWindowFxml(URL windowFxml) {
    this.windowFxml = windowFxml;
    return this;
  }

//    /**
//     * Sets the node that represents the application icon in the top left corner of the application. This does
//     * <strong>not</strong> add the icon to the window but rather use its bounds for hit testing.
//     */
//    public Builder icon(Node icon) {
//      componentDimensions.setIcon(icon);
//      return this;
//    }
//
//    /**
//     * Sets the node that represents the left menu bar of the application. This does
//     * <strong>not</strong> add the menu bar to the window but rather use its bounds for hit testing.
//     */
//    public Builder leftMenuBar(Node leftMenuBar) {
//      componentDimensions.setLeftMenuBar(leftMenuBar);
//      return this;
//    }
//
//    /**
//     * Sets the node that represents the title bar of the application. This does
//     * <strong>not</strong> add the title to the window but rather use its bounds for hit testing.
//     */
//    public Builder titleBar(Node titleBar) {
//      componentDimensions.setTitleBar(titleBar);
//      return this;
//    }
//
//    /**
//     * Sets the node that represents the right menu bar of the application. This does
//     * <strong>not</strong> add the menu bar to the window but rather use its bounds for hit testing.
//     */
//    public Builder rightMenuBar(Node rightMenuBar) {
//      componentDimensions.setRightMenuBar(rightMenuBar);
//      return this;
//    }

  @Override
  public BuilderImpl useNative(boolean useNative) {
    this.useNative = useNative;
    return this;
  }

  @Override
  public BuilderImpl allowTopResize(boolean allowTopResize) {
    this.allowTopResize = allowTopResize;
    return this;
  }

  @Override
  public BuilderImpl allowMinimize(boolean allowMinimize) {
    this.allowMinimize = allowMinimize;
    return this;
  }

  @Override
  public BuilderImpl withSceneFactory(Function<Parent, Scene> sceneFactory) {
    this.sceneFactory = sceneFactory;
    return this;
  }

  @Override
  public BuilderImpl withContent(Region content) {
    this.content = content;
    return this;
  }

  @Override
  public StageConfigurer applyStyleSheet(boolean applyStyleSheet) {
    this.applyStyleSheet = applyStyleSheet;
    return this;
  }

  @Override
  public StageCreator initOwner(Window owner) {
    this.owner = owner;
    return this;
  }

  @Override
  public StageCreator initModality(Modality modality) {
    this.modality = modality;
    return this;
  }
//  /**
//   * Whether to use "blur behind". Not yet working.
//   */
//  public Builder blurBehind(boolean blurBehind) {
//    this.blurBehind = blurBehind;
//    return this;

//  }
//  /**
//   * Whether to use "blur behind". Not yet working.
//   */
//  public Builder useAcrylic(boolean useAcrylic) {
//    this.useAcrylic = useAcrylic;
//    return this;

//  }

  @Override
  public FxStage apply() {
    if (fxmlLoader == null) {
      fxmlLoader = new FXMLLoader();
    }

    WindowController controller = createController();
    controller.setAllowMinimize(allowMinimize);
    if (content != null) {
      controller.setContent(content);
    }

    Features features = new Features();
    features.setAllowTopResize(allowTopResize);
    features.setAllowMinimize(allowMinimize);
    features.setUseNative(useNative);

    if (owner != null) {
      stage.initOwner(owner);
    }
    if (modality != null) {
      stage.initModality(modality);
    }

    boolean useWindowsNative = useWindows();
    if (useWindowsNative) {
      if (stage.isShowing()) {
        WindowsCustomStage.configure(controller, features);
      } else {
        EventHandler<WindowEvent> currentOnShown = stage.getOnShown();
        stage.setOnShown(event -> {
          stage.setOnShown(currentOnShown);
          if (currentOnShown != null) {
            currentOnShown.handle(event);
          }
          WindowsCustomStage.configure(controller, features);
        });
      }
    } else {
      stage.getScene().setFill(Color.TRANSPARENT);
      stage.initStyle(StageStyle.TRANSPARENT);
      UndecoratedStage.configure(stage, controller, features);
    }

    if (applyStyleSheet) {
      stage.getScene().getStylesheets().add(FxStage.BASE_CSS.toExternalForm());
      if (!useWindowsNative) {
        stage.getScene().getStylesheets().add(FxStage.UNDECORATED_CSS.toExternalForm());
      }
    }

    return controller;
  }

  private WindowController createController() {
    Parent newRoot;
    WindowController controller;
    try {
      fxmlLoader.setLocation(getWindowFxml());
      newRoot = fxmlLoader.load();
      controller = fxmlLoader.getController();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    if (stage.getScene() != null) {
      Region currentRoot = (Region) stage.getScene().getRoot();
      controller.setContent(currentRoot, stage);
    } else {
      Scene scene = sceneFactory.apply(newRoot);
      stage.setScene(scene);
    }
    stage.getScene().setRoot(newRoot);
    return controller;
  }

  private URL getWindowFxml() {
    if (windowFxml != null) {
      return windowFxml;
    }
    if (useWindows()) {
      return getClass().getResource("/fxml/windows.fxml");
    }
    return getClass().getResource("/fxml/undecorated.fxml");
  }

  private boolean useWindows() {
    return Platform.isWindows() && useNative && (isWindows10() || isWindows11() );
  }

  private boolean isWindows10() {
    return System.getProperty("os.name").equals("Windows 10");
  }

  private boolean isWindows11() {
    return System.getProperty("os.name").equals("Windows 11");
  }
}