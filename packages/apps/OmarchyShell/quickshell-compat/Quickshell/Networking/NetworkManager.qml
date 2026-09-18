pragma Singleton
import QtQuick
// Wi-Fi/cellular state for the bar widget and the network panel.
QtObject {
  readonly property string state: AndroidBridge.networkType
  readonly property bool wifiEnabled: AndroidBridge.networkType === "wifi"
  readonly property var activeConnection: QtObject {
    readonly property string id: AndroidBridge.wifiSsid
    readonly property int strength: AndroidBridge.wifiSignal
  }
  readonly property var accessPoints: AndroidBridge.wifiNetworks
  function connect(ssid, password) { AndroidBridge.connectWifi(ssid, password) }
  function setWifiEnabled(on) { AndroidBridge.setWifiEnabled(on) }
}
