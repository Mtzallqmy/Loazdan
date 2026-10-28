"""Validate debug schemas, authored drafts, stable original bytes and solutions."""
import collections,hashlib,json,pathlib,subprocess
import jsonschema
root=pathlib.Path(__file__).resolve().parents[1];assets=root/'apps/android/app/src/debug/assets'
m=json.loads((assets/'bundled-manifest.json').read_text());data=(assets/m['path']).read_bytes()
jsonschema.Draft202012Validator(json.loads((root/'packages/content-schema/bundled-manifest.schema.json').read_text())).validate(m)
assert len(data)==m['bytes'] and hashlib.sha256(data).hexdigest()==m['sha256']
s=json.loads((root/'contracts/puzzle.schema.json').read_text());assert s==json.loads((root/'packages/contracts/puzzle.schema.json').read_text())
validator=jsonschema.Draft202012Validator(s,format_checker=jsonschema.FormatChecker());ps=[json.loads(x) for x in data.splitlines()];counts=collections.Counter()
assert len({p['puzzleId'] for p in ps})==len(ps)
for p in ps:
 validator.validate(p);assert p['fixtureOnly'] and p['status']=='draft';t=p['type'];v=p['payload'];counts[t]+=1
 if t=='choice':assert len(v['options'])==4 and v['correctId'] in [x['id'] for x in v['options']]
 elif t=='crossword':
  cells={};intersections=0
  for e in v['entries']:
   for i,token in enumerate(e['tokens']):
    row=e['row']+(i if e['direction']=='down' else 0);col=e['col']-(i if e['direction']=='across' else 0)
    assert 0<=row<v['rows'] and 0<=col<v['cols']
    if (row,col) in cells:assert cells[row,col]==token;intersections+=1
    cells[row,col]=token
  assert intersections>0
 elif t=='wordsearch':
  assert len({len(row) for row in v['grid']})==1
  for w in v['words']:
   path=w['path'];assert len({(c['row'],c['col']) for c in path})==len(path)
   assert ''.join(v['grid'][c['row']][c['col']] for c in path)==w['label']
   steps=[(b['row']-a['row'],b['col']-a['col']) for a,b in zip(path,path[1:])];assert len(set(steps))==1 and max(abs(x) for x in steps[0])==1
 elif t=='anagram':
  fixed={x['index']:x['token'] for x in v['fixedSlots']};assert len(fixed)==len(v['fixedSlots'])
  for answer in v['acceptedAnswers']:
   assert all(i<len(answer) and answer[i]==token for i,token in fixed.items())
   assert not (collections.Counter(answer)-collections.Counter(v['tiles']))
 elif t=='matching' and 'options' in v:
  assert len(v['options'])==4 and len({o['id'] for o in v['options']})==4 and v['correctId'] in [o['id'] for o in v['options']]
 elif t=='matching':
  left={o['id'] for o in v['left']};right={o['id'] for o in v['right']};assert len(left)==len(v['left'])==len(v['pairs'])==len(right)
  assert {p['leftId'] for p in v['pairs']}==left and {p['rightId'] for p in v['pairs']}==right
 elif t=='order':assert sorted(v['correctOrder'])==sorted(o['id'] for o in v['items'])
for t in ['choice','crossword','wordsearch','anagram','matching','order']:assert counts[t]>=2
for t,n in [('matching',6),('anagram',4),('order',4)]:assert len({p['payload'].get('variant') for p in ps if p['type']==t})==n
old=subprocess.check_output(['git','show','HEAD:apps/android/app/src/debug/assets/data/vocabulary.jsonl'],cwd=root);assert data.splitlines()[:4]==old.splitlines()[:4]
release=root/'apps/android/app/src/release/assets';assert not list(release.rglob('*.jsonl'))
print(f'{len(ps)} debug-only solvable puzzles, variants, stable original definitions and SHA-256 validated')
