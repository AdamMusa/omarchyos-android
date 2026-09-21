#!/usr/bin/env python3
"""Check the built-in Home; optionally verify its native App info protection."""
import argparse
import os
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', default=os.environ.get('ANDROID_SERIAL'))
parser.add_argument('--check-app-info', action='store_true',
                    help='Check English App info controls, then return Home')
args = parser.parse_args()
adb = ['adb'] + (['-s', args.serial] if args.serial else [])


def shell(*command):
    return subprocess.check_output(adb + ['shell', *command], text=True, timeout=30)


package = shell('dumpsys', 'package', 'os.omarchy.shell')
flags = re.search(r'^\s+flags=\[([^\]]+)\]', package, re.M)
assert flags and 'SYSTEM' in flags[1].split(), 'Omarchy is not a system package'
assert 'UPDATED_SYSTEM_APP' not in flags[1].split(), 'Validate the base image without a data APK update masking it'
private = re.search(r'^\s+privateFlags=\[([^\]]+)\]', package, re.M)
assert private and 'PRIVILEGED' in private[1], 'Omarchy is not privileged'
path = '/system_ext/priv-app/OmarchyShell/OmarchyShell.apk'
assert shell('test', '-f', path) == '', 'Base system APK missing'
assert shell('pm', 'path', 'os.omarchy.shell').strip() == 'package:' + path, 'Home is not running from the base system APK'
assert '/system_ext/priv-app/OmarchyShell' in package, 'PackageManager did not scan the system APK'
home = shell('cmd', 'package', 'resolve-activity', '--brief',
             '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
assert 'os.omarchy.shell/' in home, home
launchers = shell('cmd', 'package', 'query-activities', '--brief',
                  '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.LAUNCHER',
                  '-p', 'os.omarchy.shell')
assert 'OmarchyActivity' not in launchers, 'Home must not appear as an ordinary app launcher'
assert shell('pm', 'path', 'com.android.launcher3').startswith('package:'), 'Overview provider missing'
assert shell('/system_ext/bin/bash', '-c', "'[[ ${BASH_VERSINFO[0]} -ge 5 ]] && printf bash-ready'").strip() == 'bash-ready', 'System Bash missing or not executable'
print('PASS: privileged system Home, base APK present, default Home resolves, no app-drawer entry, overview retained')

if args.check_app_info:
    try:
        shell('am', 'start', '-W', '-a', 'android.settings.APPLICATION_DETAILS_SETTINGS',
              '-d', 'package:os.omarchy.shell')
        for attempt in range(8):
            dumped = shell('uiautomator', 'dump', '/data/local/tmp/omarchy-home-protection.xml')
            if 'dumped to:' in dumped:
                page = ET.fromstring(shell('cat', '/data/local/tmp/omarchy-home-protection.xml'))
                disable = [n for n in page.iter('node') if n.get('text') == 'Disable']
                if disable:
                    break
            time.sleep(.5)
        else:
            raise AssertionError('App info did not expose Disable; use an English test device')
        parents = {child: parent for parent in page.iter() for child in parent}
        for label in disable:
            action = label
            # Compose exposes the label separately from its disabled action.
            while action.get('clickable') != 'true' and action in parents:
                action = parents[action]
            assert action.get('clickable') == 'true', 'Disable action not found'
            assert action.get('enabled') == 'false', 'System Home can be disabled in App info'
        assert not any(n.get('text') == 'Uninstall' for n in page.iter('node')), \
            'System Home exposes ordinary Uninstall'
        print('PASS: native App info disables Disable and omits ordinary Uninstall')
    finally:
        shell('input', 'keyevent', 'HOME')
