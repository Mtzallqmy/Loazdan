"""Reproducible editorial triage, never an assertion of scholarly approval.
Published files are read-only. Candidate corrections remain unsigned drafts.
"""
import copy, hashlib, json, pathlib
import jsonschema
ROOT=pathlib.Path(__file__).resolve().parents[1]
source=ROOT/'packs/opening-pack-120/data/puzzles.jsonl'
raw=source.read_bytes(); puzzles=[json.loads(line) for line in raw.splitlines()]
out=ROOT/'docs/next-experience';out.mkdir(exist_ok=True)
notes={
1:'إجابة العمود «لع» مصطنعة؛ استبدالها بكلمة ذات معنى ومراجعة الشرح.',
2:'العنوان والشرح يذكران الحق مع أن الشبكة تستخدم «قف».',
3:'الشرح لا يطابق إجابتي الشبكة «حكم» و«كتب».',
51:'الحماسة مختارات جمعها أبو تمام؛ عبارة «الشاعر الذي أبدعه» تحتاج تحريرًا.',
52:'تصحيح كتابة اسم امرئ القيس؛ توثيق الألقاب من مرجع محدد.',
57:'البديل «الوليد» وحده لا يحدد الشاعر؛ يلزم حذفه.',
58:'تعريف القافية يحتاج ضبطًا عروضيًا؛ لا يساوي الحرف الأخير فقط.',
61:'الشرح يجمع وجود الأداة مع وصف التشبيه بالمؤكد؛ يحتاج تصحيحًا موثقًا.',
63:'صيغة السؤال تخلط الصورة البيانية بالمحسن البديعي؛ الشرح لا يحلل المثال نفسه.',
66:'مثال «يقيننا بالله يقيننا» يحتاج تدقيق صحة الجناس والحركات.',
69:'«شربت ماء زمزم» لا يوضح علاقة الكلية المقصودة؛ استبدال المثال بعد مراجعة.',
75:'لا ترتيب وحيد مطلق لأركان التشبيه؛ اربطه بجملة محددة.',
76:'نسبة النص وصيغته تحتاج مصدرًا وصفحة، لا إحالة عامة.',
78:'الشرح يذكر المجاز بينما الشبكة تستخدم «جد».',
79:'الشرح يذكر الأمر بينما الشبكة تستخدم «بر».',
80:'لفظ «الجراحة» في دليل اليد يحتاج تصحيحًا إلى الجارحة.',
87:'تعميم السنة للشدة والعام للرخاء يحتاج تقييدًا بسياق موثق.',
90:'لفظ «الراجح» يحتاج تحديد المفسر والإحالة مع بيان اختلاف التفسير.',
91:'المطلوب جذر ثلاثي لكن البديل «الرحمة» ليس جذرًا.',
92:'المعنى اللغوي والمعنى التفسيري ممزوجان في البدائل؛ يلزم تحرير مرجعي.',
94:'العنوان يسأل عن المفرد والمتن يسأل عن المعنى.',
98:'«قران» و«تورة» رسمان غير مناسبين؛ تصحيح الكلمات وإعادة بناء الشبكة.',
99:'«عشا» ناقصة الهمزة؛ يلزم توسيع الشبكة وكتابة «عشاء».',
109:'مشتت «أمطرت قتيلاً» خطأ طباعي؛ إصلاحه مع تقييد الاستنتاج بالمقدمات.',
113:'تقييد السؤال بمثلث في الهندسة المستوية.',
116:'السبب والنتيجة هنا علاقات شائعة لا لزوم منطقي؛ تقييد الصياغة.',
119:'دورة الماء ليست خطًا وحيدًا؛ تحديد أن الترتيب لمسار مبسط يبدأ بالتبخر.'}
rows=[]; candidates=[]
for number,original in enumerate(puzzles,1):
 p=copy.deepcopy(original);b=p['payload'];flags=[]
 if number in notes:flags.append(notes[number])
 if p['type'] in ('anagram','four_pictures','guess_picture','symbols') and any(a in p['title'] for a in b.get('acceptedAnswers',[])):
  flags.append('العنوان يكشف إجابة مقبولة.')
 if p['type']=='four_pictures':flags.append('القرائن تعيد الشكل نفسه؛ يلزم أربع قرائن متنوعة وفكرة مشتركة.')
 if p['type']=='sequence':flags.append('معايرة الصعوبة بحسب العمليات؛ الوسم الحالي لا يطابق التعقيد بانتظام.')
 if p['world'] in ('quran','poetry','rhetoric'):flags.append('المراجعة المرجعية والنسبة/النص غير معتمدتين في هذا التدقيق.')
 changes=[]
 if p['type']=='anagram':
  prompts={7:'رتب الحروف لتسمية شيء نقرأ صفحاته.',8:'رتب الحروف لتسمية ما نراه في السماء وقد يحمل المطر.',9:'رتب الحروف لتسمية شعور الفرح بخبر سار.',58:'رتب الحروف لتكوين مصطلح يخص نهايات أبيات الشعر.',59:'رتب الحروف لتكوين اسم مفرد لقصيدة من القصائد الجاهلية المشهورة.',60:'رتب الحروف لتسمية نص شعري مؤلف من أبيات.'}
  p['title']='بناء كلمة من قرينة';b['prompt']=prompts[number];changes.append('عنوان محايد وقرينة أصلية بدل الإجابة المكشوفة.')
 if number==1:
  b['entries'][1]['tokens']=list('ليل');b['entries'][1]['clue']='الوقت المقابل للنهار';p['title']='تقاطعات: أداة ووقت';p['explanation']='القلم أداة الكتابة، والليل يقابل النهار. تتقاطع الكلمتان في اللام.';changes.append('استبدال إجابة مصطنعة بكلمة ليل دون تغيير إحداثيات التقاطع.')
 if number==57:b['acceptedAnswers']=['مسلم بن الوليد'];changes.append('إزالة البديل الملتبس.')
 if number==91:b['acceptedAnswers']=['رحم','ر ح م'];changes.append('قبول الجذر وحده.')
 if number==94:p['title']='معنى مفردة قرآنية';changes.append('مطابقة العنوان للمطلوب.')
 if number in (98,99):
  words=['قرآن','توراة'] if number==98 else ['فجر','عشاء']
  b['grid']=[list('قرآنم'),list('توراة'),list('صحفكل')] if number==98 else [list('فجرم'),list('عشاء'),list('ليلك')]
  b['words']=[dict(id=f'w{i+1}',label=w,path=[dict(row=i,col=j) for j in range(len(w))]) for i,w in enumerate(words)]
  changes.append('تصحيح رسم الكلمات وإعادة بناء مسارات الشبكة.')
 if number==109:b['options'][1]['text']='هطل مطر قليل';changes.append('تصحيح المشتت الطباعي.')
 if number==113:b['prompt']='ما مجموع قياسات الزوايا الداخلية لمثلث في الهندسة المستوية، بالدرجات؟';changes.append('تحديد الهندسة المستوية.')
 if changes:
  p['revision']=original['revision']+1;p['status']='draft'
  jsonschema.validate(p,json.loads((ROOT/'contracts/puzzle.schema.json').read_text()))
  candidates.append(p)
 rows.append(dict(number=number,puzzleId=p['puzzleId'],title=original['title'],findings=flags,status='needs_review' if flags else 'no_issue_detected_in_triage',candidateChanges=changes))
(out/'content-triage.json').write_text(json.dumps(dict(sourceSha256=hashlib.sha256(raw).hexdigest(),reviewType='engineering_and_editorial_triage_not_scholarly_approval',items=rows),ensure_ascii=False,indent=2)+'\n')
path=ROOT/'packages/knowledge/data/next_experience_candidates.jsonl'
path.write_text(''.join(json.dumps(p,ensure_ascii=False)+'\n' for p in candidates))
assert source.read_bytes()==raw
assert len(rows)==120
print(f'{len(rows)} records triaged; {len(candidates)} schema-valid unsigned revision-2 drafts; published bytes unchanged')
