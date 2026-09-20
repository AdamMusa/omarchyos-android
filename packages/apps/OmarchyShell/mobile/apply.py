"""Stage phone adaptations without modifying the vendored desktop tree."""
import json
from pathlib import Path
import shutil
import sys

tree = Path(sys.argv[1])
source = Path(__file__).resolve().parent


def replace(path, before, after):
    file = tree / path
    text = file.read_text()
    if after in text and before not in text.replace(after, ""):
        return
    if before not in text:
        raise RuntimeError(f"Mobile adaptation no longer matches {path}")
    file.write_text(text.replace(before, after, 1))


shutil.copyfile(source / "AppLibrary.qml", tree / "shell/services/AppLibrary.qml")
shutil.copyfile(source / "Background.qml", tree / "shell/plugins/background/Background.qml")
status = tree / "shell/plugins/mobile-status"
status.mkdir(exist_ok=True)
shutil.copyfile(source / "MobileStatus.qml", status / "MobileStatus.qml")
(status / "manifest.json").write_text(json.dumps({
    "schemaVersion": 1, "id": "omarchy.mobile-status", "name": "Phone status",
    "version": "1.0.0", "author": "OmarchyOS", "kinds": ["bar-widget"],
    "entryPoints": {"barWidget": "MobileStatus.qml"},
    "barWidget": {"displayName": "Phone status", "allowMultiple": False}
}, indent=2))

themes = tree / "shell/plugins/mobile-themes"
themes.mkdir(exist_ok=True)
shutil.copyfile(source / "ThemeBrowser.qml", themes / "ThemeBrowser.qml")
(themes / "manifest.json").write_text(json.dumps({
    "schemaVersion": 1, "id": "omarchy.mobile-themes", "name": "Phone themes",
    "version": "1.0.0", "author": "OmarchyOS", "kinds": ["service"],
    "entryPoints": {"service": "ThemeBrowser.qml"}
}, indent=2))
assets = tree / "mobile-themes"
assets.mkdir(exist_ok=True)
shutil.copytree(source / "wallpapers", assets / "wallpapers", dirs_exist_ok=True)
shutil.copyfile(source / "marketplace.json", assets / "marketplace.json")

enabled = {"omarchy.bar", "omarchy.menu", "omarchy.clock", "omarchy.mobile-status", "omarchy.background", "omarchy.mobile-themes"}
disabled = sorted({json.loads(path.read_text())["id"]
                   for path in (tree / "shell/plugins").rglob("*manifest.json")} - enabled)
config = {
    "version": 1, "mobileProfile": 3,
    "bar": {"position": "top", "transparent": False, "centerAnchor": "omarchy.clock",
            "layout": {"left": [{"id": "omarchy.menu"}],
                       "center": [{"id": "omarchy.clock", "format": "dddd HH:mm", "formatAlt": "d MMMM 'W'ww yyyy"}],
                       "right": [{"id": "omarchy.mobile-status"}]}},
    "plugins": [], "disabledPlugins": disabled
}
(tree / "config/omarchy/shell.json").write_text(json.dumps(config, indent=2) + "\n")

menu = {
    "root": {"label": "Omarchy", "title": "Omarchy"},
    "apps": {"icon": "󰀻", "label": "Apps", "provider": "apps"},
    "settings": {"icon": "", "label": "Phone settings"},
    "themes": {"icon": "󰏘", "label": "Themes", "action": "mobile:themes"},
    "manage": {"icon": "󰭌", "label": "Manage apps", "action": "mobile:apps"}
}
for key, label in [("wifi", "Wi-Fi"), ("bluetooth", "Bluetooth"), ("sound", "Sound"),
                   ("display", "Display"), ("screensaver", "Screensaver"), ("battery", "Battery"), ("storage", "Storage"),
                   ("accessibility", "Accessibility"), ("security", "Security"), ("all", "All settings")]:
    menu["settings." + key] = {"label": label, "action": "mobile:" + key}
(tree / "default/omarchy/omarchy-menu.jsonc").write_text(json.dumps(menu, ensure_ascii=False, indent=2) + "\n")

replace("shell/plugins/menu/Menu.qml", "    Util.execDetached(command)", """    if (command.indexOf("mobile:") === 0) {
      if (command === "mobile:themes") AndroidBridge.openThemes()
      else AndroidBridge.openSettings(command.slice(7))
      return
    }
    Util.execDetached(command)""")
replace("shell/plugins/menu/Menu.qml",
        "Math.max(Style.gapsOut, Math.round((height - root.cardHeight) / 2))",
        "Math.max(64, Style.bar.sizeHorizontal + 16)")
replace("shell/plugins/menu/Menu.qml", "maxRowsHeight = root.visibleRowsHeight",
        "maxRowsHeight = root.availableRowsHeight()")
replace("shell/plugins/menu/Menu.qml", """        Keys.onPressed: function(event) {
          if (root.deleteConfirmOpen)""", """        Keys.onPressed: function(event) {
          if (event.key === Qt.Key_Back && root.navStack.length > 0 && !root.deleteConfirmOpen) {
            root.goBack()
            event.accepted = true
            return
          }
          if (root.deleteConfirmOpen)""")
replace("shell/plugins/bar/Bar.qml",
        "readonly property int barSize: vertical ? Style.bar.sizeVertical : Style.bar.sizeHorizontal",
        "readonly property int barSize: Math.max(48, vertical ? Style.bar.sizeVertical : Style.bar.sizeHorizontal)")
