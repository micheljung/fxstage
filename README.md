# FxStage

A small library to create borderless JavaFX stages with native window behaviour.

## Features

* Borderless UI
* Native window behavior (Windows 10 only)
  * Drop shadow
  * Window resizing
  * Aero Snap
* Fallback to cross-platform Windows 10 imitation
* Non-invasive
* Fully customizable

## Screenshot

![Screenshot](media/screenshot.png)

## Usage

Convert an existing stage:

```java
class Test {
    FxStage fxWindow = FxStage.configure(stage)
      .useNative(true)
      .allowTopResize(true)
      .apply();
}
```

## Get It Now

Get it via https://bintray.com/micheljung/maven/fxstage/

```
implementation 'ch.micheljung.fxstage:fxstage:0.4.0'
```

## Important

* The API is not yet polished and might change in future
* Cross-platform Windows 10 imitation is yet buggy
* Requires Java 11+