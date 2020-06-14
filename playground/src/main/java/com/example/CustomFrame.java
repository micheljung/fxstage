package com.example;

import com.sun.jna.Memory;
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
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.Objects;

import static com.sun.jna.platform.win32.WinUser.GWL_WNDPROC;

public class CustomFrame {

  private static final int WM_NCCALCSIZE = 0x0083;
  private static final int WM_ACTIVATE = 0x0006;
  private static final int WM_NCHITTEST = 0x0084;
  private static final LRESULT WVR_VALIDRECTS = new LRESULT(0x0400);

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
      primaryStage.setScene(new Scene(new StackPane(), Color.web("#3C3F41")));
      primaryStage.show();

      // This causes the client area to be transparent, which is what I want, but it also removes window decorations.
//      primaryStage.initStyle(StageStyle.TRANSPARENT);
//      primaryStage.setScene(new Scene(new StackPane(), Color.TRANSPARENT));
//      primaryStage.show();

      HWND hwnd = new HWND();
      hwnd.setPointer(User32.INSTANCE.GetActiveWindow().getPointer());

      setCustomWindowProc(hwnd);
    }

    private void setCustomWindowProc(HWND hwnd) {
      BaseTSD.LONG_PTR defaultWindowProc = User32.INSTANCE.GetWindowLongPtr(hwnd, GWL_WNDPROC);

      // Since the window has already been created, this can't be called in WM_CREATE, so do it here
//      extendFrameIntoClientArea(hwnd);


      WinUser.WindowProc windowProc = new WinUser.WindowProc() {
        @Override
        public LRESULT callback(HWND hwnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
          switch (uMsg) {
            // With this, the whole window becomes client-area. It does not remove the default frame but JavaFX covers
            // it in white paint
            case WM_NCCALCSIZE:
              if (wParam.intValue() == 1) {
                NCCALCSIZE_PARAMS nCalcSizeParams = new NCCALCSIZE_PARAMS(new Pointer(lParam.longValue()));
                nCalcSizeParams.rgrc[0].left -= 7;
                nCalcSizeParams.rgrc[0].right -= 7;
                nCalcSizeParams.rgrc[0].bottom -= 7;
                nCalcSizeParams.rgrc[0].top -= 7;
                nCalcSizeParams.write();
                return WVR_VALIDRECTS;
              }
              return LRESULT_ZERO;

//            case WM_NCHITTEST:
//              Memory memory = new Memory(Long.BYTES);
//              Pointer lpResult = memory.share(0);
//              DwmApi.INSTANCE.DwmDefWindowProc(hwnd, uMsg, wParam, lParam, lpResult);
//              if (lpResult.getLong(0) == 0) {
//                // FIXME implement hittest
//                System.out.println("hittest");
//              }
//              return new LRESULT(lpResult.getLong(0));

            case WM_ACTIVATE:
              extendFrameIntoClientArea(hwnd);
              return User32Ex.INSTANCE.CallWindowProc(defaultWindowProc, hwnd, uMsg, wParam, lParam);

            default:
              return User32Ex.INSTANCE.CallWindowProc(defaultWindowProc, hwnd, uMsg, wParam, lParam);
          }
        }
      };

      User32Ex.INSTANCE.SetWindowLongPtr(hwnd, GWL_WNDPROC, windowProc);
//      User32Ex.INSTANCE.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);
    }
  }

  private static void extendFrameIntoClientArea(WinDef.HWND hwnd) {
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

    public WinDef.RECT[] rgrc = new WinDef.RECT[3];
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

    public int cxLeftWidth;
    public int cxRightWidth;
    public int cyTopHeight;
    public int cyBottomHeight;
  }

  public interface User32Ex extends User32 {
    User32Ex INSTANCE = Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

    Pointer SetWindowLongPtr(HWND hWnd, int nIndex, WindowProc wndProc);

    LRESULT CallWindowProc(LONG_PTR proc, HWND hWnd, int uMsg, WPARAM uParam, LPARAM lParam);
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
}
