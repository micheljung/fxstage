package ch.micheljung.fxwindow;

import com.sun.jna.Pointer;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.scene.Cursor;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static ch.micheljung.fxwindow.HitTestResult.CAPTION;

/**
 * Native window behavior on Linux. Moving, resizing, snapping and maximizing are left to the window manager, which
 * makes them behave like for any other window (e.g. edge tiling, its snap preview and keyboard shortcuts).
 */
@Log4j2
class LinuxCustomStage {

  private static final int PRIMARY_BUTTON = 1;

  private final Stage stage;
  private final WindowController controller;
  private final Features features;

  private final GtkWindowFunctions windowFunctions;

  /** Only valid while the stage is showing. */
  private Pointer gdkWindow;
  private Runnable removeRealizeListener;
  /** The top-level windows that were created while the stage was being shown. */
  private final List<Pointer> realizedWindows = new ArrayList<>();
  private int[] shadowWidth;
  private boolean shadowWidthUpdateScheduled;
  /** Whether the native window couldn't be found and native behavior is being imitated instead. */
  private boolean imitating;

  private boolean captionPressed;
  private double pressScreenX;
  private double pressScreenY;

  /** The resize cursor currently set on the scene, if any. */
  private Cursor resizeCursor;
  private Cursor cursorBeforeResize;

  private LinuxCustomStage(Stage stage, WindowController controller, Features features) {
    this.stage = stage;
    this.controller = controller;
    this.features = features;
    windowFunctions = new GtkWindowFunctions(stage, features);
  }

  static boolean isSupported() {
    return Gtk3.isAvailable();
  }

  /**
   * Configures the stage. If its native window can't be found when it is shown, native behavior is imitated by {@link
   * UndecoratedStage} instead.
   */
  static void configure(Stage stage, WindowController controller, Features features) {
    new LinuxCustomStage(stage, controller, features).install();
  }

  private void install() {
    stage.addEventFilter(MouseEvent.MOUSE_MOVED, this::onMouseMoved);
    stage.addEventFilter(MouseEvent.MOUSE_PRESSED, this::onMousePressed);
    stage.addEventFilter(MouseEvent.DRAG_DETECTED, this::onDragDetected);

    // JavaFX destroys the native window when the stage is hidden and creates a new one when it is shown again
    stage.addEventHandler(WindowEvent.WINDOW_SHOWING, event -> onShowing());
    stage.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> onShown());
    stage.addEventHandler(WindowEvent.WINDOW_HIDING, event -> {
      removeRealizeListener();
      windowFunctions.detach();
      gdkWindow = null;
    });
    // In case WINDOW_HIDING was consumed: the window is destroyed by now, so it must not be accessed anymore
    stage.showingProperty().addListener((observable, oldValue, showing) -> {
      if (!showing) {
        gdkWindow = null;
      }
    });

    // While being restored, JavaFX briefly considers the stage maximized again, so wait for it to settle
    ChangeListener<Object> stateListener = (observable, oldValue, newValue) -> scheduleShadowWidthUpdate();
    stage.maximizedProperty().addListener(stateListener);
    stage.fullScreenProperty().addListener(stateListener);
    // Not deferred, as the insets are set by CSS before the window is shown
    ChangeListener<Object> shadowListener = (observable, oldValue, newValue) -> updateShadowWidth();
    stage.outputScaleXProperty().addListener(shadowListener);
    if (controller.windowRoot.getParent() instanceof Region) {
      ((Region) controller.windowRoot.getParent()).insetsProperty().addListener(shadowListener);
    }

