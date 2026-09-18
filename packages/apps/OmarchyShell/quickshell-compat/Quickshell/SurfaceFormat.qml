import QtQuick
// GL surface format request. Omarchy asks for a non-opaque surface so the bar
// can be translucent; Android composites shell windows with alpha already, so
// the flag is recorded and otherwise has no work to do.
QtObject {
  property bool opaque: false
}
