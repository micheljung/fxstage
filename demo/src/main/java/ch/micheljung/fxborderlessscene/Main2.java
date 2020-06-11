package ch.micheljung.fxborderlessscene;

import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.Arrays;
import java.util.List;

import static com.sun.jna.platform.win32.WinUser.GWL_WNDPROC;
import static com.sun.jna.platform.win32.WinUser.SWP_FRAMECHANGED;
import static com.sun.jna.platform.win32.WinUser.SWP_NOMOVE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOSIZE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOZORDER;

public class Main2 {
  /**
   * Sent when the size and position of a window's client area must be calculated. By processing this message, an
   * application can control the content of the window's client area when the size or position of the window changes.
   */
  private static final int WM_NCCALCSIZE = 0x0083;
  public static class MARGINS extends Structure implements Structure.ByReference {

    public int cxLeftWidth;
    public int cxRightWidth;
    public int cyTopHeight;
    public int cyBottomHeight;

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList("cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight");
    }

  }


  public static interface Accent {
    public static final int ACCENT_DISABLED = 0;
    public static final int ACCENT_ENABLE_GRADIENT = 1;
    public static final int ACCENT_ENABLE_TRANSPARENTGRADIENT = 2;
    public static final int ACCENT_ENABLE_BLURBEHIND = 3;
    public static final int ACCENT_ENABLE_ACRYLIC = 4; // YES, available on build 17063
    public static final int ACCENT_INVALID_STATE = 5;
  }

  public static interface WindowCompositionAttribute {
    public static final int WCA_ACCENT_POLICY = 19;
  }

  public static class AccentPolicy extends Structure implements Structure.ByReference {
    public static final List<String> FIELDS = createFieldsOrder("AccentState", "AccentFlags", "GradientColor",
      "AnimationId");
    public int AccentState;
    public int AccentFlags;
    public int GradientColor;
    public int AnimationId;

    @Override
    protected List<String> getFieldOrder() {
      return FIELDS;
    }
  }

  public static class WindowCompositionAttributeData extends Structure implements Structure.ByReference {
    public static final List<String> FIELDS = createFieldsOrder("Attribute", "Data", "SizeOfData");
    public int Attribute;
    public Pointer Data;
    public int SizeOfData;

    @Override
    protected List<String> getFieldOrder() {
      return FIELDS;
    }
  }

  public static void main(String[] args) {
    DemoApplication.launch(DemoApplication.class, args);
  }

  public static class DemoApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
      primaryStage.show();

      WinDef.HWND hwnd = new WinDef.HWND();
      hwnd.setPointer(User32.INSTANCE.GetActiveWindow().getPointer());

      NativeLibrary dwmapi = NativeLibrary.getInstance("dwmapi");


      MARGINS margins = new MARGINS();
      margins.cxLeftWidth = 8;
      margins.cxRightWidth = 8;
      margins.cyBottomHeight = 20;
      margins.cyTopHeight = 27;


      Function extendFrameIntoClientArea = dwmapi.getFunction("DwmExtendFrameIntoClientArea");
      WinNT.HRESULT result = (WinNT.HRESULT) extendFrameIntoClientArea.invoke(WinNT.HRESULT.class,
        new Object[] { hwnd, margins });

      if (result.intValue() != 0)
        System.err.println("Call to DwmExtendFrameIntoClientArea failed.");

      //SetLayeredWindowAttributes(hwnd, 0xffffffff, (byte) 0, LWA_COLORKEY); // Modify mask color.
      User32.INSTANCE.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);


// WIN10 Composite using JNA.

      NativeLibrary user32 = NativeLibrary.getInstance("user32");

      AccentPolicy accent = new AccentPolicy();
      accent.AccentState = Accent.ACCENT_ENABLE_ACRYLIC;
      accent.GradientColor = 0x7F000000;
//      accent.AccentFlags = AccentFlags.DrawAllBorders;
      accent.write();

      WindowCompositionAttributeData data = new WindowCompositionAttributeData();
      data.Attribute = WindowCompositionAttribute.WCA_ACCENT_POLICY;
      data.SizeOfData = accent.size();
      data.Data = accent.getPointer();

      Function setWindowCompositionAttribute = user32.getFunction("SetWindowCompositionAttribute");
      setWindowCompositionAttribute.invoke(WinNT.HRESULT.class, new Object[] { hwnd, data });

      long dwp = User32.INSTANCE.GetWindowLongPtr(hwnd, GWL_WNDPROC).longValue();


//      WinUser.WindowProc proc = new WinUser.WindowProc() {
//
//        @Override
//        public WinDef.LRESULT callback(WinDef.HWND hwnd, int uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam) {
//
//          Object defaultWindowsProcedure = User32.INSTANCE.SetWindowLongPtr(hwnd, GWL_WNDPROC, this);
//
//          switch (uMsg) {
//            case WM_NCCALCSIZE:
//              if (wParam.intValue() != 0) {
//                return new WinDef.LRESULT(0);
//              } else {
//                return JNI.callPPPP(dwp, hwnd, uMsg, wParam, lParam);
//              }
//          }
//          return JNI.callPPPP(dwp, hwnd, uMsg, wParam, lParam);
//        }
//      };
//
//
//      SetWindowLongPtr(hwnd, GWL_WNDPROC, proc.address());
//      SetWindowPos(hwnd, 0, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);

    }
  }
}
