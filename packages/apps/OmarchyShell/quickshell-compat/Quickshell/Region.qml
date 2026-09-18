import QtQuick

// Input region for a shell surface. Android takes the union of these as the
// window's touchable region, so the bar stays click-through everywhere it
// draws nothing.
QtObject {
  id: root
  property var item: null
  property rect rect: Qt.rect(0, 0, 0, 0)
  property int x: 0
  property int y: 0
  property int width: 0
  property int height: 0
  property int intersection: 0   // Combine
  property list<QtObject> regions
  default property alias children: root.regions
}
