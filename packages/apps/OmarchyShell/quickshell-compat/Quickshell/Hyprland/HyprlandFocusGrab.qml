import QtQuick
// Upstream grabs pointer focus so a click outside a popup dismisses it.
// Android: the host sets the window to watch outside touches.
QtObject {
  id: root
  property bool active: false
  property var windows: []
  signal cleared()
  onActiveChanged: AndroidBridge.setOutsideTouchGrab(active)
}
