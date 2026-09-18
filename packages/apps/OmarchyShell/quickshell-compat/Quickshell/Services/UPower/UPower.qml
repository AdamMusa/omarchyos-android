pragma Singleton
import QtQuick

// Battery + power state, from BatteryManager instead of UPower.
QtObject {
  id: root
  readonly property QtObject displayDevice: QtObject {
    readonly property real percentage: AndroidBridge.batteryPercent / 100
    readonly property bool isLaptopBattery: true
    readonly property bool ready: true
    readonly property int state: AndroidBridge.charging ? 1 : 2   // charging : discharging
    readonly property real timeToEmpty: AndroidBridge.batteryTimeToEmpty
    readonly property real timeToFull: AndroidBridge.batteryTimeToFull
    readonly property real changeRate: 0
    readonly property string iconName: AndroidBridge.charging ? "battery-charging" : "battery"
  }
  readonly property bool onBattery: !AndroidBridge.charging
  readonly property var devices: [displayDevice]
}
