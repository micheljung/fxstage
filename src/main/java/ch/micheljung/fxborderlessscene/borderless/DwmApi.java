package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.Native;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinUser;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public interface DwmApi extends StdCallLibrary, WinUser, WinNT {

  /** A value for the fEnable member has been specified. */
  int DWM_BB_ENABLE = 1;
  /** A value for the hRgnBlur member has been specified. */
  int DWM_BB_BLURREGION = 2;
  /** A value for the fTransitionOnMaximized member has been specified. */
  int DWM_BB_TRANSITIONONMAXIMIZED = 4;

  DwmApi INSTANCE = Native.load("dwmapi", DwmApi.class, W32APIOptions.DEFAULT_OPTIONS);

  /**
   * Enables the blur effect on a specified window.
   */
  HRESULT DwmEnableBlurBehindWindow(HWND hWnd, DwmBlurBehind pBlurBehind);

  /**
   * Enables the blur effect on a specified window.
   */
  HRESULT DwmExtendFrameIntoClientArea(HWND hWnd, Margins pMarInset);

  /**
   * Sets the value of Desktop Window Manager (DWM) non-client rendering attributes for a window. For programming
   * guidance, and code examples, see <a href="https://docs.microsoft.com/en-us/windows/desktop/dwm/composition-ovw#controlling-non-client-region-rendering">Controlling
   * non-client region rendering</a>.
   *
   * @param hwnd The handle to the window for which the attribute value is to be set.
   * @param dwAttribute A flag describing which value to set, specified as a value of the {@link
   * WindowCompositionAttribute} enumeration. This parameter specifies which attribute to set, and the pvAttribute
   * parameter points to an object containing the attribute value.
   * @param pvAttribute A pointer to an object containing the attribute value to set. The type of the value set depends
   * on the value of the dwAttribute parameter. The <a href="https://docs.microsoft.com/en-us/windows/desktop/api/dwmapi/ne-dwmapi-dwmwindowattribute">DWMWINDOWATTRIBUTE</a>
   * enumeration topic indicates, in the row for each flag, what type of value you should pass a pointer to in the
   * pvAttribute parameter.
   * @param cbAttribute The size, in bytes, of the attribute value being set via the pvAttribute parameter. The type of
   * the value set, and therefore its size in bytes, depends on the value of the dwAttribute parameter.
   * @return If the function succeeds, it returns S_OK. Otherwise, it returns an HRESULT error code. If Desktop
   * Composition has been disabled (Windows 7 and earlier), then this function returns DWM_E_COMPOSITIONDISABLED.
   */
  HRESULT DwmSetWindowAttribute(HWND hwnd, DWORD dwAttribute, LPVOID pvAttribute, DWORD cbAttribute);

  HRESULT DwmGetWindowAttribute(HWND hwnd, DWORD dwAttribute, LPVOID pvAttribute, DWORD cbAttribute);

  HRESULT DwmIsCompositionEnabled(BOOL pfEnabled);

  /** See <a href="https://docs.microsoft.com/de-de/windows/win32/api/dwmapi/ns-dwmapi-dwm_blurbehind">MSDN</a> */
  @Structure.FieldOrder({"dwFlags", "fEnable", "hRgnBlur", "fTransitionOnMaximized"})
  class DwmBlurBehind extends Structure implements Structure.ByReference {
    /**
     * A bitwise combination of DWM Blur Behind constant values that indicates which of the members of this structure
     * have been set.
     */
    public DWORD dwFlags = new DWORD();
    /**
     * TRUE to register the window handle to DWM blur behind; FALSE to unregister the window handle from DWM blur
     * behind.
     */
    public BOOL fEnable;
    /**
     * The region within the client area where the blur behind will be applied. A NULL value will apply the blur behind
     * the entire client area.
     */
    public HRGN hRgnBlur;
    /** TRUE if the window's colorization should transition to match the maximized windows; otherwise, FALSE. */
    public BOOL fTransitionOnMaximized;
  }

  @Structure.FieldOrder({"cxLeftWidth", "cxRightWidth", "cyTopHeight", "cyBottomHeight"})
  class Margins extends Structure implements Structure.ByReference {
    public int cxLeftWidth = 0;
    public int cxRightWidth = 0;
    public int cyTopHeight = 0;
    public int cyBottomHeight = 0;
  }

  interface AccentState {
    int ACCENT_DISABLED = 0;
    int ACCENT_ENABLE_GRADIENT = 1;
    int ACCENT_ENABLE_TRANSPARENTGRADIENT = 2;
    int ACCENT_ENABLE_BLURBEHIND = 3;
    int ACCENT_ENABLE_ACRYLICBLURBEHIND = 4; // RS4 1803
    int ACCENT_ENABLE_HOSTBACKDROP = 5; // RS5 1809
    int ACCENT_INVALID_STATE = 6;
  }

  /** See <a href="https://docs.microsoft.com/en-us/windows/desktop/api/dwmapi/ne-dwmapi-dwmwindowattribute">MSDN</a>. */
  interface WindowCompositionAttribute {
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

  interface WdmNcRenderingPolicy {
    int DWMNCRP_USEWINDOWSTYLE = 1;
    int DWMNCRP_DISABLED = 2;
    int DWMNCRP_ENABLED = 3;
    int DWMNCRP_LAST = 4;
  }

  @Structure.FieldOrder({"accentState", "accentFlags", "gradientColor", "animationId"})
  class AccentPolicy extends Structure implements Structure.ByReference {
    public int accentState;
    public int accentFlags;
    public int gradientColor;
    public int animationId;
  }
}