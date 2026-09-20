#!/usr/bin/env python3
"""Refresh bundled artwork/catalog from pinned Omarchy sources (macOS sips)."""
import concurrent.futures
import argparse
import io
import json
from pathlib import Path
import subprocess
import tempfile
import urllib.request
import zipfile
from urllib.parse import quote

REVISION = '8675600e9ea0c6b6011de378b0625172b9cfdd46'
ROOT = Path(__file__).resolve().parents[1]
MOBILE = ROOT / 'mobile'

def fetch(url):
    request = urllib.request.Request(url, headers={'User-Agent': 'OmarchyOS-Android-Themes'})
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--wallpapers-only", action="store_true", help="Keep the existing marketplace snapshot")
    args = parser.parse_args()
    themes = sorted(p.name for p in (ROOT/'third_party/omarchy/themes').iterdir() if p.is_dir())
    tree = json.loads(fetch(f'https://api.github.com/repos/omacom/omarchy/git/trees/{REVISION}?recursive=1'))
    if tree.get('truncated'): raise RuntimeError('Upstream tree is incomplete')
    sources = {}
    for theme in themes:
        prefix = f'themes/{theme}/backgrounds/'
        images = sorted(entry['path'] for entry in tree['tree']
                        if entry['type'] == 'blob' and entry['path'].startswith(prefix)
                        and Path(entry['path']).suffix.lower() in {'.webp', '.jpg', '.jpeg', '.png'})
        if not images: raise RuntimeError(f'{theme} has no upstream wallpaper')
        # Omarchy cycles backgrounds in filename order. Start with its first
        # artwork rather than substituting the optional Omarchy wordmark.
        sources[theme] = f'https://raw.githubusercontent.com/omacom/omarchy/{REVISION}/' + quote(images[0])
    destination = MOBILE/'wallpapers'
    destination.mkdir(exist_ok=True)
    def artwork(item):
        theme, url = item
        with tempfile.TemporaryDirectory() as temporary:
            source = Path(temporary)/Path(url).name
            source.write_bytes(fetch(url))
            subprocess.run(['sips','-s','format','jpeg','-s','formatOptions','88','--resampleHeightWidthMax','2560',str(source),'--out',str(destination/f'{theme}.jpg')],check=True,stdout=subprocess.DEVNULL)
        (destination/f'{theme}.png').unlink(missing_ok=True)
        return theme
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        for name in pool.map(artwork, sources.items()): print('Wallpaper:', name, flush=True)
    (destination/'sources.json').write_text(json.dumps(sources,indent=2)+'\n')
    if args.wallpapers_only:
        print(f'{len(themes)} official default wallpapers updated')
        return
    revision = json.loads(fetch('https://api.github.com/repos/omacom/omarchy-theme-registry/commits/master'))['sha']
    archive = zipfile.ZipFile(io.BytesIO(fetch(f'https://codeload.github.com/omacom/omarchy-theme-registry/zip/{revision}')))
    entries = [json.loads(archive.read(name)) for name in archive.namelist() if '/themes/' in name and name.endswith('.json')]
    entries = [e for e in entries if e['slug'] not in themes]
    (MOBILE/'marketplace.json').write_text(json.dumps(sorted(entries,key=lambda e:e['name'].lower()),indent=2)+'\n')
    (MOBILE/'theme-sources.json').write_text(json.dumps({'omarchy':REVISION,'registry':revision,'default_themes':themes},indent=2)+'\n')
    print(f'{len(themes)} defaults; {len(entries)} community themes')

if __name__ == '__main__': main()
