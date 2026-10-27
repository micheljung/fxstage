package ch.micheljung.fxwindow;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Region;

import java.net.URL;
import java.util.function.Function;

public interface StageConfigurer {

  /** Configures the FXML loader to be used when loading the window's FXML. */
  StageConfigurer withFxmlLoader(FXMLLoader fxmlLoader);

  /**
   * Sets a function to be used to create a scene for a specified parent node. If none is given, a default factory will
   * be used.
   */
  StageConfigurer withSceneFactory(Function<Parent, Scene> sceneFactory);

  /** Use the specified FXML as the stage's root node instead of the default one. */
  StageConfigurer withWindowFxml(URL windowFxml);

  /** Sets the window content. */
  StageConfigurer withContent(Region content);

  /** Whether or not to apply the FxStage style sheet. Default is {@code true}. */
  StageConfigurer applyStyleSheet(boolean applyStyleSheet);

  /**
   * If {@code true}, native window behavior (like Aero glass, Aero Snap) will be used instead of a cross-platform
   * imitation of it. This is supported on Windows 10 and 11, and on Linux if JavaFX uses GTK 3 (the default), where
   * moving, resizing, snapping and maximizing are left to the window manager. If the native window can't be found, the
   * imitation is used. On Linux, the imitation still allows the window manager to maximize the window if JavaFX uses
   * GTK 3, as JavaFX wouldn't unmaximize it otherwise. Default is {@code true}.
   */
  StageConfigurer useNative(boolean useNative);

  /** If {@code true}, resize controls will be available at window's top. */
  StageConfigurer allowTopResize(boolean allowTopResize);

  /** If {@code true}, allow minimizing the window. */
  StageConfigurer allowMinimize(boolean allowMinimize);

  /** Applies the configuration to the stage and returns the created {@link ch.micheljung.fxwindow.FxStage}. */
  FxStage apply();
}