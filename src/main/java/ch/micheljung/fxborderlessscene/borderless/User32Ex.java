package ch.micheljung.fxborderlessscene.borderless;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.win32.W32APIOptions;

interface User32Ex extends User32 {

  User32Ex INSTANCE = Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

  /**
   * Sets a custom window procedure for the application window, allowing to determine how to handle intercepted
   * messages.
   *
   * @return If the function succeeds, the return value is the previous value of the specified offset. If the function
   * fails, the return value is zero. To get extended error information, call GetLastError.
   */
  LONG_PTR SetWindowLongPtr(HWND hWnd, int nIndex, WindowProc wndProc);

  /**
   * Sets a custom window procedure for the application window, allowing to determine how to handle intercepted
   * messages.
   */
  LONG_PTR SetWindowLongPtr(HWND hWnd, int nIndex, LONG_PTR wndProc);

  HRESULT SetWindowCompositionAttribute(HWND hWnd, WindowCompositionAttributeData data);

  /** Call a window procedure. */
  LRESULT CallWindowProc(LONG_PTR proc, HWND hWnd, int uMsg, WPARAM uParam, LPARAM lParam);

  @Structure.FieldOrder({"attribute", "data", "sizeOfData"})
  class WindowCompositionAttributeData extends Structure implements Structure.ByReference {
    public int attribute;
    public Pointer data;
    public int sizeOfData;
  }
}