#!/usr/bin/env python3
"""Device regression: Android Settings -> Back must restore the same Home scene.

Run with an unlocked emulator/device showing a static Omarchy wallpaper. Requires
adb on PATH; uses the selected device (ANDROID_SERIAL or --serial), no extra libs.
"""
import argparse
import os
import struct
import subprocess
import time

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', default=os.environ.get('ANDROID_SERIAL'))
parser.add_argument('--rounds', type=int, default=2)
args = parser.parse_args()
if args.rounds < 1:
    parser.error('--rounds must be positive')
adb = ['adb'] + (['-s', args.serial] if args.serial else [])


def run(*command):
    return subprocess.check_output(adb + list(command), timeout=30)


def home_is_resumed():
    state = run('shell', 'dumpsys', 'activity', 'activities').decode()
    return any('topResumedActivity=' in line
               and ('os.omarchy.shell/.OmarchyActivity' in line
                    or 'os.omarchy.shell/os.omarchy.shell.OmarchyActivity' in line)
               for line in state.splitlines())


def frame():
    data = run('exec-out', 'screencap')
    width, height, fmt = struct.unpack_from('<III', data)
    assert fmt in (1, 2), f'Expected RGBA/RGBX screen, got format {fmt}'
    pixels = data[-width * height * 4:]
    assert len(pixels) == width * height * 4
    # Sample above the gesture bar, excluding the clock, which can tick mid-test.
    samples = []
    for y in range(4, int(height * .94), 8):
        for x in range(4, width, 8):
            if y < height * .1 and width * .25 < x < width * .7:
                continue
            offset = (y * width + x) * 4
            samples.append(tuple(pixels[offset:offset + 3]))
    return width, height, samples


run('shell', 'input', 'keyevent', 'KEYCODE_HOME')
time.sleep(2)
assert home_is_resumed(), 'Omarchy must be the default Home app'
pid = run('shell', 'pidof', 'os.omarchy.shell').strip()
width, height, reference = frame()
# Only compare points that distinguish the home scene from its blank clear color.
visible = [i for i, rgb in enumerate(reference) if max(abs(c - 16) for c in rgb) > 32]
assert len(visible) > 20, 'Home is already blank or has too little visible content'
for iteration in range(args.rounds):
    for service in ('WIFI_SETTINGS', 'BLUETOOTH_SETTINGS', 'SOUND_SETTINGS',
                    'DISPLAY_SETTINGS', 'DREAM_SETTINGS'):
        result = run('shell', 'am', 'start', '-W', '-a', 'android.settings.' + service)
        assert b'Error:' not in result and not home_is_resumed(), result.decode()
        time.sleep(1)
        run('shell', 'input', 'keyevent', 'KEYCODE_BACK')
        time.sleep(1)
        assert home_is_resumed(), f'{service}: Back did not return to Home'
        assert run('shell', 'pidof', 'os.omarchy.shell').strip() == pid, 'Shell restarted'
        w, h, current = frame()
        assert (w, h) == (width, height), 'Screen dimensions changed'
        matched = sum(max(abs(a - b) for a, b in zip(reference[i], current[i])) < 24
                      for i in visible) / len(visible)
        assert matched > .95, f'{service}: home scene missing/changed ({matched:.1%} restored)'
        print(f'PASS: round {iteration + 1} {service} -> Back ({matched:.1%} restored)', flush=True)
