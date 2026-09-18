import QtQuick
QtObject {
  property string appName: ""
  property string summary: ""
  property string body: ""
  property string image: ""
  property int urgency: 1
  property var actions: []
  function dismiss() { AndroidBridge.dismissNotification(key) }
  property string key: ""
}