replace("shell/Ui/WidgetButton.qml", "Math.max(12, label.implicitWidth + scaledHorizontalMargin * 2)",
        "Math.max(48, label.implicitWidth + scaledHorizontalMargin * 2)")
# Keep the center clock inside the measured free space between the edge groups.
replace("shell/plugins/bar/Bar.qml", """        CenterModules { anchors.fill: parent }

        LeftModules {
          anchors.left: parent.left""", """        CenterModules {
          anchors.left: phoneLeft.right
          anchors.right: phoneRight.left
          height: parent.height
          clip: true
        }

        LeftModules {
          id: phoneLeft
          anchors.left: parent.left""")
replace("shell/plugins/bar/Bar.qml", """        RightModules {
          anchors.right: parent.right""", """        RightModules {
          id: phoneRight
          anchors.right: parent.right""")
# The Android app window excludes system navigation/cutout insets. Popups use
# its actual content bounds rather than the full physical screen dimensions.
replace("shell/Ui/KeyboardPanel.qml", "screen ? screen.width : 0", "ShellSurfaceRoot.screenWidth")
replace("shell/Ui/KeyboardPanel.qml", "screen ? screen.height : 0", "ShellSurfaceRoot.screenHeight")
# Android has one window, so QsWindow cannot supply a desktop bar window.
replace("shell/Ui/KeyboardPanel.qml", "anchorWindow ? anchorWindow.height : 0",
        'bar ? ((barPos === "top" || barPos === "bottom") ? bar.barSize : screenH) : 0')
replace("shell/Ui/KeyboardPanel.qml", "anchorWindow ? anchorWindow.width : screenW",
        'bar && (barPos === "left" || barPos === "right") ? bar.barSize : screenW')
replace("shell/Ui/KeyboardPanel.qml",
        "if (!root.anchorWindow || !root.anchorWindow.contentItem || !root.bar || !root.bar.clickTargets) return null",
        "if (!root.bar || !root.bar.clickTargets) return null")
replace("shell/Ui/KeyboardPanel.qml",
        "var pos = root.anchorWindow.itemPosition(target)",
        "var pos = target.mapToItem(null, 0, 0)")
replace("shell/Ui/KeyboardPanel.qml",
        "if (root.bar.targetBelongsToWindow && !root.bar.targetBelongsToWindow(target, root.anchorWindow)) continue",
        "// All phone bar targets belong to the same Android window.")
# Preserve Omarchy's calendar, fitting all seven days instead of cropping it.
clock_panel = "shell/plugins/panels/clock/Panel.qml"
replace(clock_panel, "readonly property int cellWidth: Style.space(52)",
        "readonly property int cellWidth: Math.max(22, Math.floor((calendarScroll.width - weekColumnWidth - gutterWidth - cellSpacing * 8) / 7))")
replace(clock_panel, "readonly property int weekColumnWidth: Style.space(32)",
        "readonly property int weekColumnWidth: Style.space(22)")
replace(clock_panel, "readonly property int gutterWidth: Style.space(14)",
        "readonly property int gutterWidth: Style.space(6)")
replace(clock_panel, "width: Math.max(calendarScroll.width, gridColumn.width)",
        "width: calendarScroll.width")
replace(clock_panel, "spacing: Style.space(22)", "spacing: Style.space(10)")
replace(clock_panel, "font.pixelSize: 48", "font.pixelSize: Math.min(32, calendarScroll.width / 12)")
replace(clock_panel, "font.pixelSize: 52", "font.pixelSize: Math.min(32, calendarScroll.width / 12)")
replace(clock_panel, "text: root.weekdayLabel(modelData)", "text: root.weekdayLabel(modelData).slice(0, 2)")
replace(clock_panel, "height: monthLabel.implicitHeight + Style.space(10)", "height: Math.max(48, monthLabel.implicitHeight + Style.space(10))")
replace("shell/Ui/PanelActionButton.qml", "Math.max(Style.space(22), fontSize + Style.spacing.sm * 2)",
        "Math.max(48, fontSize + Style.spacing.sm * 2)")
for path in ("shell/plugins/menu/Menu.qml", "shell/Ui/PanelKeyCatcher.qml"):
    replace(path, "event.key === Qt.Key_Escape", "(event.key === Qt.Key_Escape || event.key === Qt.Key_Back)")
# Desktop extension icons can point outside the subset we vendor. They are
# unused on Android, and dangling asset links break Qt's next CMake configure.
for path in tree.rglob("*"):
    if path.is_symlink() and not path.exists():
        path.unlink()
print("Staged Android app library, phone menu, compact bar and native settings routes")

# Android supplies fonts and layout metrics directly; do not launch desktop
# fontconfig/Hyprland probes at startup or after every palette update.
replace("shell/Commons/Style.qml", "    hyprctlProc.running = true\n    gapsOutProc.running = true",
        "    // Android window metrics are supplied by the mobile profile.")
replace("shell/Commons/Style.qml", "    fcMatchProc.running = true",
        '    root.resolvedFontFamily = "JetBrainsMono NF"')
replace("shell/services/PluginRegistry.qml", "      localPluginWatcher.running = true",
        "      // Android package changes are handled by the platform bridge.")

# Desktop screenshot previews are unused: mobile rows show actual wallpapers.
for preview in (tree / "themes").glob("*/preview*.png"):
    preview.unlink()
