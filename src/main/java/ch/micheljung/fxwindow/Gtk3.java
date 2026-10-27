package ch.micheljung.fxwindow;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Bindings to the GTK 3 libraries that JavaFX's glass toolkit uses on Linux. Since glass runs GTK's main loop on the
 * JavaFX application thread, these must only be called from there.
 */
@Log4j2
final class Gtk3 {

  /** See <a href="https://docs.gtk.org/gdk3/enum.WindowEdge.html">GdkWindowEdge</a>. */
  static final int GDK_WINDOW_EDGE_NORTH_WEST = 0;
  static final int GDK_WINDOW_EDGE_NORTH = 1;
  static final int GDK_WINDOW_EDGE_NORTH_EAST = 2;
  static final int GDK_WINDOW_EDGE_WEST = 3;
  static final int GDK_WINDOW_EDGE_EAST = 4;
  static final int GDK_WINDOW_EDGE_SOUTH_WEST = 5;
  static final int GDK_WINDOW_EDGE_SOUTH = 6;
  static final int GDK_WINDOW_EDGE_SOUTH_EAST = 7;

  /** See <a href="https://docs.gtk.org/gdk3/flags.WMFunction.html">GdkWMFunction</a>. */
  static final int GDK_FUNC_RESIZE = 1 << 1;
  static final int GDK_FUNC_MOVE = 1 << 2;
  static final int GDK_FUNC_MINIMIZE = 1 << 3;
  static final int GDK_FUNC_MAXIMIZE = 1 << 4;
  static final int GDK_FUNC_CLOSE = 1 << 5;

  static final int GDK_CURRENT_TIME = 0;

  /** X11 event type, see X.h. */
  private static final int PROPERTY_NOTIFY = 28;
  /** See <a href="https://docs.gtk.org/gdk3/enum.FilterReturn.html">GdkFilterReturn</a>. */
  private static final int GDK_FILTER_CONTINUE = 0;
  /** See <a href="https://docs.gtk.org/gtk3/enum.WindowType.html">GtkWindowType</a>. */
  private static final int GTK_WINDOW_TOPLEVEL = 0;

  private static Boolean available;

  private Gtk3() {
  }

  interface Gdk extends Library {
    Pointer gdk_display_get_default();

    Pointer gdk_x11_window_lookup_for_display(Pointer display, NativeLong window);

    Pointer gdk_window_get_toplevel(Pointer window);

    int gdk_window_get_scale_factor(Pointer window);

    void gdk_window_begin_move_drag(Pointer window, int button, int rootX, int rootY, int timestamp);

    void gdk_window_begin_resize_drag(Pointer window, int edge, int button, int rootX, int rootY, int timestamp);

    void gdk_window_set_shadow_width(Pointer window, int left, int right, int top, int bottom);

    void gdk_window_set_functions(Pointer window, int functions);

    void gdk_window_add_filter(Pointer window, GdkFilterFunc function, Pointer data);

    void gdk_window_remove_filter(Pointer window, GdkFilterFunc function, Pointer data);

    NativeLong gdk_x11_get_xatom_by_name_for_display(Pointer display, String atomName);
  }

  interface GdkFilterFunc extends Callback {
    int callback(Pointer xEvent, Pointer gdkEvent, Pointer data);
  }

  interface Gtk extends Library {
    NativeLong gtk_widget_get_type();

    int gtk_window_get_window_type(Pointer window);

    Pointer gtk_window_list_toplevels();

    String gtk_window_get_title(Pointer window);

    boolean gtk_widget_get_visible(Pointer widget);

    Pointer gtk_widget_get_window(Pointer widget);
  }

  interface GLib extends Library {
    void g_list_free(Pointer list);
  }

  interface GObject extends Library {
    String g_type_name_from_instance(Pointer instance);

    int g_signal_lookup(String name, NativeLong instanceType);

    NativeLong g_signal_add_emission_hook(int signalId, int detail, GSignalEmissionHook hook, Pointer data, Pointer dataDestroy);

    void g_signal_remove_emission_hook(int signalId, NativeLong hookId);

    Pointer g_value_get_object(Pointer value);
  }

  interface GSignalEmissionHook extends Callback {
    boolean callback(Pointer invocationHint, int paramCount, Pointer params, Pointer data);
  }

  /** Only to be accessed after {@link #isAvailable()} returned {@code true}, as loading them may crash otherwise. */
  private static final class Libraries {
    private static final Map<String, Object> OPTIONS = Map.of(Library.OPTION_STRING_ENCODING, "UTF-8");

