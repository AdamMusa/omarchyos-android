pragma Singleton
import QtQuick
// Android has no StatusNotifierItem tray; foreground-service notifications are
// the closest thing, and the bar renders them in the tray slot.
QtObject { readonly property var items: AndroidBridge.foregroundServices }
