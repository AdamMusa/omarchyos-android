# Native Omarchy power menu

Holding the power button opens Android's native power menu by default. The
framework overlay sets `config_longPressOnPowerBehavior` to Global Actions;
Android's `power_button_long_press` user preference still takes precedence.
Previously the unconfigured product inherited the assistant action, even when
no assistant was available.

The menu retains Android's Emergency, Power off and Restart actions. Its buttons
use the selected Omarchy semantic colors, outlined rectangular shapes and compact
corners. Keyboard focus gets a thicker accent outline. Android continues to own
input handling, accessibility, emergency behavior and shutdown/restart policy.
Labels use a known wrapping width when Android measures the grid, so large text
can take multiple lines without being clipped or forced into a scrolling marquee.

Build the framework and SystemUI overlays into the product image. These are
static overlays; sideloading an update is rejected and does not validate this
change. The incremental image builder accepts their compiled directory as its
sixth argument.

On an English development user with no existing power-button preference, run:

```sh
python3 packages/apps/OmarchyShell/tools/test-system-power.py \
  --serial emulator-5556 --screenshots out/power-menu-check
```

The check verifies the installed default, opens the real SystemUI menu, confirms
its three actions, dismisses it with Back, and verifies that an explicit assistant
preference overrides the default. It restores the original unset preference and
checks that SystemUI did not restart. It does not select Emergency, shut down, or
restart the phone; those actions are left in Android's existing implementation.

The v14 development image passes this check with the overlays built into
`system_ext`, including native user-preference precedence and dismissal. Visual
inspection confirms rectangular outlined actions, retained emergency tint and
complete labels at the normal text size and at 200% text size on a 320dp-wide
portrait display. The same enlarged-text check passes in landscape, where the
native grid uses a single row. Font scale, density and rotation were restored
after testing; the temporary layout-preview overlay was uninstalled before the
installed-image checks.
