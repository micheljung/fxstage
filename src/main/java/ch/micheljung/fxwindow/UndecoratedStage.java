package ch.micheljung.fxwindow;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;

import static ch.micheljung.fxwindow.HitTestResult.BOTTOM;
import static ch.micheljung.fxwindow.HitTestResult.BOTTOMLEFT;
import static ch.micheljung.fxwindow.HitTestResult.BOTTOMRIGHT;
import static ch.micheljung.fxwindow.HitTestResult.HTCLIENT;
import static ch.micheljung.fxwindow.HitTestResult.LEFT;
import static ch.micheljung.fxwindow.HitTestResult.NOWHERE;
import static ch.micheljung.fxwindow.HitTestResult.RIGHT;
import static ch.micheljung.fxwindow.HitTestResult.TOP;
import static ch.micheljung.fxwindow.HitTestResult.TOPLEFT;
import static ch.micheljung.fxwindow.HitTestResult.TOPRIGHT;

public class UndecoratedStage {

  public static final int DEFAULT_FRAME_DRAG_HEIGHT = 30;

  private final Stage stage;

  private final Point prevSize;
  private final Point prevPos;
  private final Point mousePosition;
  private final Rect resizeStartStageRect;
  /** Mouse position relative to the borders of the stage. */
  private final Rect resizeBorderOffset;
  private final Rect stageBounds;
  /**
   * The pane, painted on top of the drop shadow, the represents the visible window. This is the stage size minus the
   * drop shadow.
   */
  private final Parent windowRoot;
  private final WindowController windowController;
  private final Stage aeroSnap;

  private boolean snapped;

  private final Features features;

