#!/usr/bin/env python3
"""Runtime integration check for the native bar on Home and Android services."""
import argparse,json,os,re,subprocess,time
import xml.etree.ElementTree as ET
from pathlib import Path
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--serial', default=os.environ.get('ANDROID_SERIAL'))
parser.add_argument('--probe-dex', type=Path, required=True)
args = parser.parse_args()
adb = ['adb'] + (['-s', args.serial] if args.serial else [])
subprocess.run(adb + ['push', str(args.probe_dex), '/data/local/tmp/native-bar-probe.dex'], check=True)
def shell(*args):return subprocess.check_output(adb+['shell',*args],text=True,timeout=30)
def probe():return json.loads(shell('CLASSPATH=/data/local/tmp/native-bar-probe.dex app_process /system/bin NativeBarProbe'))
def bar(nodes):
 controls={n['description']:n for n in nodes if n['description'] in ['Omarchy menu','Omarchy settings','Back']}
 assert 'Omarchy menu' in controls and 'Omarchy settings' in controls,nodes
 for n in controls.values():
  x,y,r,b=map(int,n['bounds'].split());assert r>x and b>y and n['clickable'],n
 clocks=[n for n in nodes if re.fullmatch(r'(?:\w+ )?\d{2}:\d{2}',n['text'])]
 assert len(clocks)==1,clocks
 controls['clock']=clocks[0]
 return controls
original_pid=shell('pidof','com.android.systemui').strip()
shell('input','keyevent','HOME');time.sleep(.5)
home=bar(probe());assert 'Back' not in home,home
records={'home':home}
for action in ['WIFI_SETTINGS','BLUETOOTH_SETTINGS','BATTERY_SAVER_SETTINGS','DISPLAY_SETTINGS','SOUND_SETTINGS']:
 shell('am','force-stop','com.android.settings');shell('input','keyevent','HOME')
 shell('am','start','-W','-a','android.settings.'+action);time.sleep(.5)
 current=bar(probe());assert 'Back' in current,current
 for name in ['Omarchy menu','Omarchy settings']:
  assert current[name]['bounds']==home[name]['bounds'],(name,home,current)
 assert current['clock']['bounds']==home['clock']['bounds'],(home,current)
 if action == 'BLUETOOTH_SETTINGS':
  # A nested service must pop one level, keeping Back mounted until Home.
  shell('uiautomator','dump','/data/local/tmp/omarchy-navigation.xml')
  page=ET.fromstring(shell('cat','/data/local/tmp/omarchy-navigation.xml'))
  pair=next(n for n in page.iter('node') if n.get('text')=='Pair new device')
  x,y,r,b=map(int,re.findall(r'\d+',pair.get('bounds')))
  shell('input','tap',str((x+r)//2),str((y+b)//2));time.sleep(.5)
  nested=bar(probe())
  assert 'Back' in nested,nested
  for name in ['Omarchy menu','Omarchy settings','clock','Back']:
   assert nested[name]['bounds']==current[name]['bounds'],(name,current,nested)
  state=shell('dumpsys','activity','activities')
  assert any('topResumedActivity=' in l and 'com.android.settings/.SubSettings ' in l for l in state.splitlines()),state[-3000:]
  x,y,r,b=map(int,nested['Back']['bounds'].split())
  shell('input','tap',str((x+r)//2),str((y+b)//2));time.sleep(.5)
  state=shell('dumpsys','activity','activities')
  assert any('topResumedActivity=' in l and 'ConnectedDeviceDashboardActivity ' in l for l in state.splitlines()),state[-3000:]
  assert 'Back' in bar(probe()),'Back unmounted before returning Home'
  records['BLUETOOTH_PAIRING']=nested
 x,y,r,b=map(int,current['Back']['bounds'].split());shell('input','tap',str((x+r)//2),str((y+b)//2))
 time.sleep(.5)
 state=shell('dumpsys','activity','activities')
 assert any('topResumedActivity=' in l and 'os.omarchy.shell/' in l for l in state.splitlines()),state[-3000:]
 assert 'Back' not in bar(probe()),'Back remains mounted on Home'
 records[action]=current
assert shell('pidof','com.android.systemui').strip()==original_pid,'SystemUI restarted'
print(json.dumps(records,indent=2))
print('PASS: one native bar across Home, five services and nested Bluetooth pairing; fixed logo/gear/clock bounds; Back pops one level and unmounts on Home; SystemUI process stable')
