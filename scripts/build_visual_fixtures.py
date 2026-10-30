#!/usr/bin/env python3
"""Reproducible original geometric illustrations and draft visual puzzles."""
from pathlib import Path
from PIL import Image, ImageDraw
from hashlib import sha256
from uuid import uuid5, NAMESPACE_URL
import importlib.util
import json
import sys
import tempfile

ROOT=Path(__file__).resolve().parents[1]
spec=importlib.util.spec_from_file_location('media_pipeline',ROOT/'packages/media/pipeline.py')
m=importlib.util.module_from_spec(spec); sys.modules[spec.name]=m; spec.loader.exec_module(m)
UID=lambda name: str(uuid5(NAMESPACE_URL,'lughzdan:stage05:original:'+name))
SOURCE=UID('source-reference')
LICENSE='PROJECT-ORIGINAL-FIXTURE'
RIGHTS=m.Rights(SOURCE,LICENSE,'LUGHZDAN original geometric development fixtures',True,True,True)
OUT=ROOT/'debugassets/media'
records=[]

def shape(draw, kind, box, color):
    x,y,r,b=box; cx=(x+r)/2; cy=(y+b)/2
    if kind=='circle': draw.ellipse(box,fill=color)
    elif kind=='square': draw.rounded_rectangle(box,radius=5,fill=color)
    elif kind=='triangle': draw.polygon([(cx,y),(r,b),(x,b)],fill=color)
    elif kind=='star':
        import math
        points=[(cx+(r-x)/2*(1 if i%2==0 else .44)*math.sin(i*math.pi/5),cy-(b-y)/2*(1 if i%2==0 else .44)*math.cos(i*math.pi/5)) for i in range(10)]
        draw.polygon(points,fill=color)
    elif kind=='house':
        draw.rectangle((x+15,cy,r-15,b),fill=color); draw.polygon([(x,cy),(cx,y),(r,cy)],fill=color)
        draw.rectangle((cx-12,b-45,cx+12,b),fill='#f5f1e7')
    elif kind=='tree':
        draw.rectangle((cx-10,cy,cx+10,b),fill=color)
        draw.polygon([(cx,y),(r,cy+30),(x,cy+30)],fill=color)
    elif kind=='fish':
        draw.ellipse((x, y+15,r-30,b-15),fill=color)
        draw.polygon([(r-45,cy),(r,y),(r,b)],fill=color)
    elif kind=='key':
        draw.ellipse((x,y,x+85,y+85),fill=color)
        draw.rectangle((x+35,y+65,x+52,b),fill=color)
        draw.rectangle((x+45,b-20,r,b),fill=color)
        draw.ellipse((x+20,y+20,x+65,y+65),fill='#f5f1e7')

def asset(name,kind='circle',count=1,color='#286fa8',role='clue',alt=None):
    image=Image.new('RGB',(360,280),'#f5f1e7'); d=ImageDraw.Draw(image)
    if count==1: shape(d,kind,(90,45,270,235),color)
    else:
        for i in range(count):
            row,col=divmod(i,4)
            shape(d,kind,(25+col*82,45+row*100,85+col*82,105+row*100),color)
    with tempfile.TemporaryDirectory() as temp:
        path=Path(temp)/'original.png'; image.save(path)
        result=m.ingest(path,OUT,RIGHTS)
        # Preserve reproducible source pixels and their real hash as provenance evidence.
        original=OUT/(result['contentHash']+'.png'); original.write_bytes(path.read_bytes())
    rel='media/'+Path(result['image']['path']).name
    records.append({'assetId':UID(name),'sourceReferenceId':SOURCE,'sourcePath':'media/'+original.name,'sourceSha256':result['sourceSha256'],'rights':result['rights'],'image':{**result['image'],'path':rel},'thumbnail':{**result['thumbnail'],'path':'media/'+Path(result['thumbnail']['path']).name}})
    return dict(id=UID(name),assetPath=rel,alt=alt or f'{count} {kind}',role=role,sourceRef=SOURCE,license=LICENSE,width=360,height=280,sha256=result['image']['sha256'])

levels=[]
def level(kind,n,title,prompt,explanation,payload):
    levels.append(dict(schemaVersion=1,puzzleId=UID(kind+str(n)),revision=1,gameId=UID('game-'+kind),type=kind,world='imagination' if kind!='visual_logic' else 'logic',difficulty=1 if n==1 else 2,rulesVersion=1,normalization='arabic_basic_v1',title=title,status='draft',fixtureOnly=True,sourceRefs=[SOURCE],explanation=explanation,hints=[dict(id='h1',kind='step',text=explanation,penalty=10)],payload=dict(prompt=prompt,**payload)))

