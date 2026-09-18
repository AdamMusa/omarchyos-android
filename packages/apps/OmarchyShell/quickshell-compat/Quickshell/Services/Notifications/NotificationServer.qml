pragma Singleton
import QtQuick

// Upstream runs its own notification daemon. Android already owns delivery, so
// the shell listens through a NotificationListenerService and renders the same
// popups and history.
QtObject {
  id: root
  property bool imageSupported: true
  property bool bodyMarkupSupported: true
  property bool actionsSupported: true
  readonly property var trackedNotifications: AndroidBridge.notifications
  signal notification(var notif)
  property Connections _c: Connections {
    target: AndroidBridge
    function onNotificationPosted(n) { root.notification(n) }
  }
}
