"""Validate debug fixtures against the original contract and the actual bundled bytes."""
import hashlib,json,pathlib
import jsonschema
root=pathlib.Path(__file__).resolve().parents[1]
assets=root/'apps/android/app/src/debug/assets'
manifest=json.loads((assets/'bundled-manifest.json').read_text())
jsonschema.Draft202012Validator(json.loads((root/'packages/content-schema/bundled-manifest.schema.json').read_text())).validate(manifest)
data=(assets/manifest['path']).read_bytes()
assert len(data)==manifest['bytes']
assert hashlib.sha256(data).hexdigest()==manifest['sha256']
validator=jsonschema.Draft202012Validator(json.loads((root/'contracts/puzzle.schema.json').read_text()))
count=0
for line in data.splitlines():
 p=json.loads(line);validator.validate(p)
 assert p['fixtureOnly'] and p['status']=='draft'
 assert len(p['payload']['options'])==4
 assert p['payload']['correctId'] in [o['id'] for o in p['payload']['options']]
 count+=1
assert not (root/'apps/android/app/src/release/assets/data/vocabulary.jsonl').exists()
print(f'{count} debug-only puzzles and SHA-256 manifest validated')
