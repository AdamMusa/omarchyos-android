import QtQuick
import QtQuick.Controls
import QtQuick.Layouts
import Quickshell
import Quickshell.Wayland
import qs.Commons

Item {
  id: root
  property var shell: null
  property bool opened: false
  property bool marketplace: false
  readonly property var state: AndroidBridge.themeState
  function syncPalette() {
    if (state.palette) { Color.loadColors(state.palette); Color.loadShell(""); Style.scheduleRefresh() }
  }
  Component.onCompleted: syncPalette()
  Connections {
    target: AndroidBridge
    function onThemeStateChanged() { root.syncPalette() }
    function onThemeBrowserRequested() { root.opened = true; Qt.callLater(function() { content.forceActiveFocus() }) }
  }
  function installed(id) {
    var themes = state.installed || []
    for (var i = 0; i < themes.length; i++) if (themes[i].id === id) return true
    return false
  }
  readonly property var rows: {
    var all = marketplace ? (state.marketplace || []) : (state.installed || [])
    var query = (search.text.slice(0, search.cursorPosition) + search.preeditText + search.text.slice(search.cursorPosition)).toLowerCase().trim()
    return all.filter(function(item) { return !query || item.name.toLowerCase().indexOf(query) >= 0 })
  }
  component Action: Button {
    id: action
    implicitHeight: 48
    contentItem: Text { text: action.text; color: action.enabled ? Color.foreground : Color.muted; font.pixelSize: 14; font.bold: action.highlighted; horizontalAlignment: Text.AlignHCenter; verticalAlignment: Text.AlignVCenter; elide: Text.ElideRight }
    background: Rectangle { color: action.down ? Color.accent : Color.background; border.color: Color.accent; border.width: action.highlighted ? 2 : 1; radius: 6; opacity: action.enabled ? 1 : .45 }
  }
  PanelWindow {
    visible: root.opened
    anchors { top: true; bottom: true; left: true; right: true }
    exclusionMode: ExclusionMode.Ignore
    WlrLayershell.layer: WlrLayer.Overlay
    WlrLayershell.keyboardFocus: WlrKeyboardFocus.Exclusive
    color: Color.background
    FocusScope {
      id: content
      anchors.fill: parent
      focus: root.opened
      Keys.onPressed: function(event) {
        if (event.key === Qt.Key_Back || event.key === Qt.Key_Escape) {
          Qt.inputMethod.hide(); root.opened = false; event.accepted = true
        }
      }
      ColumnLayout {
        anchors.fill: parent
        anchors.margins: 16
        spacing: 12
        RowLayout {
          Layout.fillWidth: true
          Text { text: "Themes"; color: Color.foreground; font.pixelSize: 26; font.bold: true; Layout.fillWidth: true }
          Action { text: "Close"; Layout.preferredWidth: 72; onClicked: { Qt.inputMethod.hide(); root.opened = false } }
        }
        Text { text: "Omarchy, made yours"; color: Color.foreground; opacity: .7; font.pixelSize: 14 }
        RowLayout {
          Layout.fillWidth: true
          Action { text: "Installed (" + (root.state.installed || []).length + ")"; Layout.fillWidth: true; highlighted: !root.marketplace; onClicked: { root.marketplace = false; list.positionViewAtBeginning() } }
          Action { text: "Marketplace"; Layout.fillWidth: true; highlighted: root.marketplace; onClicked: { root.marketplace = true; list.positionViewAtBeginning() } }
        }
        TextField {
          id: search
          Layout.fillWidth: true
          Layout.preferredHeight: 48
          placeholderText: "Search themes"
          color: Color.foreground
          placeholderTextColor: Color.muted
          font.pixelSize: 16
          selectByMouse: true
          background: Rectangle { color: Color.background; border.color: search.activeFocus ? Color.accent : Color.muted; radius: 6 }
          onTextChanged: list.positionViewAtBeginning()
          onPreeditTextChanged: list.positionViewAtBeginning()
          onAccepted: { Qt.inputMethod.hide(); content.forceActiveFocus() }
        }
        RowLayout {
          Layout.fillWidth: true
          Text { text: root.marketplace ? "Official community catalog" : "Default + downloaded themes"; color: Color.foreground; opacity: .7; font.pixelSize: 12; Layout.fillWidth: true; wrapMode: Text.WordWrap }
          Action { visible: root.marketplace; text: "Refresh"; enabled: !root.state.busy; Layout.preferredWidth: 80; onClicked: AndroidBridge.refreshThemeMarketplace() }
        }
        Text {
          visible: text.length > 0
          text: root.state.error || root.state.message || ""
          Layout.fillWidth: true
          color: root.state.error ? Color.urgent : Color.accent
          font.pixelSize: 13
          wrapMode: Text.WordWrap
        }
        ProgressBar { visible: root.state.busy || false; indeterminate: true; Layout.fillWidth: true; Layout.preferredHeight: 4 }
        ListView {
          id: list
          Layout.fillWidth: true
          Layout.fillHeight: true
          clip: true
          spacing: 10
          model: root.rows
          reuseItems: true
          ScrollBar.vertical: ScrollBar {}
          delegate: Rectangle {
            id: row
            required property var modelData
            readonly property string themeId: modelData.id || modelData.slug
            readonly property bool present: root.installed(themeId)
            width: list.width
            height: details.implicitHeight + 24
            color: modelData.background || Color.background
            border.color: root.state.active === themeId ? Color.accent : Color.muted
            border.width: root.state.active === themeId ? 2 : 1
            radius: 8
            RowLayout {
              id: details
              anchors { left: parent.left; right: parent.right; top: parent.top; margins: 12 }
              spacing: 12
              ColumnLayout {
                Layout.fillWidth: true
                spacing: 6
                Text { text: row.modelData.name; color: row.modelData.foreground || Color.foreground; font.pixelSize: 16; font.bold: true; Layout.fillWidth: true; wrapMode: Text.WordWrap }
                Text { text: root.state.active === row.themeId ? "Active" : row.modelData.builtIn ? "Omarchy default · " + row.modelData.mode : row.present ? "Downloaded" : "Community theme"; color: row.modelData.foreground || Color.foreground; opacity: .7; font.pixelSize: 12; Layout.fillWidth: true; wrapMode: Text.WordWrap }
                Row {
                  visible: !root.marketplace
                  spacing: 5
                  Repeater { model: [row.modelData.accent || Color.accent, row.modelData.foreground || Color.foreground, row.modelData.background || Color.background]; Rectangle { required property var modelData; width: 18; height: 8; radius: 3; color: modelData; border.color: Color.muted } }
                }
              }
              Action {
                text: root.state.active === row.themeId ? "Active" : row.present ? "Apply" : "Install"
                Accessible.name: text + " " + row.modelData.name
                enabled: !root.state.busy && root.state.active !== row.themeId
                Layout.preferredWidth: 76
                onClicked: { Qt.inputMethod.hide(); content.forceActiveFocus(); if (row.present) AndroidBridge.applyTheme(row.themeId); else AndroidBridge.installTheme(row.themeId) }
              }
            }
          }
          Text { anchors.centerIn: parent; width: parent.width; horizontalAlignment: Text.AlignHCenter; wrapMode: Text.WordWrap; visible: list.count === 0; text: "No themes found."; color: Color.foreground }
        }
      }
    }
  }
}
