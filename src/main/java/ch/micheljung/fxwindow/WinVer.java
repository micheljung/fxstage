package ch.micheljung.fxwindow;

import com.sun.jna.platform.win32.WinNT;

class WinVer {

  static final int WIN_1803_BUILD = 17134;
  static final int WIN_1809_BUILD = 17763;
  static final int WIN_1909_BUILD = 18363;

  private final WinNT.OSVERSIONINFO osVersionInfo;

  WinVer(WinNT.OSVERSIONINFO osVersionInfo) {
    this.osVersionInfo = osVersionInfo;
  }

  int getBuild() {
    return osVersionInfo.dwBuildNumber.intValue();
  }

  boolean isWindows7() {
    return osVersionInfo.dwMajorVersion.intValue() == 6 && osVersionInfo.dwMinorVersion.intValue() == 1;
  }

  boolean isWindows8() {
    return osVersionInfo.dwMajorVersion.intValue() == 6 && osVersionInfo.dwMinorVersion.intValue() == 2;
  }

  boolean isWindows10() {
    return osVersionInfo.dwMajorVersion.intValue() == 10 && osVersionInfo.dwMinorVersion.intValue() == 0;
  }

  @Override
  public String toString() {
    return String.format("Windows %s.%s Build %s",
      osVersionInfo.dwMajorVersion, osVersionInfo.dwMinorVersion, osVersionInfo.dwBuildNumber);
  }
}
