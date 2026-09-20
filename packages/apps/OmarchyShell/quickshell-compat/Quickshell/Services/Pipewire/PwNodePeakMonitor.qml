import QtQuick

// Upstream watches a PipeWire node's peak level to draw a live input meter.
// Android exposes no per-stream peak to an app: MediaRecorder's amplitude is
// only for a recording this app owns, and reading it would mean holding the
// microphone open while a panel is on screen. The monitor therefore reports a
// flat zero, which upstream renders as an idle meter, and the panel's other
// controls (device choice, volume, mute) work normally.
QtObject {
  id: root

  property var node: null
  property bool enabled: false

  readonly property real peak: 0
  readonly property bool ready: false
}
