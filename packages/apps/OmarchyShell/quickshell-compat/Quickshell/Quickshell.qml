pragma Singleton
import QtQuick

// Upstream's `Quickshell` singleton: process environment, screens, and the
// clipboard/data dirs the shell reads at startup.
QtObject {
  id: root

  // ShellPaths (C++) resolves the Android equivalents of $HOME, $OMARCHY_PATH
  // and the state dir, so env() answers what upstream expects to find.
  readonly property var screens: [Qt.application.screens.length > 0
      ? Qt.application.screens[0] : null].filter(s => s !== null)

  function env(name) {
    switch (name) {
    case "OMARCHY_PATH": return ShellPaths.omarchyPath
    case "HOME": return ShellPaths.home
    case "XDG_CONFIG_HOME": return ShellPaths.configHome
    case "XDG_STATE_HOME": return ShellPaths.stateHome
    default: return ""
    }
  }

  function dataPath(rest) { return ShellPaths.home + "/.local/share/" + (rest || "") }
  function statePath(rest) { return ShellPaths.stateHome + "/" + (rest || "") }
  function configPath(rest) { return ShellPaths.configHome + "/" + (rest || "") }

  function execDetached(cmd) { AndroidBridge.execDetached(cmd) }
  function reload() { /* the Android host reloads by restarting the QML engine */ }
}
