package com.example;

import com.sun.javafx.tk.TKStage;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.BaseTSD;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.lang.reflect.Method;

public class CustomWndProc {

  public static void main(String[] args) {
    CustomFrameApplication.launch(CustomFrameApplication.class, args);
  }

  public static class CustomFrameApplication extends Application {


    @Override
    public void start(Stage primaryStage) {
      WebView webView = new WebView();
      webView.getEngine().load("https://www.google.com");

      primaryStage.setScene(new Scene(webView));
      primaryStage.show();

      HWND hwnd = new HWND();
      hwnd.setPointer(User32.INSTANCE.GetActiveWindow().getPointer());

      Comctl32.INSTANCE.SetWindowSubclass(hwnd, new Comctl32.Subclassproc() {
        @Override
        public WinDef.LRESULT callback(HWND hWnd, WinDef.UINT uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam, WinDef.UINT_PTR uIdSubclass, BaseTSD.DWORD_PTR dwRefData) {
          return Comctl32.INSTANCE.DefSubclassProc(hWnd, uMsg, wParam, lParam);
        }
      }, new WinDef.UINT_PTR(1), new BaseTSD.DWORD_PTR(0));
    }
  }

  public interface Comctl32 extends StdCallLibrary {
    Comctl32 INSTANCE = Native.load("comctl32", Comctl32.class, W32APIOptions.DEFAULT_OPTIONS);

    WinDef.BOOL SetWindowSubclass(
      HWND hWnd,
      Subclassproc pfnSubclass,
      WinDef.UINT_PTR uIdSubclass,
      BaseTSD.DWORD_PTR dwRefData
    );

    WinDef.LRESULT DefSubclassProc(
      HWND hWnd,
      WinDef.UINT uMsg,
      WinDef.WPARAM wParam,
      WinDef.LPARAM lParam
    );

    interface Subclassproc extends StdCallCallback {
      WinDef.LRESULT callback(HWND hWnd, WinDef.UINT uMsg, WinDef.WPARAM wParam, WinDef.LPARAM lParam, WinDef.UINT_PTR uIdSubclass, BaseTSD.DWORD_PTR dwRefData);
    }
  }


  public interface User32Ex extends User32 {
    User32Ex INSTANCE = Native.load("user32", User32Ex.class, W32APIOptions.DEFAULT_OPTIONS);

    Pointer SetWindowLongPtr(HWND hWnd, int nIndex, WindowProc wndProc);

    LRESULT CallWindowProc(Pointer proc, HWND hWnd, int uMsg, WPARAM uParam, LPARAM lParam);
  }

  private WinDef.HWND getWindowPointer(Stage stage) throws Exception {
    Method getPeerMethod = Window.class.getDeclaredMethod("getPeer");
    getPeerMethod.setAccessible(true);
    TKStage tkStage = (TKStage) getPeerMethod.invoke(stage);
    Method getPlatformWindow = tkStage.getClass().getDeclaredMethod("getPlatformWindow");
    getPlatformWindow.setAccessible(true);
    Object platformWindow = getPlatformWindow.invoke(tkStage);
    Method getNativeHandle = platformWindow.getClass().getMethod("getNativeHandle");
    getNativeHandle.setAccessible(true);
    Object nativeHandle = getNativeHandle.invoke(platformWindow);
    return new WinDef.HWND(new Pointer((Long) nativeHandle));
  }
}
