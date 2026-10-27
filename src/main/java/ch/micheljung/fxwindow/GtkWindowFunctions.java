package ch.micheljung.fxwindow;

import com.sun.jna.Pointer;
import javafx.application.Platform;
import javafx.stage.Stage;

/**
 * Keeps the window manager functions of an undecorated stage's GTK window enabled. JavaFX disables maximizing and
 * minimizing for undecorated windows, and disables them again whenever the window is restored. The window manager then
 * refuses to maximize, minimize or tile the window, and since JavaFX only considers a window restored once the window
 * manager restored it, {@code stage.setMaximized(false)} wouldn't stick either.
 */
final class GtkWindowFunctions {

  private final Stage stage;
  private final Features features;

  /** Only valid while the stage is showing. */
  private Pointer gdkWindow;
  /** Referenced so that it isn't garbage-collected while it is registered. */
  private Gtk3.GdkFilterFunc windowStateFilter;

  GtkWindowFunctions(Stage stage, Features features) {
    this.stage = stage;
    this.features = features;
    stage.resizableProperty().addListener(observable -> update());
    // In case WINDOW_HIDING was consumed: the window is destroyed by now, so it must not be accessed anymore
    stage.showingProperty().addListener((observable, oldValue, showing) -> {
      if (!showing) {
        gdkWindow = null;
        windowStateFilter = null;
      }
    });
  }

  /** Starts managing the specified window, which must belong to the stage. */
  void attach(Pointer gdkWindow) {
    if (!gdkWindow.equals(this.gdkWindow)) {
      detach();
      this.gdkWindow = gdkWindow;
      // The listener is called before JavaFX processes the state change, so update after it did
      windowStateFilter = Gtk3.addPropertyChangeListener(gdkWindow, "_NET_WM_STATE", () -> Platform.runLater(this::update));
    }
    update();
  }

  /** To be called before the stage is hidden, which destroys its window. */
  void detach() {
    if (gdkWindow != null && windowStateFilter != null) {
      Gtk3.removeFilter(gdkWindow, windowStateFilter);
    }
    gdkWindow = null;
    windowStateFilter = null;
  }

  private void update() {
    if (gdkWindow == null || !stage.isShowing()) {
      return;
    }
    int functions = Gtk3.GDK_FUNC_MOVE | Gtk3.GDK_FUNC_CLOSE;
    if (stage.isResizable()) {
      functions |= Gtk3.GDK_FUNC_RESIZE | Gtk3.GDK_FUNC_MAXIMIZE;
    }
    if (features.isAllowMinimize()) {
      functions |= Gtk3.GDK_FUNC_MINIMIZE;
    }
    Gtk3.setFunctions(gdkWindow, functions);
  }
}
