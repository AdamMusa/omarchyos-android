import QtQuick

// Upstream registers a polkit agent on the session bus and drives its own
// password dialog from the flow it hands back. Android authorises through the
// keyguard and BiometricPrompt instead, so the agent is present and creatable
// — upstream instantiates it — but never registers, and `flow` stays null so
// the dialog keeps itself hidden.
QtObject {
  id: root

  property string path: ""
  readonly property bool isRegistered: false
  readonly property bool isActive: false
  readonly property var flow: null

  signal authenticationRequestStarted()

  function cancelAuthenticationRequest() {}
}
