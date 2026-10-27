from pathlib import Path
import json,xml.etree.ElementTree as ET
files=list(Path('apps/android').glob('**/build/test-results/**/TEST-*.xml'))
summary=[]
for p in files:
 root=ET.parse(p).getroot()
 summary.append({'suite':root.attrib.get('name'),'tests':int(root.attrib.get('tests',0)),'failures':int(root.attrib.get('failures',0)),'errors':int(root.attrib.get('errors',0)),'skipped':int(root.attrib.get('skipped',0))})
print(json.dumps({'suites':summary,'totals':{key:sum(s[key] for s in summary) for key in ['tests','failures','errors','skipped']}},indent=2))
if not files:raise SystemExit('No JUnit results found')
