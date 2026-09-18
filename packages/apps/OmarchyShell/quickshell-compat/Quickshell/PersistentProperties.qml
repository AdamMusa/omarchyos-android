import QtQuick

// Upstream keeps these values across shell reloads. The Android host persists
// them in the app's data dir through AndroidBridge so a shell restart (or an
// OTA) does not reset toggles like do-not-disturb.
QtObject {
  id: root
  property string reloadKey: ""
  Component.onCompleted: AndroidBridge.restoreProperties(reloadKey, root)
  Component.onDestruction: AndroidBridge.saveProperties(reloadKey, root)
}