    if (stage.isShowing()) {
      onShown();
    }
  }

  private void onShowing() {
    removeRealizeListener();
    if (imitating) {
      return;
    }
    shadowWidth = null;
    realizedWindows.clear();
    // If the shadow width were set after the window has been shown, the window manager would move and resize the window
    // to keep its visible part in place. So it's set as soon as the window has been created.
    removeRealizeListener = Gtk3.addRealizeListener(window -> {
      realizedWindows.add(window);
      if (realizedWindows.size() == 1) {
        gdkWindow = window;
        updateShadowWidth();
      } else if (gdkWindow != null) {
        // Another window was shown at the same time, so it's unclear which one belongs to the stage
        Gtk3.setShadowWidth(gdkWindow, 0, 0, 0, 0);
        gdkWindow = null;
        shadowWidth = null;
      }
    });
  }

  private void removeRealizeListener() {
    if (removeRealizeListener != null) {
      removeRealizeListener.run();
      removeRealizeListener = null;
    }
  }

  private void onShown() {
    removeRealizeListener();
    if (imitating) {
      return;
    }

    Pointer realizedWindow = realizedWindows.size() == 1 ? realizedWindows.get(0) : null;
    realizedWindows.clear();
    gdkWindow = Gtk3.findGdkWindow(stage, realizedWindow);
    if (gdkWindow == null) {
      log.warn("Could not find native window of stage '{}', imitating native behavior instead", stage.getTitle());
      if (shadowWidth != null && realizedWindow != null) {
        Gtk3.setShadowWidth(realizedWindow, 0, 0, 0, 0);
      }
      imitating = true;
      UndecoratedStage.configure(stage, controller, features);
      return;
    }
    if (!gdkWindow.equals(realizedWindow)) {
      shadowWidth = null;
    }
    windowFunctions.attach(gdkWindow);
    updateShadowWidth();
  }

  private void onMouseMoved(MouseEvent event) {
    if (imitating) {
      return;
    }
    Cursor cursor = FrameHitTest.resizeCursor(hitTest(event));
    if (cursor == resizeCursor) {
      return;
    }
    Scene scene = stage.getScene();
    if (resizeCursor == null) {
      cursorBeforeResize = scene.getCursor();
    }
    scene.setCursor(cursor != null ? cursor : cursorBeforeResize);
    resizeCursor = cursor;
  }

  private void onMousePressed(MouseEvent event) {
    captionPressed = false;
    // Synthesized (e.g. touch) presses have no pointer button the window manager could track
    if (event.getButton() != MouseButton.PRIMARY || event.isSynthesized() || !isNativeWindowAvailable()) {
      return;
    }

    HitTestResult hitTestResult = hitTest(event);
    if (FrameHitTest.isResize(hitTestResult)) {
      event.consume();
      Gtk3.beginResizeDrag(gdkWindow, toGdkEdge(hitTestResult), PRIMARY_BUTTON, toGdk(event.getScreenX()), toGdk(event.getScreenY()));
    } else if (hitTestResult == CAPTION) {
      if (event.getClickCount() == 2) {
        event.consume();
        if (stage.isResizable()) {
          stage.setMaximized(!stage.isMaximized());
        }
        return;
      }
      captionPressed = true;
      pressScreenX = event.getScreenX();
      pressScreenY = event.getScreenY();
    }
  }

  private void onDragDetected(MouseEvent event) {
    if (!captionPressed || !event.isPrimaryButtonDown() || !isNativeWindowAvailable()) {
      return;
    }
    captionPressed = false;
    event.consume();
    // Start where the mouse was pressed, so that the window doesn't lag behind by the drag detection threshold
    Gtk3.beginMoveDrag(gdkWindow, PRIMARY_BUTTON, toGdk(pressScreenX), toGdk(pressScreenY));
  }

  private HitTestResult hitTest(MouseEvent event) {
    return FrameHitTest.hitTest(stage, controller, event.getScreenX(), event.getScreenY(), features.isAllowTopResize());
  }

  private void scheduleShadowWidthUpdate() {
    if (shadowWidthUpdateScheduled) {
      return;
    }
    shadowWidthUpdateScheduled = true;
    Platform.runLater(() -> {
      shadowWidthUpdateScheduled = false;
      updateShadowWidth();
    });
  }

  /**
   * Tells the window manager about the transparent area around the visible window, so that it isn't taken into account
   * when the window is maximized or snapped.
   */
  private void updateShadowWidth() {
    if (!isNativeWindowAvailable()) {
      return;
    }

    Insets insets = getShadowInsets();
    int[] width = {toGdk(insets.getLeft()), toGdk(insets.getRight()), toGdk(insets.getTop()), toGdk(insets.getBottom())};
    if (Arrays.equals(width, shadowWidth)) {
      return;
    }
    shadowWidth = width;
    Gtk3.setShadowWidth(gdkWindow, width[0], width[1], width[2], width[3]);
  }

  /**
   * The visible window is surrounded by its parent's padding, e.g. the backdrop of undecorated.fxml. This doesn't rely
   * on the layout, which is not up-to-date while the window is being resized.
   */
  private Insets getShadowInsets() {
    Parent parent = controller.windowRoot.getParent();
    if (stage.isMaximized() || stage.isFullScreen() || !(parent instanceof Region)) {
      return Insets.EMPTY;
    }
    return ((Region) parent).getInsets();
  }

  /** The native window is destroyed when the stage is hidden, so it must not be accessed afterwards. */
  private boolean isNativeWindowAvailable() {
    return gdkWindow != null && stage.isShowing();
  }

  /** Converts JavaFX coordinates to GDK coordinates, which differ if JavaFX doesn't use GDK's scale factor. */
  private int toGdk(double value) {
    return (int) Math.round(value * stage.getOutputScaleX() / Gtk3.getScaleFactor(gdkWindow));
  }

  private static int toGdkEdge(HitTestResult hitTestResult) {
    switch (hitTestResult) {
      case TOPLEFT:
        return Gtk3.GDK_WINDOW_EDGE_NORTH_WEST;
      case TOP:
        return Gtk3.GDK_WINDOW_EDGE_NORTH;
      case TOPRIGHT:
        return Gtk3.GDK_WINDOW_EDGE_NORTH_EAST;
      case LEFT:
        return Gtk3.GDK_WINDOW_EDGE_WEST;
      case RIGHT:
        return Gtk3.GDK_WINDOW_EDGE_EAST;
      case BOTTOMLEFT:
        return Gtk3.GDK_WINDOW_EDGE_SOUTH_WEST;
      case BOTTOM:
        return Gtk3.GDK_WINDOW_EDGE_SOUTH;
      case BOTTOMRIGHT:
        return Gtk3.GDK_WINDOW_EDGE_SOUTH_EAST;
      default:
        throw new IllegalArgumentException("Not a resize edge: " + hitTestResult);
    }
  }
}
