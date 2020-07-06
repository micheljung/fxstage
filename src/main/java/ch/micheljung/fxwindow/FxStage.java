package ch.micheljung.fxwindow;

import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.net.URL;

public interface FxStage {

  URL BASE_CSS = FxStage.class.getResource("/css/fxstage.css");
  URL UNDECORATED_CSS = FxStage.class.getResource("/css/fxstage-undecorated.css");

  /**
   * Nodes in the caption area of the window that should not be considered "caption".
   * <p>
   * The window's topmost pixels area considered the "caption area". If the mouse enters this area, dragging and
   * double-clicking will interact with the window rather than with the application, preventing any nodes placed in this
   * area from being interacted with.
   * </p>
   * <p>Nodes in this list are excluded from the caption area, allowing interaction.</p>
   */
  ObservableList<Node> getNonCaptionNodes();

  <T extends Region> T getTitleBar();

  <T extends Node> T getIcon();

  FxStage setContent(Region content);

  FxStage setTitleBar(Region titleBar);

  FxStage setIcon(Node icon);

  Stage getStage();

  /**
   * Returns a new builder that creates a new stage. Changes will be applied as soon as you call {@link
   * StageConfigurer#apply() apply()}.
   */
  static StageCreator create(Region content) {
    return (StageCreator) configure(new Stage()).withContent(content);
  }

  /**
   * Returns a new builder that allows to configure an existing stage. Changes will be applied as soon as you call
   * {@link StageConfigurer#apply() apply()}.
   */
  static StageConfigurer configure(Stage stage) {
    return new BuilderImpl(stage);
  }
}
