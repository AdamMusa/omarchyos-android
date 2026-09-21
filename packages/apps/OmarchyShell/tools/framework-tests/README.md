# Task snapshot regression probe

`SnapshotConversionProbe.java` runs in an isolated shell process on a development
emulator. It creates a synthetic GPU-only buffer, checks whether direct CPU
mapping fails on that driver, then invokes the installed framework's guarded
conversion. It never reads user task images and is not packaged in the OS.

Compile with the Android SDK, convert the class to DEX with `d8`, and push
`classes.dex` to `/data/local/tmp/snapshot-probe.dex`. Run against the framework
in the booted image:

```sh
adb shell 'CLASSPATH=/system/framework/services.jar:/data/local/tmp/snapshot-probe.dex app_process /system/bin SnapshotConversionProbe'
```

A successful recovery prints `PASS guarded snapshot conversion`. On the ARM64
host-GPU emulator used for this regression it also reports
`direct failure exercised=true`, confirming that the failing path ran. A driver
that permits CPU mapping can pass without exercising that branch; this alone
does not establish regression coverage. Verify Overview and theme changes in
the OS separately and check that `system_server` remains alive.
