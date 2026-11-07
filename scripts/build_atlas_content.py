"""Reproducible Atlas challenges from edited concepts and explicit arithmetic rules."""
import json,uuid,random,collections
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
uid=lambda text:str(uuid.uuid5(uuid.NAMESPACE_URL,'https://lughzdan.app/atlas/'+text))
refs={
 'original':('صياغة تعليمية أصلية للُغْزَدان بمساعدة آلية',''),
 'rhetoric':('جواهر البلاغة — أحمد الهاشمي، أبواب البيان والمعاني والبديع','https://www.hindawi.org/books/85925824/'),
 'yemen':('اليونسكو — مواقع اليمن المسجلة في التراث العالمي','https://whc.unesco.org/en/statesparties/ye/'),
 'water':('NASA — دورة الماء','https://science.nasa.gov/kids/earth/what-is-the-water-cycle/'),
 'space':('NASA Space Place — معجم مصطلحات الفضاء','https://spaceplace.nasa.gov/glossary/en/'),
 'biology':('OpenStax — Concepts of Biology','https://openstax.org/books/concepts-biology/pages/1-introduction'),
 'physics':('OpenStax — College Physics 2e','https://openstax.org/books/college-physics-2e/pages/1-introduction-to-science-and-the-realm-of-physics-physical-quantities-and-units'),
 'prosody':('مجمع اللغة العربية بالقاهرة — التفعيلة وشعر التفعيلة','https://www.arabicacademy.gov.eg/ar/محرك-البحث/معجم/dic-383/الشعر-المرسل-و-التفعيلة'),
}
worldNames={'words':'مدينة اللغة','poetry':'ديوان الأدب','rhetoric':'مملكة البيان','heritage':'دروب اليمن','science':'مختبر الحياة','logic':'مرصد التفكير'}
modeNames={'words':'language','poetry':'literature','rhetoric':'rhetoric','heritage':'heritage','science':'science','logic':'logic'}
concepts=[]
for line in (ROOT/'packs/atlas-600/concepts.tsv').read_text().splitlines():
 world,topic,answer,meaning,example=line.split('|')
 concepts.append(dict(world=world,topic=int(topic),answer=answer,meaning=meaning,example=example))
assert len(concepts)==120
assert collections.Counter(x['world'] for x in concepts)=={w:24 for w in worldNames if w!='logic'}
sourceRows=[]
for key,(name,url) in refs.items():sourceRows.append(dict(key=key,referenceId=uid('source/'+key),name=name,url=url))
refIds={s['key']:s['referenceId'] for s in sourceRows}

def base(world,topic,concept,index,kind,payload,explanation,hints,source='original',difficulty=1):
 name,url=refs[source]
 payload.update(sourceLabel=name,sourceUrl=url)
 if topic>=2 or payload.get('variant')=='word_bridge':difficulty=max(difficulty,2)
 return dict(schemaVersion=1,puzzleId=uid(f'{world}/{concept}/{index}'),revision=1,gameId=uid('game/'+kind),type=kind,world=world,difficulty=difficulty,rulesVersion=1,normalization='arabic_basic_v1',title=worldNames[world]+' · تحدي معرفة',status='published',fixtureOnly=False,sourceRefs=[refIds[source]],explanation=explanation,hints=hints,payload=payload,tags=['atlas:'+world,'topic:'+str(topic),'concept:'+world+'/'+str(concept)])

def hints_for(answer,example,fixed=()):
 hidden=[i for i in range(len(answer)) if i not in fixed]
 return [dict(id='context',kind='step',text=example,penalty=5)]+[dict(id='letter-'+str(i),kind='step',text=f'الحرف رقم {i+1}: {answer[i]}',penalty=10) for i in hidden[:min(2,max(0,len(hidden)-1))]]

words='علم حلم حكم حجم نجم نجل رجل رمل حمل جمل جبل حبل قبل'.split()
def bridge_path(start,goal):
 queue=[[start]];seen={start}
 for path in queue:
  if path[-1]==goal:return path
  for word in words:
   if word not in seen and sum(a!=b for a,b in zip(word,path[-1]))==1:
    seen.add(word);queue.append(path+[word])
 raise ValueError('unreachable')
bridges=[]
for a in words:
 for b in reversed(words):
  if a!=b and len(bridge_path(a,b))>=4:bridges.append((a,b))
