import QtQuick
import QtQuick.Window
import Quickshell

// The shell's single Android window. Every Omarchy surface — bar, panels,
// popups, notifications, lock — is composited inside it, layered the way the
// Wayland compositor would stack them.
Window {
  id: window

  visible: true
  // Not Window.FullScreen: on Android that asks the platform to change the
  // window's visibility state, and the activity is already laid out edge to
  // edge by its theme. Letting Qt keep the plain visible state keeps the
  // QtSurface and the QQuickWindow in step.
  width: Screen.width
  height: Screen.height
  // Nothing of the shell's own is behind Omarchy's background plugin, so the
  // clear colour only shows before the wallpaper surface exists.
  color: "#101010"
  title: "Omarchy"

  // Only release Android's boot curtain after the shell's own surfaces exist.
  property bool shellContentReady: false
  Timer {
    interval: 32
    running: !window.shellContentReady
    repeat: true
    onTriggered: {
      var shell = shellLoader.item
      if (shell && shell.bar && shell.pluginRegistry && !shell.pluginRegistry.scanning
          && ShellSurfaceRoot.backgroundReady) {
        window.shellContentReady = true
        window.requestUpdate()
      }
    }
  }

  Item {
    id: surfaceContainer
    anchors.fill: parent
  }

  // Startup marker: drawn under every shell surface, so a blank screen can be
  // told apart from a window that never painted.
  Loader {
    id: shellLoader
    active: false
    source: ShellPaths.shellQmlUrl
    onStatusChanged: if (status === Loader.Error) console.error("omarchy-shell: upstream shell.qml failed to load")
  }

  // Startup report: which widgets registered and what the shell actually put
  // on screen. Without it an empty bar is indistinguishable from a bar that
  // never got its plugins.
  Timer {
    interval: 6000
    running: true
    repeat: false
    onTriggered: {
      console.info("omarchy-shell: window " + window.width + "x" + window.height
                   + " visible=" + window.visible + " vis=" + window.visibility
                   + " dpr=" + Screen.devicePixelRatio
                   + " | contentItem " + window.contentItem.width + "x" + window.contentItem.height
                   + " children=" + window.contentItem.children.length
                   + " | container " + surfaceContainer.width + "x" + surfaceContainer.height
                   + " visible=" + surfaceContainer.visible
                   + " opacity=" + surfaceContainer.opacity
                   + " children=" + surfaceContainer.children.length
                   + " | loader status=" + shellLoader.status
                   + " item=" + (shellLoader.item ? "yes" : "null"))
      var shell = shellLoader.item
      if (!shell) { console.warn("omarchy-shell: shell root missing"); return }
      var registry = shell.barWidgetRegistry
      console.info("omarchy-shell: bar widgets:",
                   registry ? JSON.stringify(Object.keys(registry.widgets)) : "none")
      var plugins = shell.pluginRegistry
      console.info("omarchy-shell: plugins:",
                   plugins && plugins.installedPlugins
                     ? JSON.stringify(Object.keys(plugins.installedPlugins)) : "none")
      // Theme state: these come from Omarchy's own colors.toml / shell.toml,
      // so wrong values here mean the theme files did not parse.
      try {
        var probe = Qt.createQmlObject(
          'import QtQuick; import qs.Commons; QtObject {\n' +
          '  readonly property string report: "bg=" + Color.background + " fg=" + Color.foreground\n' +
          '    + " accent=" + Color.accent + " barSize=" + Style.bar.sizeHorizontal\n' +
          '    + " font=" + Style.fontFamily + " radius=" + Style.radius\n' +
          '}', window, "themeProbe")
        console.info("omarchy-shell: theme " + probe.report)
      } catch (e) {
        console.warn("omarchy-shell: theme probe failed: " + e)
      }

      if (!reportWallpaper(window.contentItem))
        console.warn("omarchy-shell: no wallpaper image in the scene")

      // Grab what the scene actually paints. A grab that shows the bar while
      // the device screen is black separates a QML problem from a compositing
      // one.
      for (var layer in ShellSurfaceRoot.layerItems) {
        var host = ShellSurfaceRoot.layerItems[layer]
        console.info("omarchy-shell: " + layer + " " + host.width + "x" + host.height
                     + " visible=" + host.visible + " opacity=" + host.opacity
                     + " window=" + (host.Window.window ? "yes" : "NONE")
                     + " parentIsContainer=" + (host.parent === surfaceContainer)
                     + " surfaces=" + host.children.length)
        for (var i = 0; i < host.children.length; i++) {
          var s = host.children[i]
          console.info("    surface " + i + " " + s.width + "x" + s.height
                       + " @" + s.x + "," + s.y + " visible=" + s.visible
                       + " children=" + s.children.length
                       + " window=" + (s.Window.window ? "yes" : "NONE")
                       + " parent=" + (s.parent ? s.parent.objectName || "unnamed" : "null"))
          if (!s.visible) continue
          describe(s, "      ", 3)
        }
      }
    }
  }

  // The wallpaper is a plain Image inside the background plugin's surface;
  // reporting its source and load status separates "no wallpaper was chosen"
  // from "the chosen file would not decode".
  property int wallpapersReported: 0
  function reportWallpaper(root) {
    for (var i = 0; i < root.children.length; i++) {
      var c = root.children[i]
      if (String(c).indexOf("QQuickImage") === 0 && c.width > 100 && wallpapersReported < 4) {
        wallpapersReported++
        console.info("omarchy-shell: image " + Math.round(c.width) + "x" + Math.round(c.height)
                     + " status=" + c.status + " visible=" + c.visible
                     + " source=" + String(c.source).slice(-60))
      }
      reportWallpaper(c)
    }
    return wallpapersReported > 0
  }

  // Walks a visible surface so an empty-looking bar can be told apart from a
  // bar whose widgets are sized to nothing.
  function describe(item, indent, depth) {
    if (depth <= 0) return
    for (var i = 0; i < item.children.length; i++) {
      var c = item.children[i]
      console.info(indent + (c.objectName || c.toString().split("(")[0])
                   + " " + Math.round(c.width) + "x" + Math.round(c.height)
                   + " @" + Math.round(c.x) + "," + Math.round(c.y)
                   + " visible=" + c.visible + " opacity=" + c.opacity)
      describe(c, indent + "  ", depth - 1)
    }
  }

  Component.onCompleted: {
    // The surface root has to exist before upstream's surfaces are built.
    ShellSurfaceRoot.container = surfaceContainer
    shellLoader.active = true
  }
}
