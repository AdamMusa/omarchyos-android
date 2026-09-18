import QtQuick

// Entry point of the shell process: it loads Omarchy's own shell.qml, which
// brings up the bar, menu, panels, notifications and lock surfaces. The path
// comes from ShellPaths so the same binary runs against the vendored tree on a
// build host and against /product/etc/omarchy on the device.
Item {
  id: root

  Loader {
    id: shellLoader
    source: ShellPaths.shellQmlUrl
    asynchronous: false
    onStatusChanged: if (status === Loader.Error) console.error("omarchy-shell: upstream shell.qml failed to load")
  }
}
