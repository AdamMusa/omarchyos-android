import QtQuick
// Upstream resolves freedesktop icon names. On Android icons come from the
// package manager, so the host hands back a data URL for a package or a
// bundled Omarchy glyph for shell-internal names.
Image {
  id: root
  property string source_: ""
  property string implicitSource: ""
  property bool backer: true
  source: implicitSource !== "" ? implicitSource : AndroidBridge.iconUrl(source_)
  fillMode: Image.PreserveAspectFit
  smooth: true
  mipmap: true
}
