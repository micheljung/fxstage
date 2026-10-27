# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

FxStage is a small Java 11 library (published to Maven Central as `io.github.micheljung:fxstage`) that turns a JavaFX `Stage` into a borderless window. On Windows 10/11 it keeps native behaviour (drop shadow, resizing, Aero Snap) by hooking the Win32 window procedure via JNA. On every other platform it falls back to a cross-platform imitation, which is known to be buggy. The README says the API isn't polished yet and may change.

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
- `demo`: sample app (`ch.micheljung.fxborderlessscene.Main`) that uses the external `jfx-waitomo-theme`.
- `playground`: standalone Win32/JNA experiments. It is not wired to the library and uses the old `org.openjfx.javafxplugin`.

## Architecture

**Entry point and builder.** `FxStage.configure(stage)` / `FxStage.create(content)` return `BuilderImpl`, which implements both `StageConfigurer` and `StageCreator`. `apply()`:
1. Loads the window FXML with `WindowController` as its controller. The FXML is `windows.fxml` on native Windows and `undecorated.fxml` otherwise, unless `withWindowFxml` overrides it.
2. Moves the stage's existing scene root (or the given content) into `windowContent`, then makes the FXML root the scene root.
3. Picks a backend through `useWindows()`, which is true only if `Platform.isWindows() && useNative && os.name` is exactly `"Windows 10"` or `"Windows 11"`.
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

**Undecorated fallback.** `UndecoratedStage` makes the stage `StageStyle.TRANSPARENT` and implements move, resize, maximize and an Aero Snap preview window (`aero-snap-preview.fxml`) in pure JavaFX, driven by mouse event filters.

**Hit testing is duplicated.** `DecorationWindowProcedure.hitTest` and `UndecoratedStage.hitTest` are separate implementations of the same idea:
- The caption height comes from the title bar's height, else the `controlBox` height, else `DEFAULT_FRAME_DRAG_HEIGHT` (30).
- The resize border is `WindowController.getResizeBorderThickness()` (8).
- `controlBox`, the icon and `nonCaptionNodes` always count as client area.

If you change caption or resize logic, check whether the other backend needs the same change.

## Conventions

- `.editorconfig`: 2-space indentation, LF line endings, UTF-8.
