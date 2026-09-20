import QtQuick

// Upstream's free-floating window. On a phone these fill the shell window.
Item {
  id: root
  property var screen: Qt.application.screens.length > 0 ? Qt.application.screens[0] : null
  property color color: "transparent"
  width: ShellSurfaceRoot.screenWidth
  height: ShellSurfaceRoot.screenHeight
  Rectangle { anchors.fill: parent; color: root.color; z: -1 }
  Component.onCompleted: {
    var host = ShellSurfaceRoot.layerFor(3)
    if (host) parent = host
  }
}
