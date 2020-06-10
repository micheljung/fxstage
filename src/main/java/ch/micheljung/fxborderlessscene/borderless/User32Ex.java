package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.platform.win32.User32;

interface User32Ex extends User32 {

  /** An offset value that is used set a new address for the window procedure. */
  int GWLP_WNDPROC = -4;

  /**
   * Sets a custom window procedure for the application window, allowing to determine how to handle intercepted
   * messages.
   */
  LONG_PTR SetWindowLongPtr(HWND hWnd, int nIndex, WindowProc wndProc);

  /**
   * Sets a custom window procedure for the application window, allowing to determine how to handle intercepted
   * messages.
   */
  LONG_PTR SetWindowLongPtr(HWND hWnd, int nIndex, LONG_PTR wndProc);

  /** Call a window procedure. */
  LRESULT CallWindowProc(LONG_PTR proc, HWND hWnd, int uMsg, WPARAM uParam, LPARAM lParam);
}