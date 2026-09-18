pragma Singleton
import QtQuick
QtObject {
  readonly property bool enabled: AndroidBridge.bluetoothEnabled
  readonly property var devices: AndroidBridge.bluetoothDevices
  function setEnabled(on) { AndroidBridge.setBluetoothEnabled(on) }
}
