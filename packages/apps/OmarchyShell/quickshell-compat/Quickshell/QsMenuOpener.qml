import QtQuick
// Opens a tray item's own menu. Android foreground services have no menus, so
// the opener exposes an empty model and the bar renders nothing for it.
QtObject {
  property var menu: null
  readonly property var children: []
}
