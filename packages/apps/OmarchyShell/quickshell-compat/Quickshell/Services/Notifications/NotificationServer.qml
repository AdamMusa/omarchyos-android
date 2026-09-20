import QtQuick

// Upstream runs its own freedesktop notification daemon and is handed each
// notification as it arrives. Android already owns delivery, so the server is
// a relay: NotificationBridge (a NotificationListenerService) reports what the
// system posted, and the same signal upstream expects is emitted here.
//
// Creatable rather than a singleton, because upstream instantiates it.
QtObject {
  id: root

  property bool keepOnReload: false
  property bool imageSupported: false
  property bool actionsSupported: false
  property bool actionIconsSupported: false
  property bool bodyMarkupSupported: false
  property bool bodyHyperlinksSupported: false
  property bool bodyImagesSupported: false
  property bool persistenceSupported: false
  property bool inlineReplySupported: false

  readonly property var trackedNotifications: ({ values: [] })

  signal notification(var notification)

  property Connections bridge: Connections {
    target: AndroidBridge
    function onNotificationPosted(posted) {
      root.notification(posted)
    }
  }
}
