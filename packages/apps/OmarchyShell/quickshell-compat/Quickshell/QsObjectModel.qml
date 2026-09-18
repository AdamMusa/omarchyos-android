import QtQuick

// Upstream hands out live object models whose `values` list changes as things
// come and go, and whose consumers connect to valuesChanged. This is that
// contract, filled by whatever the Android side supplies.
QtObject {
  property var values: []
}
