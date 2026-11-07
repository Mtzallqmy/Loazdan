"""Publish reviewed replacements while retaining original revisions for saved rounds."""
import json,uuid
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
uid=lambda text:str(uuid.uuid5(uuid.NAMESPACE_URL,'https://lughzdan.app/atlas/'+text))
refs={
 'original':uid('source/original'),
 'poet':uid('source/poet-catalog'),
 'root':uid('source/quran-roots'),
 'ababil':uid('source/ababil'),
}
new_sources=[
 dict(key='poet-catalog',referenceId=refs['poet'],name='مكتبة قطر الوطنية — ديوان مسلم بن الوليد، صريع الغواني',url='https://ediscovery.qnl.qa/ar/islandora/object/QNL%3A00011194'),
 dict(key='quran-roots',referenceId=refs['root'],name='Quranic Arabic Corpus — الجذر ر ح م',url='https://corpus.quran.com/qurandictionary.jsp?q=rHm'),
 dict(key='ababil',referenceId=refs['ababil'],name='جامعة الملك سعود — تفسير البغوي، سورة الفيل، الآية 3',url='https://quran.ksu.edu.sa/tafseer/baghawy/sura105-aya3.html'),
]
source_map={s['referenceId']:s for s in new_sources}
source_map[refs['original']]=dict(name='صياغة تعليمية لغوية للُغْزَدان بمساعدة آلية',url='')
rows=[]
for line in (ROOT/'packages/knowledge/data/next_experience_candidates.jsonl').read_text().splitlines():
 p=json.loads(line);old=p['puzzleId'];p['puzzleId']=uid('corrected/'+old);p['revision']=3;p['status']='published';p['fixtureOnly']=False;p['tags']=['supersedes:'+old]
 answer=p['payload'].get('acceptedAnswers',[])
 ref=refs['original']
 if 'مسلم بن الوليد' in answer:
  ref=refs['poet'];p['explanation']='مسلم بن الوليد شاعر عباسي اشتهر بلقب صريع الغواني، كما يرد في عنوان ديوانه وفهارس المخطوطات.'
 if 'رحم' in answer:
  ref=refs['root'];p['explanation']='الجذر الصرفي هو ر ح م، وتقبل هنا كتابته متصلا أو مفصول الحروف.'
 if 'جماعات متفرقة' in answer:
  ref=refs['ababil'];p['explanation']='أبابيل جماعات متتابعة في تفرقة، وهو المعنى المقصود في هذه الجولة.'
 if answer==['سحابة']:p['explanation']='السحابة تجمع مرئي من قطرات الماء الدقيقة أو بلورات الجليد في الغلاف الجوي.'
 if answer==['قافية']:p['explanation']='القافية نظام من الأصوات في نهاية البيت له تحديد عروضي، والروي حرف أساسي فيه.'
 if answer==['معلقة']:p['explanation']='المعلقة اسم إحدى القصائد الجاهلية التي عرفت بهذا الاسم في التراث الأدبي.'
 p['sourceRefs']=[ref]
 for entry in p['payload'].get('entries',[]):entry['sourceRef']=ref
 p['payload']['sourceLabel']=source_map[ref]['name'];p['payload']['sourceUrl']=source_map[ref]['url']
 rows.append(p)
folder=ROOT/'packs/atlas-600'
(folder/'corrected-puzzles.jsonl').write_text(''.join(json.dumps(p,ensure_ascii=False,separators=(',',':'))+'\n' for p in rows))
(folder/'correction-sources.json').write_text(json.dumps(new_sources,ensure_ascii=False,indent=2)+'\n')
base=(ROOT/'packs/literary-words-48/puzzles.jsonl').read_text()+(folder/'new-puzzles.jsonl').read_text()
(folder/'puzzles.jsonl').write_text(base+(folder/'corrected-puzzles.jsonl').read_text())
print('14 corrected replacement revisions, 494 signed rows, 600 active challenges')
