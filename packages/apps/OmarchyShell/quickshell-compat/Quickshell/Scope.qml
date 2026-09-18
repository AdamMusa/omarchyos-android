import QtQuick

// A non-visual grouping element, same role as upstream's Scope.
QtObject {
  id: root
  default property list<QtObject> children
}