  public UndecoratedStage(Stage stage, WindowController windowController, Features features) {
    this.stage = stage;
    this.windowRoot = windowController.windowRoot;
    this.windowController = windowController;
    this.features = features;

    FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/aero-snap-preview.fxml"));
    try {
      loader.load();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    aeroSnap = new Stage();
    aeroSnap.initStyle(StageStyle.TRANSPARENT);
    aeroSnap.initModality(Modality.NONE);
    aeroSnap.setScene(new Scene(loader.getRoot(), Color.TRANSPARENT));
    aeroSnap.initOwner(stage);

    stage.addEventFilter(MouseEvent.MOUSE_MOVED, event -> handleMouseMovement(windowRoot, event));
    stage.maximizedProperty().addListener(observable -> {
      if (stage.isMaximized()) {
        maximize();
      }
    });

    mousePosition = new Point(0, 0);
    resizeStartStageRect = new Rect(0, 0, 0, 0);
    resizeBorderOffset = new Rect(0, 0, 0, 0);
    stageBounds = new Rect(0, 0, 0, 0);
    prevSize = new Point(0, 0);
    prevPos = new Point(0, 0);
  }

  public static void configure(Stage stage, WindowController controller, Features features) {
    new UndecoratedStage(stage, controller, features);
  }

  private void handleMouseMovement(Parent root, MouseEvent event) {
    stageBounds.left = (int) stage.getX();
    stageBounds.top = (int) stage.getY();
    stageBounds.right = (int) (stage.getX() + stage.getWidth() - 1);
    stageBounds.bottom = (int) (stage.getY() + stage.getHeight() - 1);

    mousePosition.x = (int) event.getScreenX();
    mousePosition.y = (int) event.getScreenY();

    HitTestResult hitTestResult = hitTest(stageBounds, mousePosition, windowController, features.isAllowTopResize());

    switch (hitTestResult) {
      case TOPLEFT:
      case TOP:
      case TOPRIGHT:
      case LEFT:
      case RIGHT:
      case BOTTOMLEFT:
      case BOTTOM:
      case BOTTOMRIGHT:
        if (!stage.isMaximized()) {
          stage.getScene().setCursor(getCursor(hitTestResult));
          handleResize(root, hitTestResult);
        }
        break;
      case CAPTION:
        stage.getScene().setCursor(Cursor.DEFAULT);
        handleMove(root);
        break;
      default:
        stage.getScene().setCursor(Cursor.DEFAULT);
        root.setOnDragDetected(null);
        root.setOnMouseDragged(null);
        root.setOnMousePressed(null);
        root.setOnMouseReleased(null);
        root.setOnMouseClicked(null);
    }
  }

  private Cursor getCursor(HitTestResult hitTestResult) {
    switch (hitTestResult) {
      case TOPLEFT:
        return Cursor.NW_RESIZE;
      case TOP:
      case BOTTOM:
        return Cursor.V_RESIZE;
      case TOPRIGHT:
        return Cursor.NE_RESIZE;
      case LEFT:
      case RIGHT:
        return Cursor.H_RESIZE;
      case BOTTOMLEFT:
        return Cursor.SW_RESIZE;
      case BOTTOMRIGHT:
        return Cursor.SE_RESIZE;
      default:
        return Cursor.DEFAULT;
    }
  }

  /** Maximize on/off the application. */
  protected void maximize() {
    Rectangle2D screen;

    if (Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth() / 2, stage.getHeight() / 2).isEmpty()) {
      screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()).get(0).getVisualBounds();
    } else {
      screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth() / 2, stage.getHeight() / 2).get(0).getVisualBounds();
    }

    // Record position and size, and maximize.
    if (!snapped) {
      rememberPreviousState();
    } else if (!screen.contains(prevPos.x, prevPos.y)) {
      if (prevSize.x > screen.getWidth()) {
        prevSize.x = (int) (screen.getWidth() - 20);
      }

      if (prevSize.y > screen.getHeight()) {
        prevSize.y = (int) (screen.getHeight() - 20);
      }

      prevPos.x = (int) (screen.getMinX() + (screen.getWidth() - prevSize.x) / 2);
      prevPos.y = (int) (screen.getMinY() + (screen.getHeight() - prevSize.y) / 2);
    }

    stage.setX(screen.getMinX());
    stage.setY(screen.getMinY());
    stage.setWidth(screen.getWidth());
    stage.setHeight(screen.getHeight());
  }

  protected void handleMove(final Node node) {
    final Delta delta = new Delta();
    final Delta eventSource = new Delta();

    // Record drag deltas on press.
    node.setOnMousePressed(m -> {
      if (m.isPrimaryButtonDown()) {
        delta.x = m.getSceneX();
        delta.y = m.getSceneY();

        if (stage.isMaximized() || snapped) {
          delta.x = prevSize.x * (m.getSceneX() / stage.getWidth());
          delta.y = prevSize.y * (m.getSceneY() / stage.getHeight());
        } else {
          rememberPreviousState();
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

        Rectangle2D screen = Screen.getScreensForRectangle(m.getScreenX(), m.getScreenY(), 1, 1).get(0).getVisualBounds();

        // Aero Snap Left.
        if (m.getScreenX() <= screen.getMinX()) {
          aeroSnap.setY(screen.getMinY());
          aeroSnap.setHeight(screen.getHeight());

          aeroSnap.setX(screen.getMinX());
          aeroSnap.setWidth(Math.max(screen.getWidth() / 2, aeroSnap.getMinWidth()));

          aeroSnap.show();
        }

        // Aero Snap Right.
        else if (m.getScreenX() >= screen.getMaxX() - 1) {
          aeroSnap.setY(screen.getMinY());
          aeroSnap.setHeight(screen.getHeight());

          aeroSnap.setWidth(Math.max(screen.getWidth() / 2, aeroSnap.getMinWidth()));
          aeroSnap.setX(screen.getMaxX() - aeroSnap.getWidth());

          aeroSnap.show();
        }

        // Aero Snap Top. || Aero Snap Bottom.
        else if (m.getScreenY() <= screen.getMinY() || m.getScreenY() >= screen.getMaxY() - 1) {

          aeroSnap.setX(screen.getMinX());
          aeroSnap.setY(screen.getMinY());
          aeroSnap.setWidth(screen.getWidth());
          aeroSnap.setHeight(screen.getHeight());

          aeroSnap.show();

        } else {
          aeroSnap.close();
        }
      }
    });

    // Maximize/restore on double click.
    node.setOnMouseClicked(m -> {
      if ((MouseButton.PRIMARY.equals(m.getButton())) && (m.getClickCount() == 2)) {
        stage.setMaximized(!stage.isMaximized());
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

          // Aero Snap Top & Bottom
          else if (m.getScreenY() <= screen.getMinY() || m.getScreenY() >= screen.getMaxY() - 1) {
            if (!screen.contains(prevPos.x, prevPos.y)) {
              if (prevSize.x > screen.getWidth()) {
                prevSize.x = (int) (screen.getWidth() - 20);
              }

              if (prevSize.y > screen.getHeight()) {
                prevSize.y = (int) (screen.getHeight() - 20);
              }

              prevPos.x = (int) (screen.getMinX() + (screen.getWidth() - prevSize.x) / 2);
              prevPos.y = (int) (screen.getMinY() + (screen.getHeight() - prevSize.y) / 2);
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

      aeroSnap.close();
    });
  }

  /**
   * Set pane to resize application when pressed and dragged.
   */
  private void handleResize(Parent root, HitTestResult hitTestResult) {
    if (stage.isMaximized()) {
      return;
    }

    resizeBorderOffset.left = (int) (mousePosition.x - stage.getX());
    resizeBorderOffset.right = (int) (mousePosition.x - stage.getX() - stage.getWidth());
    resizeBorderOffset.top = (int) (mousePosition.y - stage.getY());
    resizeBorderOffset.bottom = (int) (mousePosition.y - stage.getY() - stage.getHeight());

    resizeStartStageRect.left = (int) stage.getX();
    resizeStartStageRect.right = (int) (resizeStartStageRect.left + stage.getWidth());
    resizeStartStageRect.top = (int) stage.getY();
    resizeStartStageRect.bottom = (int) (resizeStartStageRect.top + stage.getHeight());

    root.setOnDragDetected((event) -> rememberPreviousState());
    root.setOnMouseDragged(m -> {
      if (m.isPrimaryButtonDown()) {
        double height = stage.getHeight();
        double width = stage.getWidth();
        switch (hitTestResult) {
          case TOPLEFT:
          case LEFT:
          case BOTTOMLEFT:
            if ((width > stage.getMinWidth()) || (m.getX() < 0)) {
              stage.setWidth(stage.getX() - m.getScreenX() + stage.getWidth() + resizeBorderOffset.left);
              stage.setX(m.getScreenX() - resizeBorderOffset.left);
            }
            break;

          case TOPRIGHT:
          case RIGHT:
          case BOTTOMRIGHT:
            double newWidth = width + m.getX();
            if (newWidth > 0 && newWidth >= stage.getMinWidth()) {
              stage.setWidth(m.getSceneX() - resizeBorderOffset.right);
            }
            break;

          case SYSMENU:
            break;
        }

        switch (hitTestResult) {
          case TOP:
          case TOPLEFT:
          case TOPRIGHT:
            if (snapped) {
              stage.setHeight(prevSize.y);
              snapped = false;
            } else if ((height > stage.getMinHeight()) || (m.getY() < 0)) {
              stage.setHeight(stage.getY() - m.getScreenY() + stage.getHeight() + resizeBorderOffset.top);
              stage.setY(m.getScreenY() - resizeBorderOffset.top);
            }
            break;

          case BOTTOM:
          case BOTTOMLEFT:
          case BOTTOMRIGHT:
            if (snapped) {
              stage.setY(prevPos.y);
              snapped = false;
            } else {
              double newHeight = height + m.getY();
              if (newHeight > 0 && newHeight >= stage.getMinHeight()) {
                stage.setHeight(m.getSceneY() - resizeBorderOffset.bottom);
              }
            }
            break;

          case SYSMENU:
            break;
        }
      }
    });

    // Record application height and y position.
    root.setOnMousePressed(m -> {
      if ((m.isPrimaryButtonDown()) && (!snapped)) {
        prevSize.y = (int) stage.getHeight();
        prevPos.y = (int) stage.getY();
      }

    });

    // Aero Snap Resize.
    root.setOnMouseReleased(m -> {
      if ((!MouseButton.PRIMARY.equals(m.getButton())) || (snapped)) {
        return;
      }

      Rectangle2D screen = Screen.getScreensForRectangle(m.getScreenX(), m.getScreenY(), 1, 1).get(0).getVisualBounds();

      if ((stage.getY() <= screen.getMinY()) && isTop(hitTestResult)) {
        stage.setHeight(screen.getHeight());
        stage.setY(screen.getMinY());
        snapped = true;
      }

      if ((m.getScreenY() >= screen.getMaxY()) && isBottom(hitTestResult)) {
        stage.setHeight(screen.getHeight());
        stage.setY(screen.getMinY());
        snapped = true;
      }
    });

    // Aero Snap resize on double click.
    root.setOnMouseClicked(m -> {
      if ((MouseButton.PRIMARY.equals(m.getButton())) && (m.getClickCount() == 2) && (isTop(hitTestResult) || isBottom(hitTestResult))) {
        Rectangle2D screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth() / 2, stage.getHeight() / 2).get(0).getVisualBounds();

        if (snapped) {
          stage.setHeight(prevSize.y);
          stage.setY(prevPos.y);
          snapped = false;
        } else {
          prevSize.y = (int) stage.getHeight();
          prevPos.y = (int) stage.getY();
          stage.setHeight(screen.getHeight());
          stage.setY(screen.getMinY());
          snapped = true;
        }
      }
    });
  }

  private boolean isBottom(HitTestResult hitTestResult) {
    return hitTestResult == HitTestResult.BOTTOM
      || hitTestResult == HitTestResult.BOTTOMLEFT
      || hitTestResult == HitTestResult.BOTTOMRIGHT;
  }

  private boolean isTop(HitTestResult hitTestResult) {
    return hitTestResult == HitTestResult.TOP
      || hitTestResult == HitTestResult.TOPLEFT
      || hitTestResult == HitTestResult.TOPRIGHT;
  }

  private void rememberPreviousState() {
    prevSize.x = (int) stage.getWidth();
    prevSize.y = (int) stage.getHeight();
    prevPos.x = (int) stage.getX();
    prevPos.y = (int) stage.getY();
  }


  static HitTestResult hitTest(Rect window, Point mouse, WindowController controller, boolean allowTopResize) {
    Region controlBox = controller.controlBox;
    Region titleBar = controller.getTitleBar();

    Node icon = controller.getIcon();

    int top = window.top;
    int left = window.left;
    int right = window.right;
    int bottom = window.bottom;
    double frameDragHeight = titleBar != null
      ? titleBar.getHeight() : controlBox != null
      ? controlBox.getHeight() : DEFAULT_FRAME_DRAG_HEIGHT;

    HitTestResult result = HitTestResult.HTCLIENT;

    HitTestResult vertical = NOWHERE;
    int resizeBorderThickness = controller.getResizeBorderThickness();
    if (mouse.y > top + resizeBorderThickness && mouse.y <= top + 2 * resizeBorderThickness) {
      result = vertical = TOP;
    } else if (mouse.y >= bottom - resizeBorderThickness) {
      result = vertical = BOTTOM;
    }

    HitTestResult horizontal = NOWHERE;
    if (mouse.y >= top + resizeBorderThickness) {
      if (mouse.x <= left + resizeBorderThickness) {
        result = horizontal = LEFT;
      } else if (mouse.x >= right - resizeBorderThickness) {
        result = horizontal = RIGHT;
      }
    }

    if (vertical == TOP) {
      if (horizontal == LEFT) {
        result = TOPLEFT;
      } else if (horizontal == RIGHT) {
        result = TOPRIGHT;
      }
    }

    if (vertical == BOTTOM) {
      if (horizontal == LEFT) {
        result = BOTTOMLEFT;
      } else if (horizontal == RIGHT) {
        result = BOTTOMRIGHT;
      }
    }

    if ((result == HTCLIENT || (result == TOP && !allowTopResize))
      && mouse.y >= top + resizeBorderThickness
      && mouse.y <= top + resizeBorderThickness + frameDragHeight) {
      result = HitTestResult.CAPTION;
    }

    if (result != HitTestResult.HTCLIENT && (
      isMouseOn(mouse, controlBox)
        || isMouseOn(mouse, icon)
        || controller.getNonCaptionNodes().stream().anyMatch(node -> isMouseOn(mouse, node)))) {
      return HitTestResult.HTCLIENT;
    }

    return result;
  }

  private static boolean isMouseOn(Point mouse, Node node) {
    if (node == null || !node.isManaged()) {
      return false;
    }
    Bounds bounds = node.localToScreen(node.getBoundsInLocal());
    return bounds != null && bounds.contains(mouse.x, mouse.y);
  }
}
