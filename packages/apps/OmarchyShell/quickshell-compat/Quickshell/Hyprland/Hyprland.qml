pragma Singleton
import QtQuick

// Omarchy's bar shows Hyprland workspaces and dispatches window commands.
// Android has no workspaces; the nearest real concept is the recents task
// stack, so each running task appears as a workspace and dispatch() maps the
// handful of commands the bar issues onto ActivityManager actions.
QtObject {
  id: root

  property var workspaces: AndroidBridge.recentTasks()
  property var focusedWorkspace: workspaces.length > 0 ? workspaces[0] : null
  property var monitors: []
  property var focusedMonitor: null

  function dispatch(request) {
    var parts = ("" + request).split(" ")
    switch (parts[0]) {
    case "workspace":
      AndroidBridge.moveToTask(parseInt(parts[1], 10)); return
    case "killactive":
      AndroidBridge.closeForegroundTask(); return
    case "exec":
      AndroidBridge.execDetached(parts.slice(1).join(" ")); return
    default:
      AndroidBridge.hyprlandCommand(request)
    }
  }

  function refreshWorkspaces() { workspaces = AndroidBridge.recentTasks() }

  property Connections _tasks: Connections {
    target: AndroidBridge
    function onTasksChanged() { root.refreshWorkspaces() }
  }
}
