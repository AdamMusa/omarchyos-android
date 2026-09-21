#!/usr/bin/env python3
"""Exercise native volume controls on an English development emulator."""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True)
parser.add_argument('--probe-dex', type=Path, required=True,
                    help='dex/classes.dex from system-ui-tests/build-probe.sh')
parser.add_argument('--screenshots', type=Path, required=True)
args = parser.parse_args()
adb = ['adb', '-s', args.serial]
args.screenshots.mkdir(parents=True, exist_ok=True)

def shell(*command):
    return subprocess.check_output(adb + ['shell', *command], text=True, timeout=30).strip()

def volume():
    output = shell('cmd', 'media_session', 'volume', '--stream', '3', '--get')
    match = re.search(r'volume is (\d+) in range \[(\d+)\.\.(\d+)\]', output)
    assert match, output
    return tuple(map(int, match.groups()))

def nodes():
    for _ in range(3):
        result = json.loads(shell(
            'CLASSPATH=/data/local/tmp/system-ui-window-probe.dex '
            'app_process /system/bin SystemUiWindowProbe'))
        if result:
            return result
        time.sleep(.3)
    raise AssertionError('No SystemUI windows available')

def tap(node):
    x, y, right, bottom = map(int, re.findall(r'\d+', node['bounds']))
    assert right > x and bottom > y, node
    shell('input', 'tap', str((x + right) // 2), str((y + bottom) // 2))

def capture(name, current):
    (args.screenshots / (name + '.json')).write_text(json.dumps(current, indent=2))
    with (args.screenshots / (name + '.png')).open('wb') as image:
        subprocess.run(adb + ['exec-out', 'screencap', '-p'], stdout=image,
                       check=True, timeout=30)

def launch_probe():
    shell('am', 'force-stop', 'os.omarchy.uicheck')
    shell('am', 'start', '-W', '-n', 'os.omarchy.uicheck/.ProbeActivity',
          '--es', 'mode', 'volume')

def open_panel():
    path = '/data/local/tmp/omarchy-volume-check.xml'
    output = shell('uiautomator', 'dump', path)
    assert 'dumped to' in output, output
    page = ET.fromstring(shell('cat', path))
    button = next(n for n in page.iter('node')
                  if n.get('package') == 'os.omarchy.uicheck'
                  and n.get('class') == 'android.widget.Button')
    tap(button.attrib)
    time.sleep(1)
    current = nodes()
    assert any(n['id'] == 'com.android.systemui:id/volume_dialog' for n in current), current
    media = next(n for n in current
                 if n['class'] == 'android.widget.SeekBar' and n['description'] == 'Media')
    assert media['range']['current'] == volume()[0], media
    for description in ['Omarchy menu', 'Omarchy settings']:
        assert sum(n['description'] == description for n in current) == 1, current
    return current

def set_with_keys(target):
    # Android can use the first press solely to reveal a hidden panel.
    for _ in range(maximum - minimum + 2):
        current = volume()[0]
        if current == target:
            return
        shell('input', 'keyevent', '24' if current < target else '25')
        time.sleep(.4)
    assert volume()[0] == target, ('Volume key did not reach requested level', target)

assert shell('pm', 'path', 'os.omarchy.uicheck'), 'Install the disposable UI probe first'
subprocess.run(adb + ['push', str(args.probe_dex),
                     '/data/local/tmp/system-ui-window-probe.dex'], check=True)
original, minimum, maximum = volume()
timeout = shell('settings', 'get', 'secure', 'volume_dialog_dismiss_timeout')
ui_pid = shell('pidof', 'com.android.systemui')
try:
    shell('settings', 'put', 'secure', 'volume_dialog_dismiss_timeout', '60000')
    launch_probe()
    compact = open_panel()
    assert volume()[0] == original, 'Opening the panel changed media volume'
    capture('compact', compact)
    target = original + 1 if original < maximum else original - 1
    assert minimum <= target <= maximum
    set_with_keys(target)
    assert volume()[0] != original, 'Volume keys did not change the native stream'
    set_with_keys(original)
    tap(next(n for n in nodes() if n['id'] == 'com.android.systemui:id/volume_dialog_settings'))
    time.sleep(1)
    expanded = nodes()
    sliders = {n['description']: n for n in expanded if n['class'] == 'android.widget.SeekBar'}
    assert {'Media', 'Call', 'Ring', 'Notification', 'Alarm'} <= sliders.keys(), sliders
    assert sliders['Media']['range']['current'] == original, sliders['Media']
    capture('expanded', expanded)
    # Exercise the actual track as well as hardware keys: styling must preserve
    # touch input and the native stream's accessible value.
    left, top, right, bottom = map(int, sliders['Media']['bounds'].split())
    fraction = .25 if original >= (minimum + maximum) / 2 else .75
    shell('input', 'tap', str(round(left + (right - left) * fraction)),
          str((top + bottom) // 2))
    time.sleep(.5)
    changed = volume()[0]
    assert changed != original, 'Touching the media slider did not change the native stream'
    touched = next(n for n in nodes()
                   if n['class'] == 'android.widget.SeekBar' and n['description'] == 'Media')
    assert touched['range']['current'] == changed, touched
    set_with_keys(original)
    tap(next(n for n in expanded if n['text'] == 'Done'))
    time.sleep(.5)
    assert not any(n['class'] == 'android.widget.SeekBar' for n in nodes()), 'Done did not close the panel'
    focus = next(line for line in shell('dumpsys', 'window').splitlines() if 'mCurrentFocus=' in line)
    assert 'os.omarchy.uicheck/' in focus, ('Done did not return to the probe', focus)
    compact = open_panel()
    tap(next(n for n in compact if n['id'] == 'com.android.systemui:id/volume_dialog_settings'))
    time.sleep(1)
    assert any(n['text'] == 'Done' for n in nodes()), 'Expanded panel did not reopen'
    shell('input', 'keyevent', 'BACK')
    time.sleep(.5)
    assert not any(n['class'] == 'android.widget.SeekBar' for n in nodes()), 'Back did not close the panel'
    focus = next(line for line in shell('dumpsys', 'window').splitlines() if 'mCurrentFocus=' in line)
    assert 'os.omarchy.uicheck/' in focus, ('Back navigated away from the calling app', focus)
    assert shell('pidof', 'com.android.systemui') == ui_pid, 'SystemUI restarted'
    assert volume()[0] == original, 'Media volume was not restored'
    print('PASS: native compact and expanded volume controls; accessible slider ranges; '
          'volume keys and slider touch change and restore media level; '
          'Done and Back dismiss; SystemUI stable')
finally:
    try:
        if volume()[0] != original:
            launch_probe()
            set_with_keys(original)
    finally:
        if timeout == 'null':
            shell('settings', 'delete', 'secure', 'volume_dialog_dismiss_timeout')
        else:
            shell('settings', 'put', 'secure', 'volume_dialog_dismiss_timeout', timeout)
        shell('input', 'keyevent', 'HOME')
