#!/usr/bin/env python3
"""Produce an idempotent SQL publication from the checked-in signed release pack.
Run only after runtime and content acceptance checks. Never edits existing revisions.
"""
import json, hashlib, uuid
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def q(v):return "'"+str(v).replace("'","''")+"'"
def uid(s):return str(uuid.uuid5(uuid.NAMESPACE_URL,'lughzdan:release:'+s))
def statements():
 ps=[json.loads(s) for s in (ROOT/'packs/opening-pack-120/data/puzzles.jsonl').read_text().splitlines()]
 m=json.loads((ROOT/'services/api/release/manifest.json').read_text())
 canonical=lambda x:json.dumps(x,sort_keys=True,ensure_ascii=False,separators=(',',':'))
 # RFC8785 differs for arbitrary numeric values; this manifest only has bounded integers.
 h=hashlib.sha256(canonical(m).encode()).hexdigest()
 titles=dict(words='مدينة الكلمات',imagination='مختبر الخيال',poetry='ديوان الأدب',rhetoric='مملكة البيان',quran='كنوز الألفاظ القرآنية',logic='مجلس الأذكياء')
 out=[]
 for n,(slug,title) in enumerate(titles.items()):out.append(f"INSERT INTO content.categories(id,slug,title,sort_order) VALUES({q(uid('world:'+slug))},{q(slug)},{q(title)},{n}) ON CONFLICT(id) DO NOTHING")
 games={p['gameId']:p for p in ps}
 for gid,p in games.items():out.append(f"INSERT INTO content.games(id,game_type_id,category_id,slug,title,enabled) VALUES({q(gid)},{q(p['type'])},{q(uid('world:'+p['world']))},{q('opening-'+gid)},{q(p['type'])},true) ON CONFLICT(id) DO NOTHING")
 for p in ps:
  raw=canonical(p);ph=hashlib.sha256(raw.encode()).hexdigest()
  out.extend([f"INSERT INTO content.puzzles(id,game_id) VALUES({q(p['puzzleId'])},{q(p['gameId'])}) ON CONFLICT(id) DO NOTHING",f"INSERT INTO content.puzzle_versions(puzzle_id,revision,schema_version,rules_version,difficulty,normalization,definition,definition_sha256,status) VALUES({q(p['puzzleId'])},{p['revision']},1,1,{p['difficulty']},{q(p['normalization'])},{q(raw)}::jsonb,{q(ph)},'published') ON CONFLICT(puzzle_id,revision) DO NOTHING",f"INSERT INTO content.publications(puzzle_id,revision,published_at) VALUES({q(p['puzzleId'])},{p['revision']},'2026-11-02T00:00:00Z') ON CONFLICT DO NOTHING"])
 out.append(f"INSERT INTO content.content_packs(id,slug,category_id,title,is_core) VALUES({q(m['packId'])},'opening-pack-120',{q(uid('world:words'))},'الحزمة الافتتاحية — العوالم الستة',true) ON CONFLICT(id) DO NOTHING")
 out.append(f"INSERT INTO content.content_pack_versions(pack_id,version,schema_version,min_app_version_code,manifest,manifest_sha256,download_bytes,installed_bytes,object_key,status,published_at) VALUES({q(m['packId'])},1,1,100,{q(canonical(m))}::jsonb,{q(h)},{m['downloadBytes']},{m['installedBytes']},'opening-pack-120/v1','published','2026-11-02T00:00:00Z') ON CONFLICT(pack_id,version) DO NOTHING")
 return out
if __name__=='__main__':print(json.dumps(statements(),ensure_ascii=False))
