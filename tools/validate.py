#!/usr/bin/env python3
"""Stage 01 offline contract checks. Does not start a DB, server or Android app."""
import copy,hashlib,json,re,sys,unicodedata
from pathlib import Path
from importlib.metadata import version
from jsonschema import Draft202012Validator,FormatChecker
from referencing import Registry,Resource
from openapi_spec_validator import validate_spec
from pglast import parse_sql
R=Path(__file__).resolve().parents[1]
checks=[]
def load(p):return json.loads((R/p).read_text())
def check(name,fn):
 try:fn();checks.append({'name':name,'passed':True})
 except Exception as e:checks.append({'name':name,'passed':False,'error':str(e)})
def must(x,msg='assertion failed'):
 if not x:raise AssertionError(msg)
def invalid(fn):
 try:fn()
 except (ValueError,AssertionError):return
 raise AssertionError('Invalid input accepted')
def norm(x,policy='arabic_basic_v1'):
 x=unicodedata.normalize('NFC',x).strip()
 return re.sub(r'\s+',' ',re.sub('[\u0640\u064b-\u065f\u0670]','',x)) if policy=='arabic_basic_v1' else x
def semantic(d):
 p=d['payload'];t=d['type']
 must(d['status'] not in ('approved','published') or not d['fixtureOnly'])
 must(len({h['id'] for h in d['hints']})==len(d['hints']))
 if t=='crossword':
  cells={};owners={};paths=[];dirs={}
  for e in p['entries']:
   must(e['id'] not in dirs);dirs[e['id']]=e['direction'];path=[]
   for i,token in enumerate(e['tokens']):
    r=e['row']+(i if e['direction']=='down' else 0);c=e['col']-(i if e['direction']=='across' else 0)
    must(0<=r<p['rows'] and 0<=c<p['cols'],'bounds')
    must(len(norm(token))==1,'grid token')
    must((r,c) not in cells or cells[r,c]==token,'crossing conflict')
    must(all(dirs[other]!=e['direction'] for other in owners.get((r,c),[])),'parallel overlap')
    cells[r,c]=token;owners.setdefault((r,c),[]).append(e['id']);path.append((r,c))
   paths.append(path)
  connected={0}
  while True:
   expanded=connected | {j for j,b in enumerate(paths) if any(set(b)&set(paths[i]) for i in connected)}
   if expanded==connected:break
   connected=expanded
  must(len(connected)==len(paths),'disconnected')
  runs=[]
  for r,c in cells:
   if (r,c+1) not in cells:
    run=[];cc=c
    while (r,cc) in cells:run.append((r,cc));cc-=1
    if len(run)>=2:runs.append(run)
   if (r-1,c) not in cells:
    run=[];rr=r
    while (rr,c) in cells:run.append((rr,c));rr+=1
    if len(run)>=2:runs.append(run)
  must(sorted(runs)==sorted(paths),'undeclared adjacency or extended word')
 elif t=='wordsearch':
  grid=p['grid'];must(len({len(r) for r in grid})==1)
  for w in p['words']:
   pts=[(x['row'],x['col']) for x in w['path']];must(len(set(pts))==len(pts))
   must(all(0<=r<len(grid) and 0<=c<len(grid[0]) for r,c in pts))
   steps={(b[0]-a[0],b[1]-a[1]) for a,b in zip(pts,pts[1:])}
   must(len(steps)==1 and all(max(abs(x),abs(y))==1 for x,y in steps))
   must(''.join(grid[r][c] for r,c in pts)==w['label'])
 elif t=='anagram':
  for answer in p['acceptedAnswers']:must(sorted(p['tiles'])==sorted(norm(answer)))
  must(len({x['index'] for x in p['fixedSlots']})==len(p['fixedSlots']))
  for f in p['fixedSlots']:must(f['index']<len(p['tiles']) and all(norm(a)[f['index']]==f['token'] for a in p['acceptedAnswers']))
 elif t=='choice':must(len({o['id'] for o in p['options']})==len(p['options']) and p['correctId'] in {o['id'] for o in p['options']})
 elif t=='matching':
  left={x['id'] for x in p['left']};right={x['id'] for x in p['right']}
  must(len(left)==len(p['left']) and len(right)==len(p['right']))
  must({x['leftId'] for x in p['pairs']}==left and {x['rightId'] for x in p['pairs']}==right and len(p['pairs'])==len(left)==len(right))
 elif t=='order':must(len(set(p['correctOrder']))==len(p['items']) and set(p['correctOrder'])=={x['id'] for x in p['items']})
 elif t=='sequence':must(all(b-a==2 for a,b in zip(p['terms'],p['terms'][1:])) and p['answer']==p['terms'][-1]+2,'fixture announced rule')
 elif t in ('text_answer','image_guess'):must(len({norm(a) for a in p['acceptedAnswers']})==len(p['acceptedAnswers']))
