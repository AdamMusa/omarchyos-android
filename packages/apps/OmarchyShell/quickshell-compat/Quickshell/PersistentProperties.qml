import QtQuick

// Upstream keeps these values across shell reloads. The Android host persists
// them in the app's data dir through AndroidBridge so a shell restart (or an
// OTA) does not reset toggles like do-not-disturb.
QtObject {
  id: root
  // Quickshell names this reloadableId; the earlier compat spelling stays as
  // an alias so nothing that used it breaks.
  property string reloadableId: ""
  property string reloadKey: ""
  readonly property string storeKey: reloadableId || reloadKey
  Component.onCompleted: AndroidBridge.restoreProperties(storeKey, root)
  Component.onDestruction: AndroidBridge.saveProperties(storeKey, root)
}
