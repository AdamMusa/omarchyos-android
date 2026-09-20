import QtQuick
import Quickshell
import "AppSearch.js" as AppSearch

// Android's launcher activities are the entire app catalog. No .desktop scan,
// Linux icon search, package-manager subprocess or launch-feedback polling.
Item {
  id: root
  signal appsChanged()
  function entryName(entry) { return AppSearch.entryName(entry) }
  function entrySubtext(entry) { return "" }
  function sortedEntries(query) {
    return AppSearch.sortedEntries(DesktopEntries.applications.values || [], query, function(entry) {
      return entry.id === "os.omarchy.shell"
    })
  }
  function iconSource(icon) { return AndroidBridge.iconUrl(String(icon || "")) }
  function refreshIcons() { }
  function launch(id, name) { AndroidBridge.launchApp(id) }
  function remove(id, name) { AndroidBridge.openAppInfo(id) }
  Connections {
    target: AndroidBridge
    function onInstalledAppsChanged() { root.appsChanged() }
  }
}
