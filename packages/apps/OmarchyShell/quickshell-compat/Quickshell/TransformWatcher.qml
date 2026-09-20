import QtQuick

// Upstream watches the transform between two items so a popup can re-anchor
// when anything between them moves. Quickshell hooks the scene graph for this;
// here the mapped origin is sampled instead, which is cheap enough for the
// handful of anchored surfaces the shell has and needs no private API.
QtObject {
  id: root

  property Item a: null
  property Item b: null

  // Bumped whenever the mapping between a and b changes. Upstream reads it as
  // a binding dependency, so the value itself carries no meaning.
  property int transform: 0

  property point lastOrigin: Qt.point(NaN, NaN)
  property real lastScale: NaN

  function sample() {
    if (!a || !b) return
    var p = a.mapToItem(b, 0, 0)
    // Size changes move anchored surfaces just as position changes do.
    var s = a.width * 100000 + a.height
    if (p.x !== lastOrigin.x || p.y !== lastOrigin.y || s !== lastScale) {
      lastOrigin = p
      lastScale = s
      transform++
    }
  }

  property Timer sampler: Timer {
    interval: 50
    repeat: true
    running: !!root.a && !!root.b
    onTriggered: root.sample()
  }

  onAChanged: sample()
  onBChanged: sample()
}