def manifest_semantic(m,root):
 paths={};hashes={}
 for f in m['files']:
  path=f['path'];must(path not in paths and '..' not in path.split('/') and '\\' not in path)
  raw=(root/path).read_bytes();must(len(raw)==f['bytes'] and hashlib.sha256(raw).hexdigest()==f['sha256'])
  offset=0
  for ch in f['chunks']:
   part=raw[offset:offset+ch['bytes']];must(hashlib.sha256(part).hexdigest()==ch['sha256'])
   must(ch['sha256'] not in hashes or hashes[ch['sha256']]==ch['bytes']);hashes[ch['sha256']]=ch['bytes'];offset+=ch['bytes']
  must(offset==len(raw));paths[path]=f
 must(m['downloadBytes']==sum(hashes.values()) and m['installedBytes']==sum(x['bytes'] for x in m['files']))
 refs={r['referenceId'] for r in m['references']};must(len(refs)==len(m['references']))
 for r in m['references']:must(r['noticePath'] in paths and paths[r['noticePath']]['kind']=='rights')
 assets={a['assetId'] for a in m['assets']};must(len(assets)==len(m['assets']))
 for a in m['assets']:must(a['path'] in paths and paths[a['path']]['kind']=='media' and a['sourceReferenceId'] in refs)
 must(set(m['sourceRefs'])<=refs)
