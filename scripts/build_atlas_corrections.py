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
# Secondary close reading of every opening prompt, explanation and pair.
additional_sources=[
 ('simile','جواهر البلاغة — أقسام التشبيه','https://www.hindawi.org/books/85925824/2.1/'),
 ('metonymy','جواهر البلاغة — المجاز المرسل وعلاقاته','https://www.hindawi.org/books/85925824/2.2/'),
 ('rhetoric','جواهر البلاغة — المعاني والبيان والبديع','https://www.hindawi.org/books/85925824/'),
 ('year','جامعة الملك سعود — تفسير الطبري، يوسف 49','https://quran.ksu.edu.sa/tafseer/tabary/sura12-aya49.html'),
 ('night','جامعة الملك سعود — تفسير الطبري، التكوير 17','https://quran.ksu.edu.sa/tafseer/tabary/sura81-aya17.html'),
 ('kawthar','جامعة الملك سعود — تفسير الطبري، الكوثر 1','https://quran.ksu.edu.sa/tafseer/tabary/sura108-aya1.html'),
 ('samad','جامعة الملك سعود — تفسير السعدي، الإخلاص 2','https://quran.ksu.edu.sa/tafseer/saadi/sura112-aya2.html'),
 ('logic','مؤسسة هنداوي — المنطق الصوري القديم بين الأصالة والتبعية','https://downloads.hindawi.org/books/35841973.pdf'),
]
for key,name,url in additional_sources:
 refs[key]=uid('source/correction-'+key)
 source=dict(key=key,referenceId=refs[key],name=name,url=url)
 new_sources.append(source);source_map[refs[key]]=source
