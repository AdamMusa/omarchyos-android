import QtQuick

// Upstream's PopupAnchor: which window and rectangle a popup hangs off, which
// of its edges meet, and how it may be nudged to stay on screen. Android has no
// xdg-positioner, so the same rules are applied here in QML and the result is a
// plain screen position for the popup window.
QtObject {
  id: root

  property var window: null
  property var item: null
  property AnchorRect rect: AnchorRect {}
  property int edges: 0
  property int gravity: 0
  property int adjustment: 0
  property point anchorpoint: Qt.point(0, 0)

  // Emitted before the popup is positioned; upstream QML fills in rect here.
  signal anchoring()

  function updateAnchor() { anchoring() }
}
