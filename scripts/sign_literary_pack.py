"""Sign the separate literary pack; never changes the opening pack or its key."""
import base64,hashlib,json,os,uuid
from pathlib import Path
from cryptography.hazmat.primitives.asymmetric.ed25519 import Ed25519PrivateKey
from cryptography.hazmat.primitives import serialization
ROOT=Path(__file__).resolve().parents[1]
key_path=Path(os.environ['LITERARY_SIGNING_KEY_FILE'])
if ROOT in key_path.resolve().parents:raise ValueError('Private key must remain outside repository')
if not key_path.exists():
 if 'literary2026' in json.loads((ROOT/'apps/android/app/src/main/assets/pack-public-keys.json').read_text()):raise ValueError('Existing literary signer required')
 key_path.parent.mkdir(parents=True,exist_ok=True)
 key=Ed25519PrivateKey.generate()
 with os.fdopen(os.open(key_path,os.O_CREAT|os.O_EXCL|os.O_WRONLY,0o600),'wb') as out:out.write(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()))
 key_path.chmod(0o600)
else:key=serialization.load_pem_private_key(key_path.read_bytes(),None)
assets=ROOT/'apps/android/app/src/release/assets/literary'
assets.mkdir(parents=True,exist_ok=True)
rights='لُغْزَدان — ديوان الألغاز. حقوق إعداد التطبيق: معتز العلقمي. القرائن والشروح الأصلية أعدت بمساعدة آلية ولم تدّع مراجعة بشرية مستقلة. الاقتباسات المحددة للمتنبي وأحمد شوقي من شعرهما القديم، مع إحالات إلى الموسوعة الشعرية. لا تتضمن الحزمة قصائد معاصرة محمية منسوخة. النصوص التصويرية الأصلية معلّمة صراحة وليست منسوبة إلى شاعر.\n'
entries={'data/literary_words.jsonl':(ROOT/'packs/atlas-600/puzzles.jsonl').read_bytes(),'rights/literary.txt':rights.encode()}
files=[]
for path,data in entries.items():
 dest=assets/path;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(data)
 digest=hashlib.sha256(data).hexdigest()
 files.append(dict(path=path,kind='puzzles' if path.startswith('data/') else 'rights',sha256=digest,bytes=len(data),chunks=[dict(sha256=digest,bytes=len(data))]))
uid=lambda s:str(uuid.uuid5(uuid.NAMESPACE_URL,'https://lughzdan.app/literary/'+s))
sources=json.loads((ROOT/'packs/literary-words-48/sources.json').read_text())+json.loads((ROOT/'packs/atlas-600/sources.json').read_text())+json.loads((ROOT/'packs/atlas-600/correction-sources.json').read_text())
refs=[dict(referenceId=s['referenceId'],sourceId=s['referenceId'],sourceName=s['name'],author={'mutanabbi':'أبو الطيب المتنبي','shawqi':'أحمد شوقي','academy':'مجمع اللغة العربية بالقاهرة','original':'إعداد للتطبيق بمساعدة آلية'}.get(s['key'],s['name']),locator=s['url'] or 'packs/literary-words-48/puzzles.jsonl',sourceVersion='2026-11-06',licenseCode='LicenseRef-Lughzdan-Literary',attribution=s['name'],noticePath='rights/literary.txt') for s in sources]
m=dict(schemaVersion=1,packId=uid('pack'),version=5,minAppVersionCode=104,minEngineVersion=3,locale='ar',publishedAt='2026-11-07T14:00:00Z',files=files,downloadBytes=sum(f['bytes'] for f in files),installedBytes=sum(f['bytes'] for f in files),dependencies=[],sourceRefs=[s['referenceId'] for s in sources],keyId='literary2026',assets=[],references=refs)
canonical=json.dumps(m,sort_keys=True,separators=(',',':'),ensure_ascii=False).encode()
m['signature']=base64.urlsafe_b64encode(key.sign(canonical)).decode().rstrip('=')
for path in [assets/'manifest.json',ROOT/'packs/atlas-600/manifest.json']:path.write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n')
p=ROOT/'apps/android/app/src/main/assets/pack-public-keys.json';keys=json.loads(p.read_text());keys['literary2026']=base64.b64encode(key.public_key().public_bytes(serialization.Encoding.Raw,serialization.PublicFormat.Raw)).decode();p.write_text(json.dumps(keys,indent=2)+'\n')
print(json.dumps(dict(packId=m['packId'],puzzles=494,sha256=hashlib.sha256(entries['data/literary_words.jsonl']).hexdigest())))
