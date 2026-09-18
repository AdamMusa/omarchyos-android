import QtQuick

// Same contract as upstream: build the component only once `active` (or
// `loading`) turns true, then keep the instance alive.
Item {
  id: root
  visible: false
  property bool active: false
  property bool loading: false
  property Component component: null
  property QtObject item: loader.item
  Loader {
    id: loader
    active: root.active || root.loading
    sourceComponent: root.component
    asynchronous: root.loading && !root.active
  }
}
