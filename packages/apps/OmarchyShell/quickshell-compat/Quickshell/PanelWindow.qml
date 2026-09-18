import QtQuick
import QtQuick.Window

// Upstream PanelWindow is a Wayland layer-shell surface anchored to screen
// edges with an exclusion zone. Android has no layer shell: the shell process
// instead owns application-overlay windows, and the exclusion zone becomes the
// system-bar inset the shell publishes back to WindowManager.
Window {
  id: root

  property PanelAnchors anchors: PanelAnchors {}
  property int exclusiveZone: 0
  property string exclusionMode: "normal"
  property PanelMargins margins: PanelMargins {}
  property var screen: Qt.application.screens.length > 0 ? Qt.application.screens[0] : null
  property string namespaceName: "omarchy-shell"
  property var mask: null
  property int focusable: 0
  // Upstream lets a surface request a GL surface format (Omarchy asks for an
  // alpha channel so the bar can be transparent). Android composites shell
  // windows with alpha already, so this is accepted and ignored.
  property SurfaceFormat surfaceFormat: SurfaceFormat {}
  property bool aboveWindows: true
  // Upstream exposes the backing screen's logical size to children.
  readonly property real screenWidth: screen ? screen.width : Screen.width
  readonly property real screenHeight: screen ? screen.height : Screen.height

  // Android draws shell surfaces edge-to-edge; anchors decide which edge the
  // window hugs and how wide it spans.
  readonly property bool horizontal: (anchors.left && anchors.right)
  readonly property bool vertical: (anchors.top && anchors.bottom)

  flags: Qt.Window | Qt.FramelessWindowHint
  color: "transparent"
  visible: true

  // Upstream sizes a panel from its implicit size on the axis it does not
  // span, and stretches along the axis its anchors cover.
  property real implicitWidth: 0
  property real implicitHeight: 0
  readonly property real spanWidth: screen ? screen.width : Screen.width
  readonly property real spanHeight: screen ? screen.height : Screen.height
  width: (anchors.left && anchors.right) ? spanWidth : Math.max(1, implicitWidth)
  height: (anchors.top && anchors.bottom) ? spanHeight : Math.max(1, implicitHeight)

  onExclusiveZoneChanged: AndroidBridge.setShellInset(namespaceName,
      anchors.top ? "top" : anchors.bottom ? "bottom" : "none", exclusiveZone)
}
