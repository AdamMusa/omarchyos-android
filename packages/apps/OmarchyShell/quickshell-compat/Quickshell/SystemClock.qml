import QtQuick

// Ticking clock with the same precision enum upstream exposes.
QtObject {
  id: root
  enum Precision { Hours, Minutes, Seconds }
  property int precision: 1
  property date date: new Date()
  readonly property int hours: date.getHours()
  readonly property int minutes: date.getMinutes()
  readonly property int seconds: date.getSeconds()
  property bool enabled: true

  property Timer _timer: Timer {
    interval: root.precision === 2 ? 1000 : 1000 * 20
    running: root.enabled
    repeat: true
    onTriggered: root.date = new Date()
  }
}
