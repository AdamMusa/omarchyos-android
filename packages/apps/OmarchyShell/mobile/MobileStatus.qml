import QtQuick
import qs.Commons
import qs.Ui

BarWidget {
  id: root
  moduleName: "omarchy.mobile-status"
  implicitWidth: controls.implicitWidth
  implicitHeight: barSize
  Row {
    id: controls
    WidgetButton {
      bar: root.bar
      text: "\uf013"
      fixedWidth: 48
      fontSize: Math.max(18, Style.font.body)
      tooltipText: "Phone settings"
      onPressed: {
        if (root.bar) root.bar.run("omarchy-shell shell toggle omarchy.menu '{\"menu\":\"settings\"}'")
      }
    }
    WidgetButton {
      bar: root.bar
      text: (AndroidBridge.charging ? "\uf0e7 " : "") + AndroidBridge.batteryPercent + "%"
      fixedWidth: Math.max(48, labelWidth + 12)
      tooltipText: "Battery"
      onPressed: AndroidBridge.openSettings("battery")
    }
  }
}