    static final Gdk GDK = Native.load("libgdk-3.so.0", Gdk.class, OPTIONS);
    static final Gtk GTK = Native.load("libgtk-3.so.0", Gtk.class, OPTIONS);
    static final GLib GLIB = Native.load("libglib-2.0.so.0", GLib.class, OPTIONS);
    static final GObject GOBJECT = Native.load("libgobject-2.0.so.0", GObject.class, OPTIONS);
  }

  /**
   * Returns {@code true} if JavaFX uses GTK 3 in this process. Loading GTK 3 into a process that uses GTK 2 (e.g. with
   * {@code -Djdk.gtk.version=2}) would crash, so this checks the libraries that are already loaded.
   */
  static synchronized boolean isAvailable() {
    if (available == null) {
      available = Platform.isLinux() && isLoaded("libgtk-3.so") && canLoadLibraries();
    }
    return available;
  }

  private static boolean isLoaded(String library) {
    try (Stream<String> lines = Files.lines(Paths.get("/proc/self/maps"))) {
      return lines.anyMatch(line -> line.contains(library));
    } catch (IOException | RuntimeException e) {
      log.debug("Could not read loaded libraries", e);
      return false;
    }
  }

  private static boolean canLoadLibraries() {
    try {
      Objects.requireNonNull(Libraries.GDK);
      return true;
    } catch (LinkageError e) {
      log.debug("Could not load GTK 3 libraries", e);
      return false;
    }
  }

  /**
   * Returns the GdkWindow of the specified stage, or {@code null} if it can't be determined. The returned pointer is
   * only valid as long as the stage is showing.
   *
   * @param candidate the only window that was created while the stage was being shown, if known
   */
  static Pointer findGdkWindow(Stage stage, Pointer candidate) {
    Pointer gdkWindow = findGdkWindowByXid(stage);
    if (gdkWindow == null) {
      gdkWindow = candidate;
    }
    if (gdkWindow == null) {
      gdkWindow = findGdkWindowByTitle(stage.getTitle());
    }
    return gdkWindow == null ? null : Libraries.GDK.gdk_window_get_toplevel(gdkWindow);
  }

  private static boolean isX11() {
    Pointer display = Libraries.GDK.gdk_display_get_default();
    return display != null && "GdkX11Display".equals(Libraries.GOBJECT.g_type_name_from_instance(display));
  }

  /**
   * Gets the window's X11 ID from JavaFX internals, which works if JavaFX is on the class path or if the internal
   * packages are exported/opened to this library (see README).
   */
  private static Pointer findGdkWindowByXid(Stage stage) {
    long xid;
    try {
      Object peer = Class.forName("com.sun.javafx.stage.WindowHelper").getMethod("getPeer", Window.class).invoke(null, stage);
      Method getPlatformWindow = findMethod(peer.getClass(), "getPlatformWindow");
      if (getPlatformWindow == null) {
        return null;
      }
      getPlatformWindow.setAccessible(true);
      Object platformWindow = getPlatformWindow.invoke(peer);
      xid = (long) Class.forName("com.sun.glass.ui.Window").getMethod("getNativeWindow").invoke(platformWindow);
    } catch (ReflectiveOperationException | RuntimeException e) {
      log.debug("Could not get native window of stage '{}' from JavaFX", stage.getTitle(), e);
      return null;
    }

    if (xid == 0 || !isX11()) {
      return null;
    }
    return Libraries.GDK.gdk_x11_window_lookup_for_display(Libraries.GDK.gdk_display_get_default(), new NativeLong(xid));
  }

  private static Method findMethod(Class<?> type, String name) {
    for (Class<?> current = type; current != null; current = current.getSuperclass()) {
      for (Method method : current.getDeclaredMethods()) {
        if (method.getName().equals(name) && method.getParameterCount() == 0) {
          return method;
        }
      }
    }
    return null;
  }

  /** Returns the GdkWindow of the only visible top-level GTK window with the specified title, if there is one. */
  private static Pointer findGdkWindowByTitle(String title) {
    Pointer match = null;
    Pointer list = Libraries.GTK.gtk_window_list_toplevels();
    try {
      // GList: { gpointer data; GList *next; GList *prev; }
      for (Pointer node = list; node != null; node = node.getPointer(Native.POINTER_SIZE)) {
        Pointer widget = node.getPointer(0);
        if (!Libraries.GTK.gtk_widget_get_visible(widget)
          || Libraries.GTK.gtk_window_get_window_type(widget) != GTK_WINDOW_TOPLEVEL
          || !Objects.equals(title, Libraries.GTK.gtk_window_get_title(widget))) {
          continue;
        }
        if (match != null) {
          log.debug("There are multiple windows with title '{}'", title);
          return null;
        }
        match = widget;
      }
    } finally {
      if (list != null) {
        Libraries.GLIB.g_list_free(list);
      }
    }
    return match == null ? null : Libraries.GTK.gtk_widget_get_window(match);
  }

