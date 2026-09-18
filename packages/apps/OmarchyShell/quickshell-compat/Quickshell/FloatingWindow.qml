import QtQuick
import QtQuick.Window

// Upstream's free-floating window. On a phone these render as full-screen
// surfaces owned by the shell process.
Window {
  id: root
  property var screen: Qt.application.screens.length > 0 ? Qt.application.screens[0] : null
  flags: Qt.Window | Qt.FramelessWindowHint
  color: "transparent"
  visible: true
}
