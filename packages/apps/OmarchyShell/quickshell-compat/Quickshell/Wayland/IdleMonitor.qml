import QtQuick

// Upstream's idle monitor is the Wayland idle-notify protocol: it reports when
// the seat has seen no input for `timeout` seconds, and idle inhibitors held by
// applications suppress it. Android has no seat protocol, so the host tracks
// input to the shell window and the display's own interactive state; the two
// together are what "the user is not here" means on a phone.
QtObject {
  id: root

  property bool enabled: true
  property int timeout: 0
  property bool respectInhibitors: true

  // The screen going off is idleness whatever the timer says: Android may dim
  // and sleep well before Omarchy's own screensaver timeout elapses.
  readonly property bool isIdle: enabled && timeout > 0
                                 && (!AndroidBridge.screenOn
                                     || (AndroidBridge.idleSeconds >= timeout
                                         && !(respectInhibitors && AndroidBridge.idleInhibited)))
}