for n,kind,answer in [(1,'circle','دائرة'),(2,'triangle','مثلث')]:
    imgs=[asset(f'four-{n}-{i}',kind,color=c,alt=f'شكل {answer} بلون مختلف',role='clue') for i,c in enumerate(['#286fa8','#ce6047','#4e8a59','#9164a8'])]
    level('four_pictures',n,'الشكل المشترك','ما اسم الشكل المشترك بين الصور الأربع؟',f'تختلف الألوان لكن الشكل في الصور الأربع هو {answer}.',dict(images=imgs,acceptedAnswers=[answer],tiles=list(answer),fixedSlots=[]))
for n,kind,answer in [(1,'house','بيت'),(2,'tree','شجرة')]:
    img=asset(f'guess-{n}',kind,color='#4e8a59' if n==2 else '#ce6047',alt=f'رسم هندسي يمثل {answer}')
    level('guess_picture',n,'اكشف الرسم','اكشف أجزاء الصورة ثم اكتب اسم الشيء.',f'الرسم الهندسي يمثل {answer}.',dict(images=[img],acceptedAnswers=[answer],revealRows=3,revealCols=3,revealPenalty=5))
for n,kind,others in [(1,'key',['fish','tree']),(2,'fish',['key','house'])]:
    clue=asset(f'shadow-{n}-clue',kind,color='#286fa8',alt='الشكل الملون المطلوب مطابقة ظله')
    choices=[asset(f'shadow-{n}-{i}',k,color='#17293a',role='option',alt=f'خيار ظل {i+1}') for i,k in enumerate([others[0],kind,others[1]])]
    level('shadow',n,'طابق الظل','اختر الظل الذي يطابق حدود الشكل الملون.', 'قارن الحد الخارجي ومواضع الأجزاء البارزة.',dict(images=[clue]+choices,correctImageId=choices[1]['id']))
for n,kind,counts,answer in [(1,'star',[2,3],'5'),(2,'triangle',[3,4],'7')]:
    imgs=[asset(f'symbols-{n}-{i}',kind,count=c,role='symbol',alt=f'{c} رموز منفصلة') for i,c in enumerate(counts)]
    level('symbols',n,'عد الرموز','كم رمزاً في الصورتين معاً؟ اكتب العدد بالأرقام.',f'نجمع العددين {counts[0]} و{counts[1]} فنحصل على {answer}.',dict(images=imgs,acceptedAnswers=[answer,'٥' if n==1 else '٧']))
for n,kinds in [(1,['circle','triangle','square']),(2,['house','tree','fish'])]:
    left=[asset(f'pairs-{n}-left-{i}',k,color='#286fa8',role='left',alt=f'الشكل الملون {i+1}') for i,k in enumerate(kinds)]
    right=[asset(f'pairs-{n}-right-{i}',k,color='#17293a',role='right',alt=f'ظل الشكل {i+1}') for i,k in enumerate(reversed(kinds))]
    level('visual_association',n,'صل الشكل بظله','صل كل شكل ملون بظله المطابق.', 'طابق حدود كل شكل؛ ترتيب الظلال مختلف.',dict(images=left+right,visualPairs=[dict(leftId=a['id'],rightId=right[2-i]['id']) for i,a in enumerate(left)]))
for n,op,values,result,options in [(1,'add',[1,2,3],6,[5,6,7]),(2,'multiply',[1,2,3],6,[5,7,6])]:
    terms=[asset(f'logic-{n}-term-{i}','circle',count=v,color='#286fa8',role='term',alt=f'مجموعة من {v} دوائر') for i,v in enumerate(values)]
    choices=[asset(f'logic-{n}-option-{i}','circle',count=v,color='#ce6047',role='option',alt=f'خيار فيه {v} دوائر') for i,v in enumerate(options)]
    prompt='اجمع أعداد الدوائر في المجموعات الثلاث واختر الناتج.' if op=='add' else 'اضرب أعداد الدوائر في المجموعات الثلاث واختر الناتج.'
    level('visual_logic',n,'حساب بالصور',prompt,'١ + ٢ + ٣ = ٦' if op=='add' else '١ × ٢ × ٣ = ٦',dict(images=terms+choices,correctImageId=choices[options.index(result)]['id'],logicRule=dict(kind=op,values=values,optionValues={c['id']:v for c,v in zip(choices,options)},step=1)))

(ROOT/'debugdata').mkdir(exist_ok=True)
(ROOT/'debugdata/visual.jsonl').write_text(''.join(json.dumps(x,ensure_ascii=False)+'\n' for x in levels))
(ROOT/'debugassets/visual-provenance.json').write_text(json.dumps(dict(sourceReferenceId=SOURCE,license=LICENSE,fixtureOnly=True,records=records),ensure_ascii=False,indent=2)+'\n')
(OUT/'original-rights.txt').write_text('Original geometric illustrations generated by scripts/build_visual_fixtures.py for LUGHZDAN. PROJECT-ORIGINAL-FIXTURE. Derivative works, redistribution in project fixtures, and offline project use are explicitly permitted. Draft engineering fixtures; editorial approval is pending. No external imagery or scientific approval claimed.\n')
print(f'Wrote {len(levels)} draft levels, {len(records)} asset references; source {SOURCE}')
