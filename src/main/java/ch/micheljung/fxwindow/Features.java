package ch.micheljung.fxwindow;

import lombok.Data;

@Data
class Features {
  private boolean allowMinimize;
  private boolean allowTopResize;
  private boolean useBlurBehind;
  private boolean useAcrylic;
  private boolean useNative;
}