original=[json.loads(l) for l in (ROOT/'packs/opening-pack-120/data/puzzles.jsonl').read_text().splitlines()]
indexes=[51,52,61,66,69,76,82,87,90,92,107,108,116,120]
for number in indexes:
 p=json.loads(json.dumps(original[number-1]));old=p['puzzleId'];p['puzzleId']=uid('corrected/'+old)
 p['revision']=3;p['tags']=['supersedes:'+old];ref=refs['original'];a=p['payload']
 if number==51:
  p['title']='الدواوين ومؤلفوها وجامعوها'
  p['explanation']='الشوقيات لأحمد شوقي واللزوميات لأبي العلاء المعري؛ أما ديوان الحماسة فمختارات جمعها أبو تمام من شعر غيره، وليس جميع شعره من نظمه.'
 if number==52:
  a['left'][2]['text']='امرؤ القيس'
  p['explanation']='حافظ إبراهيم شاعر النيل، والأخطل شاعر أموي، وامرؤ القيس عُرف بالملك الضليل. تكتب همزة امرؤ في أول الاسم همزة وصل.'
 if number==61:
  ref=refs['simile'];p['title']='تمييز أركان التشبيه'
  a['options'][0]['text']='تشبيه مرسل مفصل (تام الأركان)'
  p['explanation']='العلم مشبّه، والنور مشبّه به، والكاف أداة التشبيه، والهداية وجه الشبه. فهو مرسل لذكر الأداة، ومفصل لذكر وجه الشبه؛ ولا يسمى مؤكدًا لأن الأداة مذكورة.'
 if number==66:
  ref=refs['rhetoric'];a['left'][2]['text']='صلّيت المغرب في المغرب'
  p['explanation']='في المثال الأخير اتفق لفظ المغرب واختلف معناه بين الصلاة والبلد. السجع توافق فواصل النثر، والمقابلة تقابل المعاني على الترتيب.'
 if number==69:
  ref=refs['metonymy'];a['left'][1]['text']='سدّ أذنيه بأصابعه'
  p['explanation']='إطلاق الكلمة على الخطاب علاقة جزئية، وذكر الأصابع مع إرادة أطرافها علاقة كلية، وذكر الغيث مع إرادة النبات علاقة سببية.'
 if number==76:
  p['title']='ترتيب عبارة مسجوعة';p['explanation']='رتبت الأوصاف مع موصوفاتها في هذه العبارة المسجوعة. لا تُثبت هذه الجولة نسبة النص إلى خطيب بعينه.'
 if number==82:
  ref=refs['samad'];p['explanation']='الصمد هو المقصود في الحوائج؛ والوسواس يلقي الخواطر، والخناس يتأخر ويتوارى. تتطابق هذه المعاني مع عناصر المطابقة المعروضة.'
 if number==87:
  ref=refs['year'];p['title']='فهم السياق في سورة يوسف'
  a['prompt']='في قوله تعالى من سورة يوسف: {عَامٌ فِيهِ يُغَاثُ النَّاسُ}، ما الذي يدل عليه السياق؟'
  a['options']=[dict(id='o1',text='عودة الغيث والخصب بعد الشدة'),dict(id='o2',text='استمرار القحط دون تغير'),dict(id='o3',text='انعدام الزراعة نهائيًا'),dict(id='o4',text='خروج يوسف إلى البحر')]
  p['hints']=[dict(id='h1',kind='step',text='تأمل الفعل يُغاث وما يتبعه من عصر الثمار.',penalty=10)]
  p['explanation']='فسّر الطبري إغاثة الناس هنا بالمطر والغيث. السؤال عن سياق الآية، ولا يجعل السنة مرادفة للشدة أو العام مرادفًا للرخاء في كل استعمال.'
 if number==90:
  ref=refs['night'];a['prompt']='ما المعنى الذي رجحه الطبري لقوله تعالى: {وَاللَّيْلِ إِذَا عَسْعَسَ}؟'
  p['explanation']='رجح الطبري إدبار الليل استدلالًا بذكر تنفس الصبح بعده. وذكر أيضًا تفسير إقبال الليل؛ لذلك حُدّد المرجع في السؤال بدل رفض تفسير آخر معروف.'
 if number==92:
  ref=refs['kawthar'];a['acceptedAnswers']=['الخير الكثير','خير كثير','الكثير من الخير']
  p['explanation']='المطلوب هنا المعنى اللغوي العام: الخير الكثير. ورد في التفسير أيضًا نهر في الجنة، لكنه ليس الجواب اللغوي المحدد في السؤال.'
 if number==107:
  p['explanation']='خالد أطول من سعيد، وسعيد أطول من ماجد؛ وبالتعدي يكون خالد أطول من ماجد، فهو الأطول بين الثلاثة.'
 if number==108:
  ref=refs['logic'];p['explanation']='إذا قُبلت المقدمتان: كل إنسان ناطق، وسقراط إنسان، لزمت النتيجة سقراط ناطق. الحد المشترك هو الإنسان، ولا تبرهن المقدمتان وحدهما أنه فيلسوف.'
 if number==116:
  p['title']='علاقات الأسباب والنتائج الشائعة'
  p['explanation']='هذه علاقات تعليمية شائعة: الاجتهاد يساعد على النجاح، والمطر يساعد على نمو الزرع، والإهمال قد يؤدي إلى الإخفاق. ليست هذه النتائج مضمونة من سبب واحد في كل حالة.'
 if number==120:
  ref=refs['logic'];p['explanation']='المعروض ثلاث خطوات: المقدمة الكبرى، ثم المقدمة الصغرى، ثم استخلاص النتيجة. الحد الأوسط يربط المقدمتين، ولا يمثل مقدمة رابعة مستقلة.'
 p['sourceRefs']=[ref];a['sourceLabel']=source_map[ref]['name'];a['sourceUrl']=source_map[ref]['url'];rows.append(p)
rows.extend(json.loads(l) for l in (ROOT/'packs/atlas-600/visual-puzzles.jsonl').read_text().splitlines())
assert len(rows)==32

folder=ROOT/'packs/atlas-600'
(folder/'corrected-puzzles.jsonl').write_text(''.join(json.dumps(p,ensure_ascii=False,separators=(',',':'))+'\n' for p in rows))
(folder/'correction-sources.json').write_text(json.dumps(new_sources,ensure_ascii=False,indent=2)+'\n')
base=(ROOT/'packs/literary-words-48/puzzles.jsonl').read_text()+(folder/'new-puzzles.jsonl').read_text()
(folder/'puzzles.jsonl').write_text(base+(folder/'corrected-puzzles.jsonl').read_text())
print('32 corrected replacement revisions, 512 signed rows, 600 active challenges')
