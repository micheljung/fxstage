package ch.micheljung.fxborderlessscene;

import ch.micheljung.fxborderlessscene.borderless.User32Ex;
import com.sun.jna.Function;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.LPARAM;
import com.sun.jna.platform.win32.WinDef.LRESULT;
import com.sun.jna.platform.win32.WinDef.POINT;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.WPARAM;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.List;

import static com.sun.jna.platform.win32.WinUser.GWL_WNDPROC;
import static com.sun.jna.platform.win32.WinUser.LWA_COLORKEY;
import static com.sun.jna.platform.win32.WinUser.SWP_FRAMECHANGED;
import static com.sun.jna.platform.win32.WinUser.SWP_NOMOVE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOSIZE;
import static com.sun.jna.platform.win32.WinUser.SWP_NOZORDER;
import static com.sun.jna.platform.win32.WinUser.WM_DESTROY;

public class Main2 {
  /**
   * Sent when the size and position of a window's client area must be calculated. By processing this message, an
   * application can control the content of the window's client area when the size or position of the window changes.
   */
  private static final int WM_NCCALCSIZE = 0x0083;
  private static final int WM_NCHITTEST = 0x0084;

  private static final User32Ex user32Ex = User32Ex.INSTANCE;

  public static final LRESULT LRESULT_ZERO = new LRESULT(0);

  @Structure.FieldOrder({"cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight"})
  public static class MARGINS extends Structure implements Structure.ByReference {

    public int cxLeftWidth;
    public int cxRightWidth;
    public int cyTopHeight;
    public int cyBottomHeight;
  }

  public interface Accent {
    int ACCENT_DISABLED = 0;
    int ACCENT_ENABLE_GRADIENT = 1;
    int ACCENT_ENABLE_TRANSPARENTGRADIENT = 2;
    int ACCENT_ENABLE_BLURBEHIND = 3;
    int ACCENT_ENABLE_ACRYLIC = 4; // YES, available on build 17063
    int ACCENT_INVALID_STATE = 5;
  }

  public interface WindowCompositionAttribute {
    int WCA_ACCENT_POLICY = 19;
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

  public static void main(String[] args) {
    DemoApplication.launch(DemoApplication.class, args);
  }

  public static class DemoApplication extends Application {

    @Override
    public void start(Stage primaryStage) {
      StackPane root = new StackPane(new Label("Hello"));
      root.setMinWidth(800);
      root.setMinHeight(600);
      root.setBackground(Background.EMPTY);
      Scene scene = new Scene(root, Color.GRAY);
//      scene.setFill(Color.RED);
//      primaryStage.setScene(scene);
      primaryStage.show();

      customize();
    }

    private void customize() {
      HWND hwnd = new HWND();
      hwnd.setPointer(User32.INSTANCE.GetActiveWindow().getPointer());

      BaseTSD.LONG_PTR dwp = User32.INSTANCE.GetWindowLongPtr(hwnd, GWL_WNDPROC);
      User32Ex.INSTANCE.SetWindowLongPtr(hwnd, GWL_WNDPROC, (hwnd1, uMsg, wParam, lParam) -> {
        LRESULT lresult;
        switch (uMsg) {
          case WM_DESTROY:
            user32Ex.SetWindowLongPtr(hwnd, GWL_WNDPROC, dwp);
            return LRESULT_ZERO;

          case WM_NCHITTEST:
            lresult = BorderLessHitTest(hwnd, uMsg, wParam, lParam);
            if (lresult.intValue() == new LRESULT(0).intValue()) {
              return User32Ex.INSTANCE.CallWindowProc(dwp, hwnd, uMsg, wParam, lParam);
            }
            return lresult;

          case WM_NCCALCSIZE:
            return new LRESULT(0);
          // This will cause windows not to draw the non-client area and thereby effectively making all the window our client area
//            if (wParam.intValue() == 1) {
//              return LRESULT_ZERO;
//            }
          // Falls through

          default:
            return user32Ex.CallWindowProc(dwp, hwnd, uMsg, wParam, lParam);
        }
      });
      boolean b = User32Ex.INSTANCE.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);
      System.out.println(b);


// WIN7 Frame using LWJGL API and JNA.
      NativeLibrary dwmapi = NativeLibrary.getInstance("dwmapi");

      MARGINS margins = new MARGINS();
      margins.cxLeftWidth = 0;
      margins.cxRightWidth = 0;
      margins.cyBottomHeight = 0;
      margins.cyTopHeight = 0;

