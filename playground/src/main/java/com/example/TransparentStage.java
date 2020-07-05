package com.example;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.Objects;

import static com.sun.jna.platform.win32.WinUser.GWL_EXSTYLE;
import static com.sun.jna.platform.win32.WinUser.GWL_STYLE;
import static com.sun.jna.platform.win32.WinUser.GWL_WNDPROC;
import static com.sun.jna.platform.win32.WinUser.SWP_FRAMECHANGED;
import static com.sun.jna.platform.win32.WinUser.SWP_NOMOVE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOSIZE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOZORDER;
import static com.sun.jna.platform.win32.WinUser.SW_SHOW;
import static com.sun.jna.platform.win32.WinUser.WS_OVERLAPPEDWINDOW;

public class TransparentStage {

  private static final int WM_NCCALCSIZE = 0x0083;
  private static final int WM_ACTIVATE = 0x0006;
  private static final int WM_NCHITTEST = 0x0084;

  public static final LRESULT LRESULT_ZERO = new LRESULT(0);

  /**
   * Starting CustomFrameApplication instead of calling this main, at least in IntelliJ, you'll get "Error: JavaFX
   * runtime components are missing, and are required to run this application".
   */
  public static void main(String[] args) {
    CustomFrameApplication.launch(CustomFrameApplication.class, args);
  }

  public static class CustomFrameApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
      // Even with a transparent scene, a white background will be painted. It will be re-painted when resizing.
      primaryStage.initStyle(StageStyle.DECORATED);
      primaryStage.setScene(new Scene(new StackPane(new Button("Hello")), 400, 300, Color.BLACK));
      primaryStage.show();


      // This causes the client area to be transparent, which is what I want, but it also removes window decorations.
//      primaryStage.initStyle(StageStyle.TRANSPARENT);
//      primaryStage.setScene(new Scene(new StackPane(), Color.TRANSPARENT));
//      primaryStage.show();

      HWND hwnd = new HWND();
      hwnd.setPointer(User32.INSTANCE.GetActiveWindow().getPointer());

      setCustomWindowProc(hwnd);
      enableBlurBehind(hwnd);

