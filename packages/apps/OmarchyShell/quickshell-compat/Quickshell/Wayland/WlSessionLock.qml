import QtQuick
// Upstream's session lock. On Android the real lock is the keyguard, so this
// asks the host to show the shell's lock surface above the keyguard and to
// hand authentication to BiometricPrompt/KeyguardManager.
QtObject {
  id: root
  property bool locked: false
  property Component surface: null
  onLockedChanged: AndroidBridge.setLockVisible(locked)
}
