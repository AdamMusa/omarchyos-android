import QtQuick
import Quickshell.Wayland

// Upstream PanelWindow is a Wayland layer-shell surface: pinned to screen
// edges, with an exclusive zone other windows must not cover. Android gives an
// app a single window, so a panel is a surface Item inside it.
//
// The type itself stays non-visual: Item.anchors is FINAL, and upstream needs
// `anchors { top: true; left: true; right: true }` to keep its layer-shell
// meaning. Content is held by `surface` from the start, so widgets created
// later by Repeaters and Loaders land there too.
QtObject {
  id: root

  property PanelAnchors anchors: PanelAnchors {}
  property PanelMargins margins: PanelMargins {}
  property int exclusiveZone: 0
  property int exclusionMode: 0
  property var screen: Qt.application.screens.length > 0 ? Qt.application.screens[0] : null
  property string namespaceName: "omarchy-shell"
  property var mask: null
  property int focusable: 0
  property SurfaceFormat surfaceFormat: SurfaceFormat {}
  property bool aboveWindows: true
  // Upstream parks a layer surface's rendering with this while it is hidden.
  // The Android host composites every surface inside one window, so parking is
  // just hiding: the flag is honoured by folding it into the surface's
  // visibility rather than by stopping a per-surface renderer.
  property bool updatesEnabled: true
  property color color: "transparent"
  property bool visible: true
  property real implicitWidth: 0
  property real implicitHeight: 0

  readonly property bool replacedBySystemBar: AndroidBridge.nativeSystemBar
      && root.WlrLayershell.namespace === "omarchy-bar"

  readonly property real spanWidth: ShellSurfaceRoot.screenWidth
  readonly property real spanHeight: ShellSurfaceRoot.screenHeight
  readonly property bool spansX: anchors.left && anchors.right
  readonly property bool spansY: anchors.top && anchors.bottom
  readonly property real width: surface ? surface.width : 0
  readonly property real height: surface ? surface.height : 0

  readonly property Item surface: Item {
    visible: root.visible && root.updatesEnabled && !root.replacedBySystemBar
    // A panel that does not span an axis is sized by its content, which
    // upstream lays out for a desktop; on a phone that regularly exceeds the
    // display, so the content size is capped to what is actually available.
    // Wallpaper stays edge to edge; interactive panels respect the OS bar,
    // gesture area and cutouts without adding the same margin twice.
    readonly property bool safeContent: !root.replacedBySystemBar && root.WlrLayershell.layer !== WlrLayer.Background
    readonly property real leftMargin: Math.max(root.margins.left, safeContent ? ShellSurfaceRoot.safeLeft : 0)
    readonly property real rightMargin: Math.max(root.margins.right, safeContent ? ShellSurfaceRoot.safeRight : 0)
    readonly property real topMargin: Math.max(root.margins.top, safeContent ? ShellSurfaceRoot.safeTop : 0)
    readonly property real bottomMargin: Math.max(root.margins.bottom, safeContent ? ShellSurfaceRoot.safeBottom : 0)
    readonly property real availableWidth: root.spanWidth - leftMargin - rightMargin
    readonly property real availableHeight: root.spanHeight - topMargin - bottomMargin

    width: root.spansX ? availableWidth
                       : Math.min(Math.max(1, root.implicitWidth), availableWidth)
    height: root.replacedBySystemBar ? 0 : root.spansY ? availableHeight
                        : Math.min(Math.max(1, root.implicitHeight), availableHeight)
    // Kept on screen on both axes: a surface placed off the display is simply
    // invisible, and there is no second monitor to slide onto.
    x: Math.max(leftMargin,
                Math.min(root.anchors.right && !root.spansX
                           ? root.spanWidth - width - rightMargin
                           : leftMargin,
                         root.spanWidth - width - rightMargin))
    y: Math.max(topMargin,
                Math.min(root.anchors.bottom && !root.spansY
                           ? root.spanHeight - height - bottomMargin
                           : topMargin,
                         root.spanHeight - height - bottomMargin))

    Rectangle { anchors.fill: parent; color: root.color; z: -1 }
  }

  // Everything declared inside the panel belongs to the surface.
  default property alias data: root.surface.data

  readonly property Item contentItem: root.surface

  // Quickshell exposes whether the surface's backing window is on screen. The
  // Android host composites every surface into one window, so the surface's
  // own visibility is the honest answer.
  readonly property bool backingWindowVisible: root.surface ? root.surface.visible : false

  Component.onCompleted: {
    var shellProps = root.WlrLayershell
    var host = ShellSurfaceRoot.layerFor(shellProps ? shellProps.layer : 2)
    if (!host) {
      console.warn("omarchy-shell: no surface host for", namespaceName)
      return
    }
    surface.parent = host
    publishInsets()
  }

  onVisibleChanged: publishInsets()
  onExclusiveZoneChanged: publishInsets()
  onExclusionModeChanged: publishInsets()

  // Ignore-mode surfaces reserve nothing, exactly as on the desktop.
  function publishInsets() {
    var shellProps = root.WlrLayershell
    AndroidBridge.setShellInset((shellProps && shellProps.namespace) || namespaceName,
        anchors.top ? "top" : anchors.bottom ? "bottom" : "none",
        (replacedBySystemBar || exclusionMode === 1 || !visible) ? 0 : exclusiveZone,
        shellProps ? shellProps.layer : 2, shellProps ? shellProps.keyboardFocus : 0)
  }
}
