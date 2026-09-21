#!/usr/bin/env python3
"""Read-only checks that Home is part of the booted image, not a sideloaded launcher."""
import argparse
import os
import re
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', default=os.environ.get('ANDROID_SERIAL'))
args = parser.parse_args()
adb = ['adb'] + (['-s', args.serial] if args.serial else [])


def shell(*command):
    return subprocess.check_output(adb + ['shell', *command], text=True, timeout=30)


package = shell('dumpsys', 'package', 'os.omarchy.shell')
flags = re.search(r'^\s+flags=\[([^\]]+)\]', package, re.M)
assert flags and 'SYSTEM' in flags[1].split(), 'Omarchy is not a system package'
private = re.search(r'^\s+privateFlags=\[([^\]]+)\]', package, re.M)
assert private and 'PRIVILEGED' in private[1], 'Omarchy is not privileged'
path = '/system_ext/priv-app/OmarchyShell/OmarchyShell.apk'
assert shell('test', '-f', path) == '', 'Base system APK missing'
assert '/system_ext/priv-app/OmarchyShell' in package, 'PackageManager did not scan the system APK'
home = shell('cmd', 'package', 'resolve-activity', '--brief',
             '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
assert 'os.omarchy.shell/' in home, home
launchers = shell('cmd', 'package', 'query-activities', '--brief',
                  '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.LAUNCHER',
                  '-p', 'os.omarchy.shell')
assert 'OmarchyActivity' not in launchers, 'Home must not appear as an ordinary app launcher'
assert shell('pm', 'path', 'com.android.launcher3').startswith('package:'), 'Overview provider missing'
print('PASS: privileged system Home, base APK present, default Home resolves, no app-drawer entry, overview retained')