def main():
 schemas={n:load('contracts/'+n+'.schema.json') for n in ['puzzle','manifest','sync','manifest-payload']}
 for n,s in schemas.items():check('schema:'+n,lambda s=s:Draft202012Validator.check_schema(s))
 api=load('contracts/openapi.json');check('openapi:3.1',lambda:validate_spec(api))
 for name,comp in [('puzzle','Puzzle'),('manifest','Manifest'),('sync','SyncRequest')]:
  check('embedded-schema-parity:'+name,lambda name=name,comp=comp:must({k:v for k,v in schemas[name].items() if k not in ('$id','$schema')}==api['components']['schemas'][comp]))
 v=Draft202012Validator(schemas['puzzle'],format_checker=FormatChecker())
 for f in sorted((R/'examples/puzzles').glob('*.json')):
  d=json.loads(f.read_text());check('puzzle-schema:'+f.stem,lambda d=d:v.validate(d));check('puzzle-semantics:'+f.stem,lambda d=d:semantic(d))
 sy=load('examples/sync-request.json');sv=Draft202012Validator(schemas['sync'],format_checker=FormatChecker());check('sync-example',lambda:sv.validate(sy))
 bad=copy.deepcopy(sy);bad['operations'][0]['kind']='set_preference';bad['operations'][0]['payload']={'key':'sound','value':'dark','baseVersion':0}
 check('reject-preference-mismatch',lambda:must(not sv.is_valid(bad)))
 large=copy.deepcopy(bad);large['operations'][0]['payload']={'key':'theme','value':'dark','baseVersion':9007199254740991};check('cas-safe-integer-range',lambda:sv.validate(large))
 schema_doc={'$schema':'https://json-schema.org/draft/2020-12/schema','components':api['components']}
 reg=Registry().with_resource('urn:lughzdan:openapi',Resource.from_contents(schema_doc))
 cv=Draft202012Validator({'$ref':'urn:lughzdan:openapi#/components/schemas/Change'},registry=reg,format_checker=FormatChecker())
 wrongchange={'version':1,'ordinal':0,'entityType':'saved_word','entityId':'x','value':{}}
 check('reject-empty-change',lambda:must(not cv.is_valid(wrongchange)))
 ov=Draft202012Validator({'$ref':'urn:lughzdan:openapi#/components/schemas/OperationResult'},registry=reg,format_checker=FormatChecker())
 session_conflict={'operationId':sy['operations'][0]['operationId'],'kind':'complete_session','status':'conflict','serverVersion':None,'code':'ID_REUSE','current':None,'currentVersion':None}
 check('session-conflict-response-schema',lambda:ov.validate(session_conflict))
 cas_conflict=dict(session_conflict,kind='save_word',currentVersion=0,code='VERSION_CONFLICT')
 check('cas-conflict-absent-row',lambda:ov.validate(cas_conflict))
 wrongchange['value']={'vocabularyId':sy['operations'][0]['payload']['puzzleId'],'saved':False,'serverVersion':1}
 check('saved-word-tombstone-schema',lambda:cv.validate(wrongchange))
 for a,b,same in [('قَلَم','قلم',True),('أمل','امل',False),('على','علي',False),('رحمة','رحمه',False),('قـلم','قلم',True),('ا\u0654مل','أمل',True)]:
  check('normalization:'+a,lambda a=a,b=b,same=same:must((norm(a)==norm(b))==same and norm(norm(a))==norm(a)))
 badgrid=load('examples/puzzles/crossword.json');badgrid['payload']['entries'][1]['tokens'][1]='س';check('reject-crossword-conflict',lambda:invalid(lambda:semantic(badgrid)))
 badgrid2=load('examples/puzzles/crossword.json');badgrid2['payload']['entries'][0]['col']=0;check('reject-crossword-bounds',lambda:invalid(lambda:semantic(badgrid2)))
 badschema=load('examples/puzzles/choice.json');badschema['type']='unknown';check('reject-unknown-type',lambda:must(not v.is_valid(badschema)))
 production=load('examples/puzzles/choice.json');production['status']='published';check('reject-published-fixture',lambda:must(not v.is_valid(production)))
 m=load('examples/pack/manifest-unsigned.json');check('manifest-candidate-schema',lambda:Draft202012Validator(schemas['manifest-payload'],format_checker=FormatChecker()).validate(m))
 check('manifest-content-integrity',lambda:manifest_semantic(m,R/'examples/pack'))
 check('reject-unsigned-production-manifest',lambda:must(not Draft202012Validator(schemas['manifest']).is_valid(m)))
 tampered=copy.deepcopy(m);tampered['files'][0]['sha256']='0'*64;check('reject-tampered-file',lambda:invalid(lambda:manifest_semantic(tampered,R/'examples/pack')))
 tampered=copy.deepcopy(m);tampered['assets'][0]['path']='media/missing.png';check('reject-unresolved-asset',lambda:invalid(lambda:manifest_semantic(tampered,R/'examples/pack')))
 tampered=copy.deepcopy(m);tampered['installedBytes']+=1;check('reject-size-mismatch',lambda:invalid(lambda:manifest_semantic(tampered,R/'examples/pack')))
 sql=(R/'database/001_blueprint.sql').read_text();check('postgresql-parse-only',lambda:must(len(parse_sql(sql))>0))
 tables=re.findall(r'CREATE TABLE ([\w.]+)',sql)
 check('all-tables-rls-default-deny',lambda:must(all('ALTER TABLE '+t+' ENABLE ROW LEVEL SECURITY' in sql and 'ALTER TABLE '+t+' FORCE ROW LEVEL SECURITY' in sql for t in tables)))
 required=['PRODUCT_VISION','ARCHITECTURE','GAME_ENGINE_SPEC','GAME_CATALOG','OFFLINE_FIRST','CONTENT_PACK_FORMAT','SYNC_PROTOCOL','DATABASE_SCHEMA','API_CONTRACT','CONTENT_SOURCES_POLICY','EDITORIAL_WORKFLOW','UX_UI_DESIGN_SYSTEM','SECURITY_MODEL','TEST_STRATEGY','IMPLEMENTATION_ROADMAP']
 check('required-deliverables',lambda:must(all((R/'docs'/f'{n}.md').exists() for n in required) and len(list((R/'docs/ADR').glob('*.md')))>=5))
 sourceids={x['id'] for x in load('examples/source-register.json')}
 check('fixture-provenance-links',lambda:must(all(set(json.loads(f.read_text())['sourceRefs'])<=sourceids for f in (R/'examples/puzzles').glob('*.json'))))
 result={'date':__import__('datetime').datetime.now(__import__('datetime').timezone.utc).date().isoformat(),'scope':'Local blueprint validation only','passed':sum(c['passed'] for c in checks),'failed':sum(not c['passed'] for c in checks),'checks':checks,'sqlTables':len(tables),'sqlStatements':len(parse_sql(sql)),'apiOperations':sum(len(v) for v in api['paths'].values()),'toolVersions':{x:version(x) for x in ['jsonschema','openapi-spec-validator','pglast']},'notRun':['Android build/UI','Kotlin compile','PostgreSQL migrations/RLS runtime','HTTP backend','Ed25519 implementation','download crash integration','performance benchmark']}
 (R/'reports/validation.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k!='checks'},ensure_ascii=False,indent=2))
 for c in checks:
  if not c['passed']:print(c)
 return 1 if result['failed'] else 0
if __name__=='__main__':sys.exit(main())
