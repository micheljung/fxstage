package ch.micheljung.fxwindow;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import java.net.URL;

public interface FxStage {

  URL BASE_CSS = FxStage.class.getResource("/css/fxstage.css");
  URL UNDECORATED_CSS = FxStage.class.getResource("/css/fxstage-undecorated.css");

  <T extends Node> T getLeftMenu();

  <T extends Node> T getRightMenu();

  <T extends Region> T getTitleBar();

  <T extends Node> T getIcon();

  FxStage setContent(Region content);

  FxStage setTitleBar(Region titleBar);

  FxStage setLeftMenu(Node leftMenu);

  FxStage setRightMenu(Node rightMenu);

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
