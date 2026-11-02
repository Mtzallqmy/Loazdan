#!/usr/bin/env python3
"""Validate that a signed APK contains the exact checked-in release content."""
import json,hashlib,zipfile,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
with zipfile.ZipFile(sys.argv[1]) as apk:
 libs=[n for n in apk.namelist() if n.startswith('lib/') and n.endswith('.so')]
 assert libs and all(n.startswith('lib/arm64-v8a/') for n in libs),libs
 b=json.loads(apk.read('assets/bundled-manifest.json'))
 assert not b['fixtureOnly']
 raw=apk.read('assets/'+b['path'])
 assert len(raw)==b['bytes'] and hashlib.sha256(raw).hexdigest()==b['sha256']
 ps=[json.loads(l) for l in raw.decode().splitlines()]
 assert len(ps)==120 and all(p['status']=='published' and not p['fixtureOnly'] for p in ps)
 assert raw==(ROOT/'packs/opening-pack-120/data/puzzles.jsonl').read_bytes()
 media=set()
 for p in ps:
  for im in p['payload'].get('images',[]):
   media.add(im['assetPath']);data=apk.read('assets/'+im['assetPath']);assert hashlib.sha256(data).hexdigest()==im['sha256']
 assert 'assets/pack-public-keys.json' in apk.namelist()
 print(json.dumps(dict(puzzles=len(ps),media=len(media),nativeLibraries=libs,contentSha256=b['sha256'])))
