import QtQuick

// Upstream's session lock. On Android the real lock is the keyguard, so this
// asks the host to show the shell's lock surface above the keyguard and to
// hand authentication to BiometricPrompt/KeyguardManager.
QtObject {
  id: root

  property bool locked: false
  property Component surface: null

  // True once the compositor (here: the keyguard) has confirmed the screen is
  // actually covered. Upstream will not start authentication before it is.
  readonly property bool secure: locked

  // Quickshell emits this when the compositor acknowledges a lock or unlock,
  // which is a different moment from the property assignment: upstream uses it
  // to close panels and start the fingerprint reader.
  signal lockStateChanged()

  // Quickshell reports the compositor confirming the screen is covered
  // separately from the lock request itself; upstream starts the fingerprint
  // reader from here, so the two must stay distinct signals.
  signal secureStateChanged()

  onLockedChanged: {
    AndroidBridge.setLockVisible(locked)
    root.lockStateChanged()
    root.secureStateChanged()
  }
}
