import QtQuick
import Quickshell
import Quickshell.Wayland
import qs.Commons

Item {
  property var shell: null
  PanelWindow {
    anchors { top: true; bottom: true; left: true; right: true }
    exclusionMode: ExclusionMode.Ignore
    color: Color.background
    WlrLayershell.layer: WlrLayer.Background
    WlrLayershell.namespace: "omarchy-background"
    Image {
      anchors.fill: parent
      source: AndroidBridge.themeState.palette ? (AndroidBridge.themeState.wallpaper || "") : ("file://" + Quickshell.env("XDG_STATE_HOME") + "/omarchy/current/background")
      // Fill portrait displays without stretching or letterboxing landscape artwork.
      fillMode: Image.PreserveAspectCrop
      asynchronous: true
      cache: false
    }
  }
}
