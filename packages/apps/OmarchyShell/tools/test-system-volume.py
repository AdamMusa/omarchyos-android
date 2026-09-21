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

def navigation(current):
    controls = {}
    for description in ['Omarchy menu', 'Omarchy settings', 'Back']:
        matches = [n for n in current if n['description'] == description]
        assert len(matches) == 1, (description, matches)
        assert matches[0]['clickable'], matches[0]
        controls[description] = matches[0]
    clocks = [n for n in current if re.fullmatch(r'(?:\w+ )?\d{2}:\d{2}', n['text'])]
    assert len(clocks) == 1 and clocks[0]['clickable'], clocks
    controls['clock'] = clocks[0]
    return controls

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
    reference_bar = navigation(compact)
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
    expanded_bar = navigation(expanded)
    for name, reference in reference_bar.items():
        assert expanded_bar[name]['bounds'] == reference['bounds'], (name, reference, expanded_bar[name])
    sliders = {n['description']: n for n in expanded if n['class'] == 'android.widget.SeekBar'}
    capture('expanded', expanded)
    expected_streams = {'Media', 'Call', 'Ring', 'Notification', 'Alarm'}
    seen_streams = set(sliders)
    if not expected_streams <= seen_streams:
        # At large font sizes the fixed navbar leaves fewer rows in the first
        # viewport. Verify the remaining streams can be reached by scrolling.
        left, top, right, _ = map(int, sliders['Media']['bounds'].split())
        # Begin within the last visible row, not the fixed footer above Done.
        bottom = max(int(n['bounds'].split()[1]) for n in sliders.values()) - 40
        x = (left + right) // 2
        for _ in range(3):
            shell('input', 'swipe', str(x), str(bottom), str(x), str(top), '350')
            time.sleep(.3)
            scrolled = nodes()
            seen_streams.update(n['description'] for n in scrolled
                                if n['class'] == 'android.widget.SeekBar')
            for name, reference in reference_bar.items():
                assert navigation(scrolled)[name]['bounds'] == reference['bounds']
            if expected_streams <= seen_streams:
                break
        capture('expanded-scrolled', scrolled)
        assert expected_streams <= seen_streams, seen_streams
        for _ in range(3):
            shell('input', 'swipe', str(x), str(top), str(x), str(bottom), '350')
        time.sleep(.3)
        expanded = nodes()
        sliders = {n['description']: n for n in expanded if n['class'] == 'android.widget.SeekBar'}
    assert sliders['Media']['range']['current'] == original, sliders['Media']
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
    tap(navigation(nodes())['Back'])
    time.sleep(.5)
    assert not any(n['class'] == 'android.widget.SeekBar' for n in nodes()), 'Navbar Back did not close the panel'
    focus = next(line for line in shell('dumpsys', 'window').splitlines() if 'mCurrentFocus=' in line)
    assert 'os.omarchy.uicheck/' in focus, ('Navbar Back navigated away from the calling app', focus)
    compact = open_panel()
    tap(next(n for n in compact if n['id'] == 'com.android.systemui:id/volume_dialog_settings'))
    time.sleep(1)
    shell('input', 'keyevent', 'BACK')
    time.sleep(.5)
    assert not any(n['class'] == 'android.widget.SeekBar' for n in nodes()), 'Back did not close the panel'
    focus = next(line for line in shell('dumpsys', 'window').splitlines() if 'mCurrentFocus=' in line)
    assert 'os.omarchy.uicheck/' in focus, ('Back navigated away from the calling app', focus)
    for action in ['Omarchy menu', 'Omarchy settings', 'clock']:
        launch_probe()
        compact = open_panel()
        tap(next(n for n in compact if n['id'] == 'com.android.systemui:id/volume_dialog_settings'))
        time.sleep(1)
        tap(navigation(nodes())[action])
        time.sleep(1)
        current = nodes()
        assert not any(n['class'] == 'android.widget.SeekBar' for n in current), ('Modal remained open', action)
        activity = shell('dumpsys', 'activity', 'activities')
        assert any('topResumedActivity=' in line and 'os.omarchy.shell/' in line
                   for line in activity.splitlines()), (action, activity[-2000:])
        capture('navigate-' + action.replace(' ', '-').lower(), current)
        shell('input', 'keyevent', 'BACK')
    launch_probe()
    compact = open_panel()
    tap(next(n for n in compact if n['id'] == 'com.android.systemui:id/volume_dialog_settings'))
    time.sleep(1)
    gear = navigation(nodes())['Omarchy settings']
    left, top, right, bottom = map(int, gear['bounds'].split())
    x, y = str((left + right) // 2), str((top + bottom) // 2)
    shell('input', 'swipe', x, y, x, y, '800')
    time.sleep(1)
    current = nodes()
    assert not any(n['class'] == 'android.widget.SeekBar' and n['description'] in expected_streams
                   for n in current), 'Gear long-press left the volume modal over Quick Settings'
    focus = next(line for line in shell('dumpsys', 'window').splitlines() if 'mCurrentFocus=' in line)
    assert 'NotificationShade' in focus, ('Gear long-press did not open Quick Settings', focus)
    capture('navigate-quick-settings', current)
    shell('cmd', 'statusbar', 'collapse')
    assert shell('pidof', 'com.android.systemui') == ui_pid, 'SystemUI restarted'
    assert volume()[0] == original, 'Media volume was not restored'
    print('PASS: native compact and expanded volume controls; accessible slider ranges; '
          'volume keys and slider touch change and restore media level; '
          'Done, navbar Back and hardware Back dismiss; '
          'one matching modal navbar opens Home menu/settings/calendar and Quick Settings; SystemUI stable')
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
