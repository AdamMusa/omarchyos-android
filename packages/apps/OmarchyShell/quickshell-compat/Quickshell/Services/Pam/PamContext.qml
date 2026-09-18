import QtQuick

// Lock-screen authentication. PAM's conversation maps onto BiometricPrompt and
// KeyguardManager: respond() forwards a typed password, start() triggers the
// fingerprint/biometric flow.
QtObject {
  id: root
  property string config: ""
  property string configDirectory: ""
  property string user: ""
  property bool active: false
  signal completed(int result)
  signal pamMessage(string message, bool isError, bool responseRequired)

  function start() { AndroidBridge.beginAuth(root) }
  function abort() { AndroidBridge.cancelAuth() }
  function respond(response) { AndroidBridge.submitPassword(response) }
}
