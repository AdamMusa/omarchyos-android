pragma Singleton
import QtQuick

// Upstream reads PipeWire nodes for the audio widget and volume OSD.
// AudioManager provides the same three things the shell actually uses:
// default sink volume, mute state, and the output device name.
QtObject {
  id: root
  readonly property QtObject defaultAudioSink: QtObject {
    readonly property QtObject audio: QtObject {
      property real volume: AndroidBridge.volume
      property bool muted: AndroidBridge.muted
      onVolumeChanged: AndroidBridge.volume = volume
      onMutedChanged: AndroidBridge.muted = muted
    }
    readonly property string description: AndroidBridge.audioOutputName
    readonly property bool isSink: true
  }
  readonly property QtObject defaultAudioSource: QtObject {
    readonly property QtObject audio: QtObject {
      property real volume: AndroidBridge.micVolume
      property bool muted: AndroidBridge.micMuted
    }
  }
  readonly property var nodes: [defaultAudioSink, defaultAudioSource]
}
