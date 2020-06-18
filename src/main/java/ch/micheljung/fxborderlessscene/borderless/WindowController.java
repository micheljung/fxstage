package ch.micheljung.fxborderlessscene.borderless;

import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

public class WindowController implements FxWindow {

  private static final PseudoClass MAXIMIZED_PSEUDO_CLASS = PseudoClass.getPseudoClass("maximized");

  public Pane titleBar;
  public Pane leftMenuBar;
  public Pane rightMenuBar;
  public Node icon;
  public Pane windowRoot;
  public Region controlBox;
  public Button minimizeButton;
  public Button maximizeButton;
  public Button restoreButton;
  public Button closeButton;
  public Pane windowContent;

  public void initialize() {
    minimizeButton.managedProperty().bind(minimizeButton.visibleProperty());
    maximizeButton.managedProperty().bind(maximizeButton.visibleProperty());
    restoreButton.managedProperty().bind(restoreButton.visibleProperty());
    closeButton.managedProperty().bind(closeButton.visibleProperty());

    restoreButton.visibleProperty().bind(maximizeButton.visibleProperty().not());

    windowRoot.sceneProperty().addListener(observable ->
      windowRoot.getScene().windowProperty().addListener(observable1 -> {
        maximizeButton.visibleProperty().bind(getStage().maximizedProperty().not());
        getStage().maximizedProperty().addListener(observable2 -> getStage().getScene().getRoot().pseudoClassStateChanged(MAXIMIZED_PSEUDO_CLASS, getStage().isMaximized()));
      }));
  }

  public void onMinimizeButtonClicked() {
    getStage().setIconified(true);
  }

  public void onMaximiseButtonClicked() {
    getStage().setMaximized(true);
  }

  public void onRestoreButtonClicked() {
    getStage().setMaximized(false);
  }

  public void onCloseButtonClicked() {
    getStage().close();
  }

  private Stage getStage() {
    return (Stage) windowRoot.getScene().getWindow();
  }

  int getFrameResizeBorderThickness() {
    return 8;
  }

  int getShadowInset() {
    return 0;
  }

  @Override
  public void setContent(Node node) {
    windowContent.getChildren().setAll(node);
  }
}
