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
      source: AndroidBridge.themeState.wallpaper || ("file://" + Quickshell.env("XDG_STATE_HOME") + "/omarchy/current/background")
      // Keep the desktop artwork intact in portrait.
      fillMode: AndroidBridge.themeState.wallpaperFit === false ? Image.PreserveAspectCrop : Image.PreserveAspectFit
      asynchronous: true
      cache: false
    }
  }
}
