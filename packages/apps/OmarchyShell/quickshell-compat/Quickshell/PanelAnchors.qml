import QtQuick
// Which screen edges a panel is pinned to. Two opposite edges mean the panel
// spans that axis, which is how Omarchy's bar decides its orientation.
QtObject {
  property bool top: false
  property bool bottom: false
  property bool left: false
  property bool right: false
}