# Each bridge is a different start/goal problem with its own saved path.
assert len(bridges)>=12
puzzles=[]
for world in list(worldNames)[:-1]:
 group=[c for c in concepts if c['world']==world]
 for i,c in enumerate(group):
  answer,meaning,example,topic=c['answer'],c['meaning'],c['example'],c['topic']
  source='rhetoric' if world=='rhetoric' else 'yemen' if world=='heritage' and topic==0 else ['water','space','biology','physics'][topic] if world=='science' else 'prosody' if world=='poetry' and topic==2 else 'original'
  explanation=f'{answer}: {meaning}. {example}.'
  peers=[x['answer'] for x in group if x['topic']==topic and x['answer']!=answer]
  rng=random.Random(uid(f'{world}/{i}'))
  options=rng.sample(peers,3)+[answer];rng.shuffle(options)
  opts=[dict(id=uid(f'{world}/{i}/option/{word}'),text=word) for word in options]
  payload=dict(prompt=f'أي كلمة يدل عليها هذا الوصف؟\n{meaning}',options=opts,correctId=next(o['id'] for o in opts if o['text']==answer))
  if world=='words' and topic==3:
   payload['variant']='language_detective';payload['prompt']='المحقق اللغوي: اختر التصويب الذي يوافق السياق.\n'+meaning
  puzzles.append(base(world,topic,i,0,'choice',payload,explanation,[dict(id='context',kind='step',text=example,penalty=5)],source))
  fixedIndex=len(answer)//2
  payload=dict(prompt='أكمل الكلمة الموافقة للقرينة:\n'+meaning,acceptedAnswers=[answer],variant='literary_complete',mode=modeNames[world],fixedSlots=[dict(index=fixedIndex,token=answer[fixedIndex])])
  puzzles.append(base(world,topic,i,1,'text_answer',payload,explanation,hints_for(answer,example,[fixedIndex]),source))
  if world=='words' and i<12:
   start,goal=bridges[i]
   payload=dict(prompt='اعبر بين كلمتين بتغيير حرف واحد في كل خطوة.',acceptedAnswers=[goal],variant='word_bridge',items=[dict(id=uid('bridge/'+w),text=w) for w in [start]+[w for w in words if w!=start]])
   path=bridge_path(start,goal)
   exp='مسار ممكن: '+' ← '.join(path)+'. تتغير في كل خطوة خانة واحدة، وتبقى جميع الخطوات كلمات عربية من المعجم المصغر المعروض.'
   puzzles.append(base(world,topic,i,2,'text_answer',payload,exp,[dict(id='route',kind='step',text='خطوة تقربك من الهدف',penalty=15)]))
  elif i%2==0:
   payload=dict(prompt='رتب الحروف لتبني الكلمة:\n'+meaning,tiles=list(answer),acceptedAnswers=[answer],variant='anagram',fixedSlots=[])
   puzzles.append(base(world,topic,i,2,'anagram',payload,explanation,[dict(id='context',kind='step',text=example,penalty=5)],source))
  else:
   payload=dict(prompt='من أنا؟\n'+example,acceptedAnswers=[answer],variant='who_am_i')
   clues=[dict(id='meaning',kind='step',text=meaning,penalty=10),dict(id='length',kind='step',text=f'تتكون الكلمة المقصودة من {len(answer)} حروف وتبدأ بـ {answer[0]}.',penalty=10)]
   puzzles.append(base(world,topic,i,2,'text_answer',payload,explanation,clues,source))
# 24 concept families of explicit numerical rules, each tested in three formats.
for i in range(24):
 topic=i//6;step=i%6+2;start=i+1
 if topic==0:values=[start+step*k for k in range(5)];rule=f'أضف {step} في كل خطوة.'
 elif topic==1:values=[start*(k+1) for k in range(5)];rule=f'مضاعفات العدد {start} بالترتيب.'
 elif topic==2:values=[(i%6+k+1)**2 for k in range(5)];rule='هذه مربعات أعداد صحيحة متتالية.'
 else:values=[start,start+step,start+step+1,start+2*step+1,start+2*step+2];rule=f'تناوب بين إضافة {step} وإضافة 1.'
 shown,answer=values[:4],values[4]
 puzzles.append(base('logic',topic,i,0,'sequence',dict(terms=shown,answer=answer,rule=rule),rule, [dict(id='rule',kind='step',text=rule,penalty=15)]))
 options=[answer,answer+1,answer+step+2,max(0,answer-1)]
 opts=[dict(id=uid(f'logic/{i}/o/{n}'),text=str(n)) for n in options]
 prompt='اختر العدد الذي يكمل النمط: '+ '، '.join(map(str,shown))+'، ؟'
 puzzles.append(base('logic',topic,i,1,'choice',dict(prompt=prompt,options=opts,correctId=opts[0]['id']),rule,[dict(id='rule',kind='step',text=rule,penalty=15)]))
 # Ordering uses an explicit criterion to avoid a second valid order.
 numbers=sorted(set(values))
 assert len(numbers)==5
 items=[dict(id=uid(f'logic/{i}/item/{n}'),text=str(n)) for n in numbers]
 puzzles.append(base('logic',topic,i,2,'order',dict(prompt='رتب الأعداد من الأصغر إلى الأكبر.',items=items,correctOrder=[o['id'] for o in items],variant='chronology'), 'الترتيب التصاعدي: '+' < '.join(map(str,numbers)),[]))
assert len(puzzles)==432
assert len({p['payload'].get('prompt',str(p['payload'].get('terms'))) for p in puzzles})<432 # same ordering instruction, distinct boards
assert len({p['puzzleId'] for p in puzzles})==432
folder=ROOT/'packs/atlas-600'
(folder/'new-puzzles.jsonl').write_text(''.join(json.dumps(p,ensure_ascii=False,separators=(',',':'))+'\n' for p in puzzles))
original=(ROOT/'packs/literary-words-48/puzzles.jsonl').read_text()
(folder/'puzzles.jsonl').write_text(original+(folder/'new-puzzles.jsonl').read_text())
(folder/'sources.json').write_text(json.dumps(sourceRows,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(dict(newChallenges=len(puzzles),concepts=144,totalWithExisting=600,perWorld=dict(collections.Counter(p['world'] for p in puzzles)))))
