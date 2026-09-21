import QtQuick

// Upstream anchors popups to a parent surface through the Wayland positioner.
// Here a popup is an Item on the overlay layer of the shell window, placed by
// the same anchor/gravity/adjustment rules so menus and tooltips land where
// the bar expects them.
Item {
  id: root

  property PopupAnchor anchor: PopupAnchor {}
  property bool visibleRequested: false
  property var parentWindow: null
  property int relativeX: 0
  property int relativeY: 0
  property color color: "transparent"

  property real implicitWidth_: 0
  property alias implicitWidth: root.implicitWidth_
  property real implicitHeight_: 0
  property alias implicitHeight: root.implicitHeight_

  // Upstream sizes popups for a desktop, where a 700px calendar is small. A
  // phone surface cannot be wider than the screen it is composited into, so
  // the requested size is capped rather than allowed to overflow off-screen.
  width: Math.min(Math.max(1, implicitWidth), Math.max(0, ShellSurfaceRoot.screenWidth - ShellSurfaceRoot.safeLeft - ShellSurfaceRoot.safeRight))
  height: Math.min(Math.max(1, implicitHeight), Math.max(0, ShellSurfaceRoot.screenHeight - ShellSurfaceRoot.safeTop - ShellSurfaceRoot.safeBottom))
  visible: visibleRequested

  Rectangle { anchors.fill: parent; color: root.color; z: -1 }

  Component.onCompleted: {
    var host = ShellSurfaceRoot.layerFor(3)   // overlay
    if (host) parent = host
    reanchor()
  }

  onVisibleChanged: if (visible) reanchor()
  onVisibleRequestedChanged: if (visibleRequested) reanchor()
  onAnchorChanged: reanchor()

  Connections {
    target: ShellSurfaceRoot
    function onSafeTopChanged() { if (root.visible) root.reanchor() }
    function onSafeBottomChanged() { if (root.visible) root.reanchor() }
    function onScreenWidthChanged() { if (root.visible) root.reanchor() }
    function onScreenHeightChanged() { if (root.visible) root.reanchor() }
  }

  function reanchor() {
    if (!anchor) return
    anchor.updateAnchor()

    var px = anchor.rect.x + relativeX
    var py = anchor.rect.y + relativeY

    // The anchor rect is in the anchor item's window; inside one Android
    // window that is already shell-window space, so only clamping is left.
    // Clamped on both axes whatever the positioner asked for: upstream's
    // adjustment flags describe which way a compositor may slide a popup on a
    // large screen, but on a phone there is nowhere else for it to go, and an
    // unclamped popup simply leaves the display.
    var maxX = Math.max(0, ShellSurfaceRoot.screenWidth - ShellSurfaceRoot.safeRight - width)
    var maxY = Math.max(0, ShellSurfaceRoot.screenHeight - ShellSurfaceRoot.safeBottom - height)
    px = Math.max(ShellSurfaceRoot.safeLeft, Math.min(px, maxX))
    py = Math.max(ShellSurfaceRoot.safeTop, Math.min(py, maxY))

    root.x = px
    root.y = py
  }
}
