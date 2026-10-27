# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

FxStage is a small Java 11 library (published to Maven Central as `io.github.micheljung:fxstage`) that turns a JavaFX `Stage` into a borderless window. On Windows 10/11 it keeps native behaviour (drop shadow, resizing, Aero Snap) by hooking the Win32 window procedure via JNA. On Linux it lets the window manager move, resize and maximize the window through GDK 3 via JNA. On every other platform it falls back to a cross-platform imitation, which is known to be buggy. The README says the API isn't polished yet and may change.

## Commands

Gradle 7.3.2 wrapper (`./gradlew`). CI (`.github/workflows/build.yml`) runs `./gradlew build` on JDK 11.

- Build everything: `./gradlew build`
- Build only the library: `./gradlew :build`
- Run the demo app: `./gradlew :demo:run`
- Override the JavaFX native classifier: `-PjavaFxPlatform=win|linux|mac`. The default `unspecified` is auto-detected from the host OS in the root `build.gradle`.

There are no tests in the repo.

Releases are made by pushing a git tag. `publish.yml` uses the tag name as `-Pversion` and runs `publishToSonatype closeAndReleaseSonatypeStagingRepository`, signing with GPG. `version=unspecified` in `gradle.properties` is just a placeholder.

## Modules

- `:` (root): the library, in package `ch.micheljung.fxwindow`. JavaFX is `compileOnly`, so consumers supply their own JavaFX. JNA and log4j-api are `api` dependencies. Lombok is a `compileOnly` + `annotationProcessor` dependency (`@Data`, `@Log4j2`).
- `demo`: sample app (`ch.micheljung.fxborderlessscene.Main`) that uses the external `jfx-waitomo-theme`. It runs on `demoJavaFxVersion` (17), because JavaFX 11 crashes at startup on Wayland. The library itself still compiles against `javaFxVersion` (11).
- `playground`: standalone Win32/JNA experiments. It is not wired to the library and uses the old `org.openjfx.javafxplugin`.

## Architecture

**Entry point and builder.** `FxStage.configure(stage)` / `FxStage.create(content)` return `BuilderImpl`, which implements both `StageConfigurer` and `StageCreator`. `apply()`:
1. Loads the window FXML with `WindowController` as its controller. The FXML is `windows.fxml` on native Windows and `undecorated.fxml` otherwise, unless `withWindowFxml` overrides it.
2. Moves the stage's existing scene root (or the given content) into `windowContent`, then makes the FXML root the scene root.
3. Picks a backend:
   - `useWindows()` is true only if `Platform.isWindows() && useNative && os.name` is exactly `"Windows 10"` or `"Windows 11"`.
   - `useLinux()` is true if `Platform.isLinux() && useNative` and GTK 3 is already loaded by JavaFX (`/proc/self/maps`). Loading GTK 3 into a GTK 2 process would crash.
   - Otherwise `UndecoratedStage`. The Linux backend also falls back to it if it can't find the native window.
4. Adds `fxstage.css`, plus `fxstage-undecorated.css` for the non-native backend.

`WindowController` is the `FxStage` instance returned to callers. It holds the FXML-injected nodes (`controlBox`, `windowRoot`, `windowContent`, buttons) plus the user-provided title bar, icon and `nonCaptionNodes`.

**Native Windows backend.**
- `WindowsCustomStage.configure` runs once the stage is showing; if it isn't showing yet, it is deferred by wrapping `onShown`. It gets the HWND through `GetActiveWindow()` and replaces `GWL_WNDPROC` with a `DecorationWindowProcedure`.
- The procedure handles:
  - `WM_NCCALCSIZE`: removes the native frame while keeping the resize border insets, with extra top inset when maximized.
  - `WM_NCHITTEST`: maps the mouse position to caption, resize edge, or client area.
  - Every other message goes to the original window procedure through `CallWindowProc`.
- `DecorationWindowProcedure` instances are kept in a static list so JNA callbacks aren't garbage-collected. Don't remove that.
- Native bindings missing from jna-platform live in `User32Ex` and `DwmApi`.

**Native Linux backend.** `LinuxCustomStage` uses `undecorated.fxml` and a `TRANSPARENT` stage like the fallback, but leaves window management to the window manager. `Gtk3` holds the JNA bindings; all calls must run on the JavaFX thread, which is GTK's main thread.
- The GdkWindow is looked up on every `WINDOW_SHOWN`, since JavaFX destroys the native window on hide. Native calls must stop at `WINDOW_HIDING`.
- Lookup order (`Gtk3.findGdkWindow`):
  1. The X11 ID from JavaFX internals by reflection. On the module path this needs the flags from the README.
  2. The only top-level window realized between `WINDOW_SHOWING` and `WINDOW_SHOWN`.
  3. The only visible top-level GTK window with the stage's title.
  If all fail, the stage falls back to `UndecoratedStage`.
- Mouse filters: pressing a resize edge calls `gdk_window_begin_resize_drag`. Dragging the caption calls `gdk_window_begin_move_drag` on `DRAG_DETECTED`. Double-pressing the caption toggles maximize.
- `gdk_window_set_shadow_width` (`_GTK_FRAME_EXTENTS`) is set to the backdrop's insets, and to 0 when maximized. It is set from a `realize` emission hook before the window is mapped. Otherwise the window manager would move and resize the window to keep its visible part in place. Don't defer the insets-driven update: CSS sets the insets before the window is mapped.
- `GtkWindowFunctions`: JavaFX restricts the WM functions of undecorated windows (no maximize/minimize) and resets them on every restore. `gdk_window_set_functions` is therefore re-applied from a GDK filter on `_NET_WM_STATE`. The filter must be removed on `WINDOW_HIDING`, before the window is destroyed.
- JavaFX's `GtkWindow._maximize` always reports `MAXIMIZE`. `setMaximized(false)` only sticks once the WM sends a real restore. So the WM must be allowed to maximize the window, and `UndecoratedStage` uses `GtkWindowFunctions` on Linux too when GTK 3 is loaded. Without GTK 3 (GTK 2 glass), the imitation's restore may still be unreliable.

**Undecorated fallback.** `UndecoratedStage` makes the stage `StageStyle.TRANSPARENT` and implements move, resize, maximize and an Aero Snap preview in pure JavaFX, driven by mouse event filters. The preview (`aero-snap-preview.fxml`) is a `Popup`, so it gets no task bar entry and doesn't take focus; mouse events that reach it are forwarded. The bounds from before maximizing are tracked continuously and restored on unmaximize.

**Hit testing is duplicated.** `DecorationWindowProcedure.hitTest` (Windows) and `FrameHitTest.hitTest` (Linux and the fallback) are separate implementations of the same idea:
- The caption height comes from the title bar's height, else the `controlBox` height, else `DEFAULT_FRAME_DRAG_HEIGHT` (30).
- Resize border:
  - On Windows it is `WindowController.getResizeBorderThickness()` (8).
  - In `FrameHitTest` it is the transparent shadow area around `windowRoot` plus 4px inside it, with 16px corners.
- `controlBox`, the icon and `nonCaptionNodes` always count as client area.

If you change caption or resize logic, check whether the other backend needs the same change.

## Conventions

- `.editorconfig`: 2-space indentation, LF line endings, UTF-8.