  static int getScaleFactor(Pointer gdkWindow) {
    return Libraries.GDK.gdk_window_get_scale_factor(gdkWindow);
  }

  static void beginMoveDrag(Pointer gdkWindow, int button, int rootX, int rootY) {
    Libraries.GDK.gdk_window_begin_move_drag(gdkWindow, button, rootX, rootY, GDK_CURRENT_TIME);
  }

  static void beginResizeDrag(Pointer gdkWindow, int edge, int button, int rootX, int rootY) {
    Libraries.GDK.gdk_window_begin_resize_drag(gdkWindow, edge, button, rootX, rootY, GDK_CURRENT_TIME);
  }

  /**
   * Calls the listener whenever the X11 property with the specified name of the specified window changed, before GDK
   * processes the change. The returned filter must be referenced until it is removed using {@link #removeFilter}, as the
   * native code would otherwise call a garbage-collected callback. Returns {@code null} and does nothing if GDK doesn't
   * use X11.
   */
  static GdkFilterFunc addPropertyChangeListener(Pointer gdkWindow, String propertyName, Runnable listener) {
    if (!isX11()) {
      return null;
    }
    NativeLong atom = Libraries.GDK.gdk_x11_get_xatom_by_name_for_display(Libraries.GDK.gdk_display_get_default(), propertyName);
    // XPropertyEvent: { int type; unsigned long serial; Bool send_event; Display *display; Window window; Atom atom; ... }
    long displayOffset = align(2L * Native.LONG_SIZE + 4, Native.POINTER_SIZE);
    long atomOffset = displayOffset + Native.POINTER_SIZE + Native.LONG_SIZE;
    GdkFilterFunc filter = (xEvent, gdkEvent, data) -> {
      if (xEvent.getInt(0) == PROPERTY_NOTIFY && xEvent.getNativeLong(atomOffset).equals(atom)) {
        listener.run();
      }
      return GDK_FILTER_CONTINUE;
    };
    Libraries.GDK.gdk_window_add_filter(gdkWindow, filter, null);
    return filter;
  }

  static void removeFilter(Pointer gdkWindow, GdkFilterFunc filter) {
    Libraries.GDK.gdk_window_remove_filter(gdkWindow, filter, null);
  }

  private static long align(long offset, int alignment) {
    return (offset + alignment - 1) / alignment * alignment;
  }

  /**
   * Calls the listener with the GdkWindow of every top-level GTK window that is realized, which happens before it is
   * shown, until the returned runnable is run.
   */
  static Runnable addRealizeListener(Consumer<Pointer> listener) {
    int signalId = Libraries.GOBJECT.g_signal_lookup("realize", Libraries.GTK.gtk_widget_get_type());
    // Emission hooks of "realize" are called after the window has been created
    GSignalEmissionHook hook = (invocationHint, paramCount, params, data) -> {
      Pointer widget = Libraries.GOBJECT.g_value_get_object(params);
      if ("GtkWindow".equals(Libraries.GOBJECT.g_type_name_from_instance(widget))
        && Libraries.GTK.gtk_window_get_window_type(widget) == GTK_WINDOW_TOPLEVEL) {
        Pointer gdkWindow = Libraries.GTK.gtk_widget_get_window(widget);
        if (gdkWindow != null) {
          listener.accept(gdkWindow);
        }
      }
      return true;
    };
    NativeLong hookId = Libraries.GOBJECT.g_signal_add_emission_hook(signalId, 0, hook, null, null);
    return () -> {
      Libraries.GOBJECT.g_signal_remove_emission_hook(signalId, hookId);
      // Keeps the hook from being garbage-collected while it is registered
      Reference.reachabilityFence(hook);
    };
  }

  /** Tells the window manager which operations the window supports, see {@link #GDK_FUNC_RESIZE} etc. */
  static void setFunctions(Pointer gdkWindow, int functions) {
    Libraries.GDK.gdk_window_set_functions(gdkWindow, functions);
  }

  /** Tells the window manager which part of the window is shadow, so that it is excluded when snapping/maximizing. */
  static void setShadowWidth(Pointer gdkWindow, int left, int right, int top, int bottom) {
    Libraries.GDK.gdk_window_set_shadow_width(gdkWindow, left, right, top, bottom);
  }
}
