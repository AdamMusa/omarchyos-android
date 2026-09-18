pragma Singleton
import QtQuick
// Android grants privilege through its own permission dialogs; the shell's
// polkit agent stays inert but present so upstream QML loads unchanged.
QtObject { readonly property bool registered: false; readonly property var pendingRequest: null }
