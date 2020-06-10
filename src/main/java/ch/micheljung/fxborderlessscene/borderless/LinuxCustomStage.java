package ch.micheljung.fxborderlessscene.borderless;

import ch.micheljung.fxborderlessscene.window.TransparentWindow;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class LinuxCustomStage extends CustomStage {

  private static final String BOTTOM = "bottom";

  private static Stage stage;
  protected Delta prevSize;
  protected Delta prevPos;
  private boolean snapped;

  /** Transparent Window used to show how the window will be resized */
  private TransparentWindow transparentWindow;

  public LinuxCustomStage() {
    prevSize = new Delta();
    prevPos = new Delta();
    snapped = false;
  }

  /**
   * Creates the Transparent Window
   *
   * @param parentWindow The parentWindow of the TransparentWindow
   */
  public void createTransparentWindow(Stage parentWindow) {
    transparentWindow = new TransparentWindow();
    transparentWindow.getWindow().initOwner(parentWindow);
  }

  /** Maximize on/off the application. */
  protected void toggleMaximize() {
    Rectangle2D screen;

    if (Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth() / 2, stage.getHeight() / 2).isEmpty()) {
      screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()).get(0).getVisualBounds();
    } else {
      screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth() / 2, stage.getHeight() / 2).get(0).getVisualBounds();
    }

    if (stage.isMaximized()) {
      stage.setWidth(prevSize.x);
      stage.setHeight(prevSize.y);
      stage.setX(prevPos.x);
      stage.setY(prevPos.y);
//      maximizeButton.setVisible(true);
      stage.setMaximized(false);
    } else {
      // Record position and size, and maximize.
      if (!snapped) {
        prevSize.x = stage.getWidth();
        prevSize.y = stage.getHeight();
        prevPos.x = stage.getX();
        prevPos.y = stage.getY();
      } else if (!screen.contains(prevPos.x, prevPos.y)) {
        if (prevSize.x > screen.getWidth()) {
          prevSize.x = screen.getWidth() - 20;
        }

        if (prevSize.y > screen.getHeight()) {
          prevSize.y = screen.getHeight() - 20;
        }

        prevPos.x = screen.getMinX() + (screen.getWidth() - prevSize.x) / 2;
        prevPos.y = screen.getMinY() + (screen.getHeight() - prevSize.y) / 2;
      }

      stage.setX(screen.getMinX());
      stage.setY(screen.getMinY());
      stage.setWidth(screen.getWidth());
      stage.setHeight(screen.getHeight());

//      maximizeButton.setVisible(false);
      stage.setMaximized(true);
    }
  }

  /**
   * Minimize the application.
   */
  protected void minimize() {
    stage.setIconified(true);
  }

  /**
   * Set a node that can be pressed and dragged to move the application around.
   *
   * @param node the node.
   */
  protected void setMoveControl(final Node node) {
    final Delta delta = new Delta();
    final Delta eventSource = new Delta();

    // Record drag deltas on press.
    node.setOnMousePressed(m -> {
      if (m.isPrimaryButtonDown()) {
        delta.x = m.getSceneX(); //getX()
        delta.y = m.getSceneY(); //getY()

        if (stage.isMaximized() || snapped) {
          delta.x = prevSize.x * (m.getSceneX() / stage.getWidth());//(m.getX() / stage.getWidth())
          delta.y = prevSize.y * (m.getSceneY() / stage.getHeight());//(m.getY() / stage.getHeight())
        } else {
          prevSize.x = stage.getWidth();
          prevSize.y = stage.getHeight();
          prevPos.x = stage.getX();
          prevPos.y = stage.getY();
        }

        eventSource.x = m.getScreenX();
        eventSource.y = node.prefHeight(stage.getHeight());
      }
    });

    // Dragging moves the application around.
    node.setOnMouseDragged(m -> {
      if (m.isPrimaryButtonDown()) {

        // Move x axis.
        stage.setX(m.getScreenX() - delta.x);

        if (snapped) {

          // Aero Snap off.
          Rectangle2D screen = Screen.getScreensForRectangle(m.getScreenX(), m.getScreenY(), 1, 1).get(0).getVisualBounds();

          stage.setHeight(screen.getHeight());

          if (m.getScreenY() > eventSource.y) {
            stage.setWidth(prevSize.x);
            stage.setHeight(prevSize.y);
            snapped = false;
          }
        } else {
          // Move y axis.
          stage.setY(m.getScreenY() - delta.y);
        }

        // Aero Snap off.
        if (stage.isMaximized()) {
          stage.setWidth(prevSize.x);
          stage.setHeight(prevSize.y);
          stage.setMaximized(false);
        }

        //--------------------------Check here for Transparent Window--------------------------
        //Rectangle2D wholeScreen = Screen.getScreensForRectangle(m.getScreenX(), m.getScreenY(), 1, 1).get(0).getBounds()
        Rectangle2D screen = Screen.getScreensForRectangle(m.getScreenX(), m.getScreenY(), 1, 1).get(0).getVisualBounds();

        // Aero Snap Left.
        if (m.getScreenX() <= screen.getMinX()) {
          transparentWindow.getWindow().setY(screen.getMinY());
          transparentWindow.getWindow().setHeight(screen.getHeight());

          transparentWindow.getWindow().setX(screen.getMinX());
          transparentWindow.getWindow().setWidth(Math.max(screen.getWidth() / 2, transparentWindow.getWindow().getMinWidth()));

          transparentWindow.show();
        }

        // Aero Snap Right.
        else if (m.getScreenX() >= screen.getMaxX() - 1) {
          transparentWindow.getWindow().setY(screen.getMinY());
          transparentWindow.getWindow().setHeight(screen.getHeight());

          transparentWindow.getWindow().setWidth(Math.max(screen.getWidth() / 2, transparentWindow.getWindow().getMinWidth()));
          transparentWindow.getWindow().setX(screen.getMaxX() - transparentWindow.getWindow().getWidth());

          transparentWindow.show();
        }

        // Aero Snap Top. || Aero Snap Bottom.
        else if (m.getScreenY() <= screen.getMinY() || m.getScreenY() >= screen.getMaxY() - 1) {

          transparentWindow.getWindow().setX(screen.getMinX());
          transparentWindow.getWindow().setY(screen.getMinY());
          transparentWindow.getWindow().setWidth(screen.getWidth());
          transparentWindow.getWindow().setHeight(screen.getHeight());

          transparentWindow.show();

        } else {
          transparentWindow.close();
        }
      }
    });

    // Maximize on double click.
    node.setOnMouseClicked(m -> {
      if ((MouseButton.PRIMARY.equals(m.getButton())) && (m.getClickCount() == 2)) {
        toggleMaximize();
      }
    });

    // Aero Snap on release.
    node.setOnMouseReleased(m -> {

      try {
        if ((MouseButton.PRIMARY.equals(m.getButton())) && (m.getScreenX() != eventSource.x)) {
          Rectangle2D screen = Screen.getScreensForRectangle(m.getScreenX(), m.getScreenY(), 1, 1).get(0).getVisualBounds();

          // Aero Snap Left.
          if (m.getScreenX() <= screen.getMinX()) {
            stage.setY(screen.getMinY());
            stage.setHeight(screen.getHeight());

            stage.setX(screen.getMinX());
            stage.setWidth(Math.max(screen.getWidth() / 2, stage.getMinWidth()));

            snapped = true;
          }

          // Aero Snap Right.
          else if (m.getScreenX() >= screen.getMaxX() - 1) {
            stage.setY(screen.getMinY());
            stage.setHeight(screen.getHeight());

            stage.setWidth(Math.max(screen.getWidth() / 2, stage.getMinWidth()));
            stage.setX(screen.getMaxX() - stage.getWidth());

            snapped = true;
          }

          // Aero Snap Top ||  Aero Snap Bottom
          else if (m.getScreenY() <= screen.getMinY() || m.getScreenY() >= screen.getMaxY() - 1) {
            if (!screen.contains(prevPos.x, prevPos.y)) {
              if (prevSize.x > screen.getWidth()) {
                prevSize.x = screen.getWidth() - 20;
              }

              if (prevSize.y > screen.getHeight()) {
                prevSize.y = screen.getHeight() - 20;
              }

              prevPos.x = screen.getMinX() + (screen.getWidth() - prevSize.x) / 2;
              prevPos.y = screen.getMinY() + (screen.getHeight() - prevSize.y) / 2;
            }

            stage.setX(screen.getMinX());
            stage.setY(screen.getMinY());
            stage.setWidth(screen.getWidth());
            stage.setHeight(screen.getHeight());
            stage.setMaximized(true);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      //Hide the transparent window -- close this window no matter what
      transparentWindow.close();
    });
  }

  /**
   * Set pane to resize application when pressed and dragged.
   *
   * @param pane the pane the action is set to.
   * @param direction the resize direction. Diagonal: 'top' or 'bottom' + 'right' or 'left'.
   * [[SuppressWarningsSpartan]]
   */
  private void setResizeControl(Pane pane, final String direction) {

    //Record the previous size and previous point
    pane.setOnDragDetected((event) -> {
      prevSize.x = stage.getWidth();
      prevSize.y = stage.getHeight();
      prevPos.x = stage.getX();
      prevPos.y = stage.getY();
    });

    pane.setOnMouseDragged(m -> {
      if (m.isPrimaryButtonDown()) {
        double width = stage.getWidth();
        double height = stage.getHeight();

        // Horizontal resize.
        if (direction.endsWith("left")) {
          double comingWidth = width - m.getScreenX() + stage.getX();

          //Check if it violates minimumWidth
          if (comingWidth > 0 && comingWidth >= stage.getMinWidth()) {
            stage.setWidth(stage.getX() - m.getScreenX() + stage.getWidth());
            stage.setX(m.getScreenX());
          }

        } else if (direction.endsWith("right")) {
          double comingWidth = width + m.getX();

          //Check if it violates
          if (comingWidth > 0 && comingWidth >= stage.getMinWidth()) {
            stage.setWidth(m.getSceneX());
          }
        }

        // Vertical resize.
        if (direction.startsWith("top")) {
          if (snapped) {
            stage.setHeight(prevSize.y);
            snapped = false;
          } else if ((height > stage.getMinHeight()) || (m.getY() < 0)) {
            stage.setHeight(stage.getY() - m.getScreenY() + stage.getHeight());
            stage.setY(m.getScreenY());
          }
        } else if (direction.startsWith(BOTTOM)) {
          if (snapped) {
            stage.setY(prevPos.y);
            snapped = false;
          } else {
            double comingHeight = height + m.getY();

            //Check if it violates
            if (comingHeight > 0 && comingHeight >= stage.getMinHeight()) {
              stage.setHeight(m.getSceneY());
            }
          }

        }
      }
    });

    // Record application height and y position.
    pane.setOnMousePressed(m -> {
      if ((m.isPrimaryButtonDown()) && (!snapped)) {
        prevSize.y = stage.getHeight();
        prevPos.y = stage.getY();
      }

    });

    // Aero Snap Resize.
    pane.setOnMouseReleased(m -> {
      if ((MouseButton.PRIMARY.equals(m.getButton())) && (!snapped)) {
        Rectangle2D screen = Screen.getScreensForRectangle(m.getScreenX(), m.getScreenY(), 1, 1).get(0).getVisualBounds();

        if ((stage.getY() <= screen.getMinY()) && (direction.startsWith("top"))) {
          stage.setHeight(screen.getHeight());
          stage.setY(screen.getMinY());
          snapped = true;
        }

        if ((m.getScreenY() >= screen.getMaxY()) && (direction.startsWith(BOTTOM))) {
          stage.setHeight(screen.getHeight());
          stage.setY(screen.getMinY());
          snapped = true;
        }
      }

    });

    // Aero Snap resize on double click.
    pane.setOnMouseClicked(m -> {
      if ((MouseButton.PRIMARY.equals(m.getButton())) && (m.getClickCount() == 2) && ("top".equals(direction) || BOTTOM.equals(direction))) {
        Rectangle2D screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth() / 2, stage.getHeight() / 2).get(0).getVisualBounds();

        if (snapped) {
          stage.setHeight(prevSize.y);
          stage.setY(prevPos.y);
          snapped = false;
        } else {
          prevSize.y = stage.getHeight();
          prevPos.y = stage.getY();
          stage.setHeight(screen.getHeight());
          stage.setY(screen.getMinY());
          snapped = true;
        }
      }
    });
  }
}
