#!/usr/bin/env python3
"""Reboot a development device and record through Omarchy Home's first frame.

This records Android's composed display, not the emulator host window. Inspect
both before concluding that a recording artifact is a visible boot defect.
Secure devices must be unlocked normally; this tool never bypasses keyguard.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time
import uuid

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', required=True)
parser.add_argument('--output', type=Path, required=True, help='New evidence directory')
parser.add_argument('--timeout', type=int, default=120, help='Host seconds to wait for Home (20–150)')
parser.add_argument('--size', help='Recording dimensions, e.g. 540x1200, to reduce encoder load')
args = parser.parse_args()
if not 20 <= args.timeout <= 150:
    parser.error('--timeout must be between 20 and 150 seconds')
if args.size and not re.fullmatch(r'[1-9][0-9]*x[1-9][0-9]*', args.size):
    parser.error('--size must be WIDTHxHEIGHT')
args.output.mkdir(parents=True, exist_ok=False)
adb = ['adb', '-s', args.serial]


def read(command, timeout=10):
    return subprocess.check_output(adb + ['shell', command], timeout=timeout).decode(
        'utf-8', errors='replace').strip()


report = {'serial': args.serial, 'home_frame_observed': False, 'recording_size': args.size,
          'visual_review_required': True}
old_boot = read('cat /proc/sys/kernel/random/boot_id')
report['previous_boot_id'] = old_boot
started = time.monotonic()
subprocess.run(adb + ['reboot'], check=True, timeout=15)
boot = old_boot
while time.monotonic() - started < 90:
    try:
        result = subprocess.run(adb + ['shell', 'cat /proc/sys/kernel/random/boot_id'],
                                capture_output=True, timeout=3)
        candidate = result.stdout.decode(errors='replace').strip()
        if result.returncode == 0 and candidate and candidate != old_boot:
            boot = candidate
            break
    except subprocess.TimeoutExpired:
        pass
    time.sleep(.25)
report['boot_id'] = boot
report['new_boot_host_seconds'] = round(time.monotonic() - started, 3)
if boot == old_boot:
    (args.output / 'capture.json').write_text(json.dumps(report, indent=2))
    raise SystemExit('No new boot observed; no recording started')

remote = '/data/local/tmp/omarchy-boot-' + uuid.uuid4().hex
record_started = time.monotonic()
with (args.output / 'screenrecord.log').open('wb') as log:
    size_option = '--size ' + args.size if args.size else ''
    recording = subprocess.Popen(adb + ['shell',
        f'echo $$ > {remote}.pid; exec screenrecord --verbose --time-limit 180 {size_option} {remote}.mp4'],
        stdout=log, stderr=subprocess.STDOUT)
    try:
        while time.monotonic() - record_started < args.timeout:
            if recording.poll() is not None:
                break
            try:
                events = read('logcat -d -s omarchy-shell:I', timeout=5)
                if 'Home frame ready; boot curtain removed' in events:
                    report['home_frame_observed'] = True
                    report['home_frame_host_seconds'] = round(time.monotonic() - started, 3)
                    # Encoding can lag the display. Save an independent final
                    # screenshot and leave additional time for queued frames.
                    with (args.output / 'home-signal.png').open('wb') as image:
                        subprocess.run(adb + ['exec-out', 'screencap', '-p'],
                                       stdout=image, timeout=15, check=True)
                    time.sleep(5)
                    with (args.output / 'settled.png').open('wb') as image:
                        subprocess.run(adb + ['exec-out', 'screencap', '-p'],
                                       stdout=image, timeout=15, check=True)
                    break
            except (subprocess.TimeoutExpired, subprocess.CalledProcessError):
                pass
            time.sleep(1)
    finally:
        if recording.poll() is None:
            # A second reboot invalidates the recorded PID. Never signal a
            # potentially reused PID in another boot.
            try:
                if read('cat /proc/sys/kernel/random/boot_id') == boot:
                    pid = read(f'cat {remote}.pid')
                    if pid.isdigit():
                        read('kill -INT ' + pid)
            except subprocess.SubprocessError as error:
                report['stop_error'] = str(error)
            try:
                recording.wait(timeout=15)
            except subprocess.TimeoutExpired:
                recording.terminate()
                recording.wait(timeout=5)
        report['recorder_returncode'] = recording.returncode
        report['capture_host_seconds'] = round(time.monotonic() - record_started, 3)
        (args.output / 'capture.json').write_text(json.dumps(report, indent=2))

# Keep raw log bytes: Android's buffers can contain non-UTF-8 records.
with (args.output / 'logcat.txt').open('wb') as output:
    subprocess.run(adb + ['logcat', '-d', '-v', 'monotonic'], stdout=output,
                   stderr=subprocess.STDOUT, timeout=30, check=True)
subprocess.run(adb + ['pull', remote + '.mp4', str(args.output / 'boot.mp4')],
               check=True, timeout=30)
read(f'rm -f {remote}.pid {remote}.mp4')
print(json.dumps(report, indent=2))
if not report['home_frame_observed'] or report['recorder_returncode'] != 0:
    raise SystemExit('Incomplete capture; inspect capture.json and screenrecord.log')
print('Observed Home signal; inspect both screenshots and the video before judging visual continuity')