      Function extendFrameIntoClientArea = dwmapi.getFunction("DwmExtendFrameIntoClientArea");
      WinNT.HRESULT result = (WinNT.HRESULT) extendFrameIntoClientArea.invoke(WinNT.HRESULT.class,
        new Object[]{hwnd, margins});

      if (result.intValue() != 0) {
        System.err.println("Call to DwmExtendFrameIntoClientArea failed.");
      }

//      User32.INSTANCE.SetLayeredWindowAttributes(hwnd, 0xffffff00, (byte) 0, LWA_COLORKEY); // Modify mask color.
      User32.INSTANCE.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);

// WIN10 Composite using JNA.

      AccentPolicy accent = new AccentPolicy();
      accent.AccentState = Accent.ACCENT_ENABLE_BLURBEHIND;
      accent.write();

      User32Ex.WindowCompositionAttributeData data = new User32Ex.WindowCompositionAttributeData();
      data.attribute = WindowCompositionAttribute.WCA_ACCENT_POLICY;
      data.sizeOfData = accent.size();
      data.data = accent.getPointer();

//      WinNT.HRESULT hresult = User32Ex.INSTANCE.SetWindowCompositionAttribute(hwnd, data);
//      System.out.println(hresult);

      User32Ex.INSTANCE.SetWindowPos(hwnd, hwnd, 0, 0, 0, 0, SWP_NOMOVE | SWP_NOSIZE | SWP_NOZORDER | SWP_FRAMECHANGED);
    }
  }

  static LRESULT BorderLessHitTest(HWND hWnd, int message, WPARAM wParam, LPARAM lParam) {
    int borderOffset = 4;
    int borderThickness = 4;

    POINT ptMouse = new POINT();
    RECT rcWindow = new RECT();
    User32.INSTANCE.GetCursorPos(ptMouse);
    User32.INSTANCE.GetWindowRect(hWnd, rcWindow);

    int uRow = 1, uCol = 1;
    boolean fOnResizeBorder = false, fOnFrameDrag = false;

    int titleBarHeight = 10;
    int topOffset = titleBarHeight == 0 ? borderThickness : titleBarHeight;
    if (ptMouse.y >= rcWindow.top && ptMouse.y < rcWindow.top + topOffset + borderOffset) {
      fOnResizeBorder = (ptMouse.y < (rcWindow.top + borderThickness));  // Top Resizing
      if (!fOnResizeBorder) {
        double controlBoxWidth = 150;
        double extraRightReservedWidth = 0;
        double iconWidth = 20;
        double extraLeftReservedWidth = 0;
        fOnFrameDrag = (ptMouse.y <= rcWindow.top + titleBarHeight + borderOffset)
          && (ptMouse.x < (rcWindow.right - (controlBoxWidth
          + borderOffset + extraRightReservedWidth)))
          && (ptMouse.x > (rcWindow.left + iconWidth
          + borderOffset + extraLeftReservedWidth));
      }
      uRow = 0; // Top Resizing or Caption Moving
    } else if (ptMouse.y < rcWindow.bottom && ptMouse.y >= rcWindow.bottom - borderThickness) {
      uRow = 2; // Bottom Resizing
    }
    if (ptMouse.x >= rcWindow.left && ptMouse.x < rcWindow.left + borderThickness) {
      uCol = 0; // Left Resizing
    } else if (ptMouse.x < rcWindow.right && ptMouse.x >= rcWindow.right - borderThickness) {
      uCol = 2; // Right Resizing
    }

    final int HTTOPLEFT = 13, HTTOP = 12, HTCAPTION = 2, HTTOPRIGHT = 14, HTLEFT = 10, HTNOWHERE = 0,
      HTRIGHT = 11, HTBOTTOMLEFT = 16, HTBOTTOM = 15, HTBOTTOMRIGHT = 17, HTSYSMENU = 3;

    int[][] hitTests = {
      {HTTOPLEFT, fOnResizeBorder ? HTTOP : fOnFrameDrag ? HTCAPTION : HTNOWHERE, HTTOPRIGHT},
      {HTLEFT, HTNOWHERE, HTRIGHT},
      {HTBOTTOMLEFT, HTBOTTOM, HTBOTTOMRIGHT},
    };

    return new LRESULT(hitTests[uRow][uCol]);
  }
}