      ((Button)primaryStage.getScene().getRoot().getChildrenUnmodifiable().get(0)).setText("hasasdf");
    }

    private void setCustomWindowProc(HWND hwnd) {
      BaseTSD.LONG_PTR defaultWindowProc = User32.INSTANCE.GetWindowLongPtr(hwnd, GWL_WNDPROC);

      // Since the window has already been created, this can't be called in WM_CREATE, so do it here
//      extendFrameIntoClientArea(hwnd);

      User32Ex.INSTANCE.SetWindowLong(hwnd, GWL_STYLE, WS_OVERLAPPEDWINDOW);
      User32Ex.INSTANCE.SetWindowLong(hwnd, GWL_EXSTYLE, WinUser.WS_EX_LAYERED);
      User32Ex.INSTANCE.ShowWindow(hwnd, SW_SHOW);
      User32Ex.INSTANCE.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);
    }
  }

  private static void extendFrameIntoClientArea(HWND hwnd) {
    WinNT.HRESULT hresult = DwmApi.INSTANCE.DwmExtendFrameIntoClientArea(hwnd, new Margins(0, 0, 1, 0));
    if (!Objects.equals(hresult, WinError.S_OK)) {
      throw new IllegalStateException("Could not call DwmExtendFrameIntoClientArea");
    }
  }

  @Structure.FieldOrder({"rgrc", "lppos"})
  public static class NCCALCSIZE_PARAMS extends Structure implements Structure.ByReference {
    public NCCALCSIZE_PARAMS(Pointer p) {
      super(p);
      read();
    }

    public WinDef.RECT rgrc[];
    public WindowPos lppos;
  }

  @Structure.FieldOrder({"hwndInsertAfter", "hwnd", "x", "y", "cx", "cy", "flags"})
  public static class WindowPos extends Structure implements Structure.ByReference {
    public WindowPos(Pointer p) {
      super(p);
      read();
    }

    public int hwndInsertAfter;
    public int hwnd;
    public int x;
    public int y;
    public int cx;
    public int cy;
    public int flags;
  }

  @Structure.FieldOrder({"cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight"})
  public static class Margins extends Structure implements Structure.ByReference {

    public Margins(int cxLeftWidth, int cxRightWidth, int cyTopHeight, int cyBottomHeight) {
      this.cxLeftWidth = cxLeftWidth;
      this.cxRightWidth = cxRightWidth;
      this.cyTopHeight = cyTopHeight;
      this.cyBottomHeight = cyBottomHeight;
    }

    public int cxLeftWidth = 0;
    public int cxRightWidth = 0;
    public int cyTopHeight = 0;
    public int cyBottomHeight = 0;
  }

  public interface User32Ex extends User32 {
    User32Ex INSTANCE = Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

    Pointer SetWindowLongPtr(HWND hWnd, int nIndex, WindowProc wndProc);

    Pointer SetWindowLongPtr(HWND hWnd, int nIndex, int wndProc);

    HRESULT SetWindowCompositionAttribute(HWND hWnd, WindowCompositionAttributeData data);
  }

  public interface DwmApi extends StdCallLibrary, WinUser, WinNT {

    /** A value for the fEnable member has been specified. */
    int DWM_BB_ENABLE = 1;
    /** A value for the hRgnBlur member has been specified. */
    int DWM_BB_BLURREGION = 2;
    /** A value for the fTransitionOnMaximized member has been specified. */
    int DWM_BB_TRANSITIONONMAXIMIZED = 4;

    DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);

    /**
     * @see <a href="https://docs.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmextendframeintoclientarea?redirectedfrom=MSDN">MSDN</a>
     */
    HRESULT DwmExtendFrameIntoClientArea(HWND hWnd, Margins pMarInset);

    /** @see <a href="https://docs.microsoft.com/en-us/windows/win32/api/dwmapi/nf-dwmapi-dwmdefwindowproc">MSDN</a> */
    void DwmDefWindowProc(HWND hWnd, int msg, WPARAM wParam, LPARAM lParam, Pointer plResult);
  }


  private static void enableBlurBehind(WinDef.HWND hwnd) {
    AccentPolicy accent = new AccentPolicy();
    accent.accentState = AccentState.ACCENT_ENABLE_BLURBEHIND;
    accent.write();

    WindowCompositionAttributeData data = new WindowCompositionAttributeData();
    data.attribute = WindowCompositionAttribute.DWMWA_ACCENT_POLICY;
    data.data = accent.getPointer();
    data.sizeOfData = accent.size();

    WinNT.HRESULT hresult = User32Ex.INSTANCE.SetWindowCompositionAttribute(hwnd, data);
    if (hresult.intValue() != 1) {
      throw new IllegalStateException("Could not call SetWindowCompositionAttribute: " + hresult);
    }
  }

  @Structure.FieldOrder({"attribute", "data", "sizeOfData"})
  public static class WindowCompositionAttributeData extends Structure implements Structure.ByReference {
    public int attribute;
    public Pointer data;
    public int sizeOfData;
  }

  /** See <a href="https://docs.microsoft.com/de-de/windows/win32/api/dwmapi/ns-dwmapi-dwm_blurbehind">MSDN</a> */
  @Structure.FieldOrder({"dwFlags", "fEnable", "hRgnBlur", "fTransitionOnMaximized"})
  public class DwmBlurBehind extends Structure implements Structure.ByReference {
    /**
     * A bitwise combination of DWM Blur Behind constant values that indicates which of the members of this structure
     * have been set.
     */
    public WinDef.DWORD dwFlags = new WinDef.DWORD();
    /**
     * TRUE to register the window handle to DWM blur behind; FALSE to unregister the window handle from DWM blur
     * behind.
     */
    public WinDef.BOOL fEnable;
    /**
     * The region within the client area where the blur behind will be applied. A NULL value will apply the blur behind
     * the entire client area.
     */
    public WinDef.HRGN hRgnBlur;
    /** TRUE if the window's colorization should transition to match the maximized windows; otherwise, FALSE. */
    public WinDef.BOOL fTransitionOnMaximized;
  }

  public interface AccentState {
    int ACCENT_DISABLED = 0;
    int ACCENT_ENABLE_GRADIENT = 1;
    int ACCENT_ENABLE_TRANSPARENTGRADIENT = 2;
    int ACCENT_ENABLE_BLURBEHIND = 3;
    /** Added with Windows 10, Version 1803 (Build 17134). */
    int ACCENT_ENABLE_ACRYLICBLURBEHIND = 4;
    /** Added with Windows 10, Version 1809 (Build 17763). */
    int ACCENT_ENABLE_HOSTBACKDROP = 5;
    int ACCENT_INVALID_STATE = 6;
  }

  /** See <a href="https://docs.microsoft.com/en-us/windows/desktop/api/dwmapi/ne-dwmapi-dwmwindowattribute">MSDN</a>. */
  public interface WindowCompositionAttribute {
    int DWMWA_UNDEFINED = 0;
    int DWMWA_NCRENDERING_ENABLED = 1;
    int DWMWA_NCRENDERING_POLICY = 2;
    int DWMWA_TRANSITIONS_FORCEDISABLED = 3;
    int DWMWA_ALLOW_NCPAINT = 4;
    int DWMWA_CAPTION_BUTTON_BOUNDS = 5;
    int DWMWA_NONCLIENT_RTL_LAYOUT = 6;
    int DWMWA_FORCE_ICONIC_REPRESENTATION = 7;
    int DWMWA_EXTENDED_FRAME_BOUNDS = 8;
    int DWMWA_HAS_ICONIC_BITMAP = 9;
    int DWMWA_THEME_ATTRIBUTES = 10;
    int DWMWA_NCRENDERING_EXILED = 11;
    int DWMWA_NCADORNMENTINFO = 12;
    int DWMWA_EXCLUDED_FROM_LIVEPREVIEW = 13;
    int DWMWA_VIDEO_OVERLAY_ACTIVE = 14;
    int DWMWA_FORCE_ACTIVEWINDOW_APPEARANCE = 15;
    int DWMWA_DISALLOW_PEEK = 16;
    int DWMWA_CLOAK = 17;
    int DWMWA_CLOAKED = 18;
    int DWMWA_ACCENT_POLICY = 19;
    int DWMWA_FREEZE_REPRESENTATION = 20;
    int DWMWA_EVER_UNCLOAKED = 21;
    int DWMWA_VISUAL_OWNER = 22;
    int DWMWA_HOLOGRAPHIC = 23;
    int DWMWA_EXCLUDED_FROM_DDA = 24;
    int DWMWA_PASSIVEUPDATEMODE = 25;
    int DWMWA_USEDARKMODECOLORS = 26;
    int DWMWA_LAST = 27;
  }

  public interface WdmNcRenderingPolicy {
    int DWMNCRP_USEWINDOWSTYLE = 1;
    int DWMNCRP_DISABLED = 2;
    int DWMNCRP_ENABLED = 3;
    int DWMNCRP_LAST = 4;
  }

  @Structure.FieldOrder({"accentState", "accentFlags", "gradientColor", "animationId"})
  public static class AccentPolicy extends Structure implements Structure.ByReference {
    public int accentState;
    public int accentFlags;
    public int gradientColor;
    public int animationId;
  }
}
