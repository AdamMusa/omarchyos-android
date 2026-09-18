import QtQuick
import QtQuick.Window
import QtQuick.Window as QW

// Upstream anchors popups to a parent layer-shell surface. On Android the
// shell owns one overlay window per popup, positioned against the anchor
// item's screen coordinates so menus and tooltips land where the bar expects.
Window {
  id: root

  // Grouped anchor description, same shape as upstream's PopupAnchor.
  property PopupAnchor anchor: PopupAnchor {}
  property bool visibleRequested: false
  property var parentWindow: null
  property int relativeX: 0
  property int relativeY: 0
  // Upstream sizes popups from their content's implicit size; Window has no
  // implicit size of its own, so mirror it here and drive width/height from it.
  property real implicitWidth: 0
  property real implicitHeight: 0
  width: implicitWidth > 0 ? implicitWidth : 1
  height: implicitHeight > 0 ? implicitHeight : 1

  flags: Qt.Popup | Qt.FramelessWindowHint
  color: "transparent"
  visible: visibleRequested

  // Resolve the anchor into a screen position. Upstream fills anchor.rect in
  // its onAnchoring handler, so ask for that first, then place the window
  // against the anchor window's origin and keep it on screen.
  function reanchor() {
    if (!anchor) return
    anchor.updateAnchor()

    var origin = Qt.point(0, 0)
    var host = anchor.window || parentWindow
    if (host) origin = Qt.point(host.x, host.y)
    else if (anchor.item && anchor.item.mapToGlobal) origin = anchor.item.mapToGlobal(0, 0)

    var px = origin.x + anchor.rect.x + relativeX
    var py = origin.y + anchor.rect.y + relativeY

    var screenW = Screen.width
    var screenH = Screen.height
    if (anchor.adjustment & 1) px = Math.max(0, Math.min(px, screenW - width))
    if (anchor.adjustment & 2) py = Math.max(0, Math.min(py, screenH - height))

    root.x = px
    root.y = py
  }

  onAnchorChanged: reanchor()
  onVisibleRequestedChanged: if (visibleRequested) reanchor()
  onVisibleChanged: if (visible) reanchor()
}
