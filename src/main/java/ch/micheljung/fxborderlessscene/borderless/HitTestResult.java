package ch.micheljung.fxborderlessscene.borderless;

public enum HitTestResult {
  TOPLEFT(13),
  TOP(12),
  CAPTION(2),
  TOPRIGHT(14),
  LEFT(10),
  NOWHERE(0),
  RIGHT(11),
  BOTTOMLEFT(16),
  BOTTOM(15),
  BOTTOMRIGHT(17),
  SYSMENU(3);

  /**
   * @param windowsValue see <a href="https://docs.microsoft.com/en-us/windows/win32/inputdev/wm-nchittest#return-value">WM_NCHITTEST
   * return values</a>
   */
  HitTestResult(int windowsValue) {

  }
}
