import QtQuick

// Root of the shell process. Upstream's ShellRoot is a non-visual scope that
// owns every surface and service instance, so it holds plain objects rather
// than visual children.
QtObject {
  id: root
  default property list<QtObject> data
}
