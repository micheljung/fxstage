package ch.micheljung.fxwindow;

public enum HitTestResult {
  TOPLEFT(13),
  TOP(12),
  CAPTION(2),
  TOPRIGHT(14),
  LEFT(10),
  NOWHERE(0),
  HTCLIENT(1),
  RIGHT(11),
  BOTTOMLEFT(16),
  BOTTOM(15),
  BOTTOMRIGHT(17),
  SYSMENU(3);

  final int windowsValue;

  /**
   * @param windowsValue see <a href="https://docs.microsoft.com/en-us/windows/win32/inputdev/wm-nchittest#return-value">WM_NCHITTEST
   * return values</a>
   */
  HitTestResult(int windowsValue) {
    this.windowsValue = windowsValue;
  }
}
