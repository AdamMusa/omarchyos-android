pragma Singleton
import QtQuick

// Media controls. MediaSessionManager gives the same fields MPRIS does, so the
// bar's media widget and the lock-screen controls work unchanged.
QtObject {
  id: root
  readonly property var players: AndroidBridge.mediaSessions
  readonly property var activePlayer: players.length > 0 ? players[0] : null
}
