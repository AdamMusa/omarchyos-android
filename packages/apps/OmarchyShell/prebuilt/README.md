# OmarchyShell prebuilt

`OmarchyShell.apk` here is the artifact of `tools/build-apk.sh`, not a
checked-in binary. Copy the APK from the build output into this directory
before running a platform build; `.gitignore` keeps it out of the repository.

It is imported rather than built by Soong because the shell is a Qt
application — it needs `qt-cmake`, `androiddeployqt` and gradle, none of which
exist inside the platform build. The import still signs it with the platform
certificate and installs it as a privileged `system_ext` app, so from the
device's point of view there is no difference from a Soong-built shell.

The product also installs `omarchy_bash` in `/system_ext/bin/bash`, extracted
from that same APK. It must be a system executable: native libraries in a system
app directory are not executable files. Home uses Bash to discover its plugins;
omitting this module leaves the startup curtain waiting for a wallpaper.
