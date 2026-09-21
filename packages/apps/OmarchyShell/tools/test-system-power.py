#!/usr/bin/env python3
"""Check the native power menu on an English test user without invoking its actions."""
import argparse
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True)
parser.add_argument('--screenshots', type=Path)
args = parser.parse_args()
adb = ['adb', '-s', args.serial]

def shell(*command):
    return subprocess.check_output(adb + ['shell', *command], text=True, timeout=30)

original = shell('settings', 'get', 'global', 'power_button_long_press').strip()
assert original == 'null', 'Use a test user with no power-button override; keep existing preferences intact'
assert shell('cmd', 'overlay', 'lookup', 'android',
             'android:integer/config_longPressOnPowerBehavior').strip() == '1', 'Missing power-menu product default'
assert 'mLongPressOnPowerBehavior=LONG_PRESS_POWER_GLOBAL_ACTIONS' in shell('dumpsys', 'window', 'policy')
ui_pid = shell('pidof', 'com.android.systemui').strip()
try:
    shell('input', 'keyevent', 'WAKEUP')
    shell('input', 'keyevent', '--longpress', 'POWER')
    time.sleep(.7)
    path = '/data/local/tmp/omarchy-power-check.xml'
    output = shell('uiautomator', 'dump', path)
    assert 'dumped to' in output, output
    root = ET.fromstring(shell('cat', path))
    nodes = list(root.iter('node'))
    labels = {n.get('text') for n in nodes if n.get('package') == 'com.android.systemui'}
    assert {'Emergency', 'Power off', 'Restart'} <= labels, labels
    assert 'os.omarchy.shell' not in {n.get('package') for n in nodes}, 'Power menu must be owned by SystemUI'
    if args.screenshots:
        args.screenshots.mkdir(parents=True, exist_ok=True)
        with (args.screenshots / 'power-menu.png').open('wb') as image:
            subprocess.run(adb + ['exec-out', 'screencap', '-p'], stdout=image, check=True, timeout=30)
    shell('input', 'keyevent', 'BACK')
    time.sleep(.3)
    output = shell('uiautomator', 'dump', path)
    assert 'dumped to' in output, output
    dismissed = ET.fromstring(shell('cat', path))
    assert not {'Emergency', 'Power off', 'Restart'} <= {
        n.get('text') for n in dismissed.iter('node')
        if n.get('package') == 'com.android.systemui'
    }, 'Back did not dismiss the power menu'
    # A user-selected assistant remains authoritative over the product default.
    shell('settings', 'put', 'global', 'power_button_long_press', '5')
    for _ in range(20):
        if 'mLongPressOnPowerBehavior=LONG_PRESS_POWER_ASSISTANT' in shell('dumpsys', 'window', 'policy'):
            break
        time.sleep(.1)
    else:
        raise AssertionError('Native user preference did not override the product default')
finally:
    shell('settings', 'delete', 'global', 'power_button_long_press')
    shell('input', 'keyevent', 'BACK')
for _ in range(20):
    if 'mLongPressOnPowerBehavior=LONG_PRESS_POWER_GLOBAL_ACTIONS' in shell('dumpsys', 'window', 'policy'):
        break
    time.sleep(.1)
else:
    raise AssertionError('Product default was not restored')
assert shell('pidof', 'com.android.systemui').strip() == ui_pid, 'SystemUI restarted during the check'
print('PASS: native power-menu default, Emergency/Power off/Restart actions present, Back dismisses, user override respected, SystemUI stable')
