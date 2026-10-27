package ch.micheljung.fxwindow;

import com.sun.jna.Pointer;
import javafx.beans.InvalidationListener;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.IOException;

public class UndecoratedStage {

  public static final int DEFAULT_FRAME_DRAG_HEIGHT = 30;

  private final Stage stage;

  private final Point prevSize;
  private final Point prevPos;
  private final Point mousePosition;
  private final Rect resizeStartStageRect;
  /** Mouse position relative to the borders of the stage. */
  private final Rect resizeBorderOffset;
  /**
   * The pane, painted on top of the drop shadow, the represents the visible window. This is the stage size minus the
   * drop shadow.
   */
  private final Parent windowRoot;
  private final WindowController windowController;
  /** A popup rather than a stage, so it neither shows up in the task bar nor steals the focus. */
  private final Popup aeroSnap;
  private final Region aeroSnapContent;
  /** The stage's bounds when it was last not maximized. */
  private Rectangle2D normalBounds;

  private boolean snapped;
  /** Whether the stage was unmaximized by the current drag. */
  private boolean unmaximizedByDrag;

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

    aeroSnapContent = loader.getRoot();
    aeroSnapContent.setMouseTransparent(true);
    aeroSnap = new Popup();
    aeroSnap.getContent().add(aeroSnapContent);
    // The popup may end up under the mouse while the stage is being dragged, in which case it may receive the events.
    // Their coordinates are relative to the popup, so the handlers must only use screen coordinates.
    aeroSnap.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> forward(event, windowRoot.getOnMouseDragged()));
    aeroSnap.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> forward(event, windowRoot.getOnMouseReleased()));
    stage.focusedProperty().addListener(observable -> {
      if (!stage.isFocused()) {
        aeroSnap.hide();
      }
    });

    if (Gtk3.isAvailable()) {
      enableGtkWindowFunctions();
    }

    stage.addEventFilter(MouseEvent.MOUSE_MOVED, event -> handleMouseMovement(windowRoot, event));
    stage.maximizedProperty().addListener(observable -> {
      if (stage.isMaximized()) {
        maximize();
      } else {
        restore();
      }
    });

    InvalidationListener boundsListener = observable -> {
      if (!stage.isMaximized()) {
        normalBounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());
      }
    };
    stage.xProperty().addListener(boundsListener);
    stage.yProperty().addListener(boundsListener);
    stage.widthProperty().addListener(boundsListener);
    stage.heightProperty().addListener(boundsListener);

    mousePosition = new Point(0, 0);
    resizeStartStageRect = new Rect(0, 0, 0, 0);
    resizeBorderOffset = new Rect(0, 0, 0, 0);
    prevSize = new Point(0, 0);
    prevPos = new Point(0, 0);
  }

  public static void configure(Stage stage, WindowController controller, Features features) {
    new UndecoratedStage(stage, controller, features);
  }

  /**
   * Even when imitating native behavior, the window manager needs to be allowed to maximize the window, as JavaFX
   * wouldn't unmaximize it otherwise.
   */
  private void enableGtkWindowFunctions() {
    GtkWindowFunctions windowFunctions = new GtkWindowFunctions(stage, features);
    Runnable attach = () -> {
      Pointer gdkWindow = Gtk3.findGdkWindow(stage, null);
      if (gdkWindow != null) {
        windowFunctions.attach(gdkWindow);
      }
    };
    stage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> attach.run());
    stage.addEventHandler(WindowEvent.WINDOW_HIDING, event -> windowFunctions.detach());
    if (stage.isShowing()) {
      attach.run();
    }
  }

  private static void forward(MouseEvent event, EventHandler<? super MouseEvent> handler) {
    event.consume();
    if (handler != null) {
      handler.handle(event);
    }
  }

  private void handleMouseMovement(Parent root, MouseEvent event) {
    // No button is pressed, so no drag is in progress
    aeroSnap.hide();

    mousePosition.x = (int) event.getScreenX();
    mousePosition.y = (int) event.getScreenY();

    HitTestResult hitTestResult = FrameHitTest.hitTest(stage, windowController, event.getScreenX(), event.getScreenY(), features.isAllowTopResize());

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
          stage.getScene().setCursor(FrameHitTest.resizeCursor(hitTestResult));
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

  /** Maximize on/off the application. */
  protected void maximize() {
    Rectangle2D screen;

    if (Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth() / 2, stage.getHeight() / 2).isEmpty()) {
      screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()).get(0).getVisualBounds();
    } else {
      screen = Screen.getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth() / 2, stage.getHeight() / 2).get(0).getVisualBounds();
    }

    // Record position and size, and maximize. The stage may already have been resized by the window manager.
    if (!snapped) {
      if (normalBounds != null) {
        prevPos.x = (int) normalBounds.getMinX();
        prevPos.y = (int) normalBounds.getMinY();
        prevSize.x = (int) normalBounds.getWidth();
        prevSize.y = (int) normalBounds.getHeight();
      } else {
        rememberPreviousState();
      }
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

  /** Restores the position and size the stage had before it was maximized. */
  private void restore() {
    snapped = false;
    if (prevSize.x <= 0 || prevSize.y <= 0) {
      return;
    }
    stage.setX(prevPos.x);
    stage.setY(prevPos.y);
    stage.setWidth(prevSize.x);
    stage.setHeight(prevSize.y);
  }

  protected void handleMove(final Node node) {
    final Delta delta = new Delta();
    final Delta eventSource = new Delta();

    // Record drag deltas on press.
    node.setOnMousePressed(m -> {
      if (m.isPrimaryButtonDown()) {
        delta.x = m.getSceneX();
        delta.y = m.getSceneY();

        unmaximizedByDrag = false;
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
        // Aero Snap off. Only once, as the window manager may take a while to restore the window.
        if (stage.isMaximized() && !unmaximizedByDrag) {
          unmaximizedByDrag = true;
          stage.setMaximized(false);
        }

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

        Rectangle2D screen = Screen.getScreensForRectangle(m.getScreenX(), m.getScreenY(), 1, 1).get(0).getVisualBounds();

        // Aero Snap Left.
        if (m.getScreenX() <= screen.getMinX()) {
          showAeroSnap(screen.getMinX(), screen.getMinY(), Math.max(screen.getWidth() / 2, stage.getMinWidth()), screen.getHeight());
        }

        // Aero Snap Right.
        else if (m.getScreenX() >= screen.getMaxX() - 1) {
          double width = Math.max(screen.getWidth() / 2, stage.getMinWidth());
          showAeroSnap(screen.getMaxX() - width, screen.getMinY(), width, screen.getHeight());
        }

        // Aero Snap Top. || Aero Snap Bottom.
        else if (m.getScreenY() <= screen.getMinY() || m.getScreenY() >= screen.getMaxY() - 1) {
          showAeroSnap(screen.getMinX(), screen.getMinY(), screen.getWidth(), screen.getHeight());
        } else {
          aeroSnap.hide();
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
            stage.setMaximized(true);
          }
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      aeroSnap.hide();
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

  private void showAeroSnap(double x, double y, double width, double height) {
    aeroSnapContent.setPrefSize(width, height);
    if (aeroSnap.isShowing()) {
      aeroSnap.setX(x);
      aeroSnap.setY(y);
    } else {
      aeroSnap.show(stage, x, y);
    }
  }

  private void rememberPreviousState() {
    prevSize.x = (int) stage.getWidth();
    prevSize.y = (int) stage.getHeight();
    prevPos.x = (int) stage.getX();
    prevPos.y = (int) stage.getY();
  }

}
