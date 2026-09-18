pragma Singleton
import QtQuick
import Quickshell

// Upstream tracks Wayland toplevels (open windows) to tell when a launch has
// actually produced a window. Android's equivalent is the task list, so each
// running task appears as a toplevel with its app id, title and activation.
QtObject {
  id: root

  readonly property QsObjectModel toplevels: QsObjectModel {
    values: AndroidBridge.recentTasks()
  }
  readonly property var activeToplevel: toplevels.values.length > 0 ? toplevels.values[0] : null

  property Connections _tasks: Connections {
    target: AndroidBridge
    function onTasksChanged() { root.toplevels.values = AndroidBridge.recentTasks() }
  }
}
