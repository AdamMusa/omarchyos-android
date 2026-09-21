pragma Singleton
import QtQuick
import QtQuick.Window

// On the desktop every Quickshell surface is its own Wayland window. An
// Android app owns exactly one window, so the shell composites its surfaces
// inside it: one Item per layer-shell layer, stacked in the same order the
// compositor would use. PanelWindow/PopupWindow attach themselves here.
QtObject {
  id: root

  // Set once by the host's Main.qml.
  property Item container: null
  property bool backgroundReady: false
  readonly property real pixelRatio: container ? container.Screen.devicePixelRatio : 1
  readonly property real safeTop: AndroidBridge.nativeSystemBar ? (AndroidBridge.systemInsets.top || 0) / pixelRatio : 0
  readonly property real safeBottom: AndroidBridge.nativeSystemBar ? (AndroidBridge.systemInsets.bottom || 0) / pixelRatio : 0
  readonly property real safeLeft: AndroidBridge.nativeSystemBar ? (AndroidBridge.systemInsets.left || 0) / pixelRatio : 0
  readonly property real safeRight: AndroidBridge.nativeSystemBar ? (AndroidBridge.systemInsets.right || 0) / pixelRatio : 0

  readonly property var layerItems: ({})

  function layerFor(layer) {
    if (!container) return null
    var key = "layer" + layer
    if (!layerItems[key]) {
      var item = Qt.createQmlObject(
        'import QtQuick; Item { anchors.fill: parent; z: ' + layer + ' }',
        container, "ShellSurfaceRoot.layer" + layer)
      layerItems[key] = item
    }
    return layerItems[key]
  }

  readonly property real screenWidth: container ? container.width : 0
  readonly property real screenHeight: container ? container.height : 0
}
