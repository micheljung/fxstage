package ch.micheljung.fxborderlessscene.borderless;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.Node;

class ComponentBinding {

  private static final int DEFAULT_TITLE_BAR_HEIGHT = 30;

  private final IntegerProperty frameResizeBorderThickness = new SimpleIntegerProperty(4);
  private final IntegerProperty frameBorderThickness = new SimpleIntegerProperty(1);

  private Node controlBox;
  private Node rightMenuBar;
  private Node titleBar;
  private Node icon;
  private Node leftMenuBar;

  int getControlBoxWidth() {
    return (int) controlBox.getLayoutBounds().getWidth();
  }

  int getIconWidth() {
    return icon != null ? (int) icon.getLayoutBounds().getWidth() : 0;
  }

  int getExtraLeftReservedWidth() {
    return leftMenuBar != null ? (int) leftMenuBar.getLayoutBounds().getWidth() : 0;
  }

  int getExtraRightReservedWidth() {
    return rightMenuBar != null ? (int) rightMenuBar.getLayoutBounds().getWidth() : 0;
  }

  int getTitleBarHeight() {
    return titleBar != null ? (int) titleBar.getLayoutBounds().getHeight() : DEFAULT_TITLE_BAR_HEIGHT;
  }

  int getFrameResizeBorderThickness() {
    return frameResizeBorderThickness.get();
  }

  int getFrameBorderThickness() {
    return frameBorderThickness.get();
  }

  public void setControlBox(Node controlBox) {
    this.controlBox = controlBox;
  }

  public void setRightMenuBar(Node rightMenuBar) {
    this.rightMenuBar = rightMenuBar;
  }

  public void setTitleBar(Node titleBar) {
    this.titleBar = titleBar;
  }

  public void setIcon(Node icon) {
    this.icon = icon;
  }

  public void setLeftMenuBar(Node leftMenuBar) {
    this.leftMenuBar = leftMenuBar;
  }

}