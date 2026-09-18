import QtQuick

// Upstream instantiates one copy of `delegate` per entry in `model` (usually
// one per screen). Phones have a single display, but keeping the type means
// bar/panel code that iterates screens works untouched, and external displays
// work the day the device supports them.
Item {
  id: root
  visible: false
  property var model: []
  property Component delegate: null
  property list<QtObject> instances

  onModelChanged: rebuild()
  onDelegateChanged: rebuild()
  Component.onCompleted: rebuild()

  function rebuild() {
    for (var i = 0; i < instances.length; i++)
      if (instances[i]) instances[i].destroy()
    instances = []
    if (!delegate || !model) return
    var list = Array.isArray(model) ? model : [model]
    for (var j = 0; j < list.length; j++) {
      var obj = delegate.createObject(root, { modelData: list[j] })
      if (obj) instances.push(obj)
    }
  }
}
