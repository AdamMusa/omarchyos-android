#!/usr/bin/env bash
# Applies the OmarchyOS patches to a staged copy of the vendored Omarchy tree.
#
# third_party/omarchy stays byte-identical to upstream so it can be re-synced
# with one rsync; anything OmarchyOS has to change to build against this Qt
# lives here and is applied to the staged copy instead. Each patch says which
# Qt behaviour forces it, so it can be dropped when the toolchain moves on.
set -Eeuo pipefail
tree=${1:?usage: apply.sh <staged-omarchy-tree>}
python3 - "$tree" <<'PY'
import pathlib, re, sys
tree = pathlib.Path(sys.argv[1])

# 0002 — Every Omarchy panel is a KeyboardPanel, and KeyboardPanel sizes its
# card from a contentWidth each panel picks for a desktop. A phone is narrower
# than any of those, so the card ran off the right edge and covered the bar.
# The card is capped to what the display can actually show; the origin
# calculation reads the same capped values so it stays centred under its icon.
panel = tree / "shell/Ui/KeyboardPanel.qml"
if panel.is_file():
    text = panel.read_text()
    anchor = "  readonly property point cardOrigin: {"
    if "fittedCardWidth" not in text and anchor in text:
        text = text.replace(anchor, """  // OmarchyOS: the card can never be larger than the screen it is drawn on.
  readonly property real fittedCardWidth: Math.min(contentWidth, screenW - 2 * margin)
  readonly property real fittedCardHeight: Math.min(contentHeight, screenH - 2 * margin)

""" + anchor, 1)
        # Inside the origin block only, the capped values decide placement.
        start = text.index(anchor)
        end = text.index("return Qt.point(Math.round(x), Math.round(y))", start)
        block = text[start:end]
        block = block.replace("contentWidth", "fittedCardWidth").replace("contentHeight", "fittedCardHeight")
        # The property's own declaration must keep its name.
        block = block.replace("readonly property real fittedCardWidth", "readonly property real fittedCardWidth")
        text = text[:start] + block + text[end:]
        text = text.replace("""    width: root.contentWidth
    height: root.contentHeight""", """    width: root.fittedCardWidth
    height: root.fittedCardHeight""", 1)
        panel.write_text(text)
        print("patched shell/Ui/KeyboardPanel.qml: card capped to the screen")

# 0001 — Qt 6.8's QML parser still treats `transient` as a future reserved
# word, so `var transient = false` fails to parse and takes the whole
# notifications service down. The variable is local to isEphemeral(); the
# string key "transient" in the freedesktop hints must not be touched.
for rel in ("shell/plugins/notifications/Service.qml",):
    path = tree / rel
    if not path.is_file():
        continue
    text = path.read_text()
    lines = []
    for line in text.splitlines(keepends=True):
        # Comments keep upstream's wording: the hint really is called
        # "transient" and the note explains the protocol, not the variable.
        if not line.lstrip().startswith("//"):
            line = re.sub(r'(?<!["\w])transient(?!["\w])', "isTransient", line)
        lines.append(line)
    patched = "".join(lines)
    if patched != text:
        path.write_text(patched)
        print(f"patched {rel}: transient -> isTransient")
PY
