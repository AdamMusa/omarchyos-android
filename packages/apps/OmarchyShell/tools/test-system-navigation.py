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
def probe():
 # UiAutomation may briefly return no windows while its service attaches.
 for attempt in range(3):
  nodes=json.loads(shell('CLASSPATH=/data/local/tmp/native-bar-probe.dex app_process /system/bin NativeBarProbe'))
  if nodes:return nodes
  time.sleep(.5)
 return nodes
def bar(nodes):
 for name in ['Omarchy menu','Omarchy settings','Back']:
  assert sum(n['description']==name for n in nodes)<=1,('Duplicate navbar control',name,nodes)
 controls={n['description']:n for n in nodes if n['description'] in ['Omarchy menu','Omarchy settings','Back']}
 assert 'Omarchy menu' in controls and 'Omarchy settings' in controls,nodes
 # Service screens must retain the Home design, not an OM/hamburger substitute.
 assert controls['Omarchy menu']['text']=='\ue900',controls
 assert controls['Omarchy settings']['text']=='\uf013',controls
 for n in controls.values():
  x,y,r,b=map(int,n['bounds'].split());assert r>x and b>y and n['clickable'],n
 clocks=[n for n in nodes if re.fullmatch(r'(?:\w+ )?\d{2}:\d{2}',n['text'])]
 assert len(clocks)==1,clocks
 controls['clock']=clocks[0]
 # Back mounts in its reserved slot without overlapping the other controls.
 rectangles=[tuple(map(int,n['bounds'].split())) for n in controls.values()]
 for index,(left,top,right,bottom) in enumerate(rectangles):
  for other_left,other_top,other_right,other_bottom in rectangles[index+1:]:
   assert (right<=other_left or other_right<=left or bottom<=other_top or other_bottom<=top),('Overlapping navbar controls',controls)
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
shell('input','keyevent','APP_SWITCH');time.sleep(1)
overview=bar(probe())
for name in ['Omarchy menu','Omarchy settings','clock']:
 assert overview[name]['bounds']==home[name]['bounds'],('Overview',name,home,overview)
records['overview']=overview
shell('input','keyevent','BACK');shell('input','keyevent','HOME');time.sleep(.5)
for panel in ['expand-notifications','expand-settings']:
 shell('cmd','statusbar',panel);time.sleep(1)
 current=bar(probe());assert 'Back' in current,current
 for name in ['Omarchy menu','Omarchy settings','clock']:
  assert current[name]['bounds']==home[name]['bounds'],(panel,name,home,current)
 records[panel]=current
 if panel=='expand-settings':
  # Scrolling the service body must not move its navbar.
  _,_,screen_width,bar_height=map(int,current['Omarchy settings']['bounds'].split())
  shell('input','swipe',str(screen_width//2),str(bar_height*8),str(screen_width//2),str(bar_height*3),'350')
  time.sleep(.5)
  scrolled=bar(probe())
  for name in ['Omarchy menu','Omarchy settings','clock']:
   assert scrolled[name]['bounds']==current[name]['bounds'],(panel,name,current,scrolled)
  if 'Back' in scrolled:
   assert scrolled['Back']['bounds']==current['Back']['bounds'],(current,scrolled)
  else:
   # If all tiles fit, Android treats the upward swipe as shade dismissal.
   # Reopen it to test the explicit Back control too.
   shell('cmd','statusbar',panel);time.sleep(1)
   current=bar(probe());assert 'Back' in current,current
 # Android may first return from Quick Settings to the notification shade.
 for attempt in range(2):
  x,y,r,b=map(int,current['Back']['bounds'].split())
  shell('input','tap',str((x+r)//2),str((y+b)//2));time.sleep(1)
  current=bar(probe())
  if 'Back' not in current:break
 assert 'Back' not in current,('Shade Back did not return Home',panel,current)
# Tile editing is a nested service too; it must retain the same navbar.
shell('cmd','statusbar','expand-settings');time.sleep(1)
edit=next(n for n in probe() if n['id']=='com.android.systemui:id/qs_edit_mode_button')
x,y,r,b=map(int,edit['bounds'].split())
shell('input','tap',str((x+r)//2),str((y+b)//2));time.sleep(1)
editing=bar(probe());assert 'Back' in editing,editing
for name in ['Omarchy menu','Omarchy settings','clock']:
 assert editing[name]['bounds']==home[name]['bounds'],('QS edit',name,home,editing)
records['quick-settings-edit']=editing
x,y,r,b=map(int,editing['Back']['bounds'].split())
shell('input','tap',str((x+r)//2),str((y+b)//2));time.sleep(1)
assert any(n['id']=='com.android.systemui:id/qs_edit_mode_button' for n in probe()),'Back did not leave tile editing'
shell('cmd','statusbar','collapse');time.sleep(.5)
assert 'Back' not in bar(probe()),'Back remains mounted after shade closes'
assert shell('pidof','com.android.systemui').strip()==original_pid,'SystemUI restarted'
print(json.dumps(records,indent=2))
print('PASS: one native bar across Home, five services, nested Bluetooth pairing, Overview, notifications, Quick Settings and tile editing; fixed logo/gear/clock bounds while scrolling; Back returns Home; SystemUI process stable')
