pragma Singleton
import QtQuick

// Upstream reads .desktop files. The Android equivalent is the launcher
// activity list, shaped into the same fields the app library and the menu use:
// id, name, icon, comment, categories and an execute() that launches it.
QtObject {
  id: root

  readonly property QsObjectModel applications: QsObjectModel {
    values: AndroidBridge.installedApps()
  }

  property Connections _packages: Connections {
    target: AndroidBridge
    function onInstalledAppsChanged() {
      root.applications.values = AndroidBridge.installedApps()
    }
  }

  function byId(id) {
    var apps = applications.values
    for (var i = 0; i < apps.length; i++)
      if (apps[i].id === id) return apps[i]
    return null
  }

  function heuristicLookup(name) {
    var direct = byId(name)
    if (direct) return direct
    var needle = ("" + name).toLowerCase()
    var apps = applications.values
    for (var i = 0; i < apps.length; i++)
      if (("" + apps[i].name).toLowerCase() === needle) return apps[i]
    return null
  }
}
