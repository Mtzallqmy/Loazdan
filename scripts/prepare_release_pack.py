#!/usr/bin/env python3
"""Build reproducible original media and make the opening pack runtime-ready.

This is technical validation, not a claim of independent scholarly review.
"""
from pathlib import Path
from hashlib import sha256
from uuid import uuid5, NAMESPACE_URL
import json, math, re
from PIL import Image, ImageDraw, ImageFont
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'apps/android/app/src/main/assets'
UID=lambda s: str(uuid5(NAMESPACE_URL,'lughzdan:release:'+s))
SOURCE=UID('original-visuals')
LICENSE='LicenseRef-Lughzdan-Original'
PACK=UID('opening-pack-120')
BG='#faf5e9'

def draw_shape(d,kind,color,offset=0):
    box=(80+offset,50,280-offset,230)
    if kind=='circle': d.ellipse(box,fill=color)
    elif kind=='square':d.rectangle(box,fill=color)
    elif kind=='triangle':d.polygon([(180,45),(290,235),(70,235)],fill=color)
    elif kind=='crescent' or kind=='moon':
        d.ellipse(box,fill=color);d.ellipse((135+offset,30,300-offset,210),fill=BG)
    elif kind=='sun':
        d.ellipse((120,80,240,200),fill=color)
        for i in range(12):
            a=i*math.pi/6;d.line((180+75*math.cos(a),140+75*math.sin(a),180+110*math.cos(a),140+110*math.sin(a)),fill=color,width=8)
    elif kind=='tree':
        d.rectangle((165,135,195,240),fill='#875a38');d.ellipse((80,30,280,190),fill=color)
    elif kind=='book':
        d.polygon([(45,60),(175,80),(175,240),(45,210)],fill=color);d.polygon([(185,80),(315,60),(315,210),(185,240)],fill=color)
        for y in (100,130,160):d.line((70,y,150,y+15),fill=BG,width=4);d.line((210,y+15,290,y),fill=BG,width=4)
    elif kind=='star':
        d.polygon([(180+(105 if i%2==0 else 45)*math.sin(i*math.pi/5),140-(105 if i%2==0 else 45)*math.cos(i*math.pi/5)) for i in range(10)],fill=color)
    elif kind=='sword':
        d.polygon([(180,30),(200,70),(190,195),(170,195),(160,70)],fill=color);d.rectangle((130,190,230,205),fill=color);d.rectangle((170,205,190,250),fill=color)
    elif kind=='shield':
        d.polygon([(80,55),(180,30),(280,55),(265,170),(180,250),(95,170)],fill=color)
    else:raise ValueError(kind)

def media(kind,color,offset=0,number=None):
    image=Image.new('RGB',(360,280),BG);d=ImageDraw.Draw(image)
    if number is not None:
        font=ImageFont.truetype('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf',78)
        d.text((180,140),str(number),font=font,anchor='mm',fill=color)
    else:draw_shape(d,kind,color,offset)
    import io
    b=io.BytesIO();image.save(b,format='WEBP',lossless=True);raw=b.getvalue();h=sha256(raw).hexdigest();path='media/'+h+'.webp'
    f=ASSETS/path;f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(raw)
    return path,h

def main():
    path=ROOT/'packs/opening-pack-120/data/puzzles.jsonl'
    ps=[json.loads(s) for s in path.read_text().splitlines()]
    refs=ASSETS/'release-references.json'
    mapping={v:k for k,v in json.loads(refs.read_text()).items()} if refs.exists() else {}
    def reference(x):
        return x if re.fullmatch(r"[a-f0-9-]{36}",x) else mapping.setdefault(x,UID("reference:"+x))
    for p in ps:
        p['status']='published'
        p['gameId']=UID('game:'+p['world']+':'+p['type'])
        p['sourceRefs']=[reference(x) for x in p['sourceRefs']]
        for e in p['payload'].get('entries',[]):e['sourceRef']=reference(e['sourceRef'])
        b=p['payload']
        if p['puzzleId'] in ('2609f5aa-de9e-5fee-9e64-868999789b47','65bc824c-80fc-5673-9c8e-ab01afbc90a7'):
            b['words'][1]['path']=[dict(row=r,col=1) for r in range(3)]
            if p['world']=='quran':
                b['grid'][2][1]='ل';p['explanation']='مفردتان قرآنيتان تدلان على الفضائل: بر، عدل.'
            else:p['explanation']='كلمتان تدلان على الفضائل: عدل، صدق.'
        ims=b.get('images',[])
        if not ims:continue
        p['sourceRefs']=[SOURCE]
        t=p['type']
        for i,im in enumerate(ims):
            color=['#087d80','#c26a23','#6c4fa0','#247139'][i%4];num=None;offset=(i%3)*6
            if t=='four_pictures':kind={'دائرة':'circle','مثلث':'triangle','مربع':'square','هلال':'crescent'}[b['acceptedAnswers'][0]]
            elif t=='guess_picture':kind={'شمس':'sun','قمر':'moon','شجرة':'tree','كتاب':'book'}[b['acceptedAnswers'][0]]
            elif t=='shadow':
                kind='triangle' if p['difficulty']%2 else 'star';offset=0
                if im['role']=='option':
                    color='#202830'
                    if im['id']!=b['correctImageId']:kind='square'
                p['explanation']='الظل الصحيح يحافظ على الحواف والزوايا نفسها للشكل الأصلي.'
            elif t=='symbols':
                kind={'نجم':'star','سيف':'sword','درع':'shield'}[b['acceptedAnswers'][0]]
                b['prompt']='ما اسم الرمز المشترك الظاهر في الصورتين؟'
                p['explanation']='الصورتان تعرضان الرمز نفسه بلونين مختلفين.'
            elif t=='visual_association':
                kinds={pair['leftId']:('circle' if j==0 else 'triangle') for j,pair in enumerate(b['visualPairs'])}
                kinds.update({pair['rightId']:kinds[pair['leftId']] for pair in b['visualPairs']})
                kind=kinds[im['id']];offset=0
                b['prompt']='صل كل شكل بالشكل المطابق له، مع تجاهل اختلاف اللون.'
                p['explanation']='التطابق هنا بحسب الشكل الهندسي؛ اللون لا يغير هوية الشكل.'
            elif t=='visual_logic':
                kind='circle';r=b['logicRule'];num=r['values'][i] if im['role']=='term' else r['optionValues'][im['id']]
            else:raise ValueError(t)
            asset,h=media(kind,color,offset,num)
            im.update(assetPath=asset,sha256=h,sourceRef=SOURCE,license=LICENSE,width=360,height=280,alt=('عدد '+str(num) if num is not None else {'circle':'دائرة','square':'مربع','triangle':'مثلث','crescent':'هلال','sun':'شمس','moon':'قمر','tree':'شجرة','book':'كتاب','star':'نجم','sword':'سيف','shield':'درع'}[kind]))
    raw=('\n'.join(json.dumps(p,ensure_ascii=False) for p in ps)+'\n').encode()
    for f in [path,ASSETS/'data/opening-pack-120.jsonl',ROOT/'packages/knowledge/data/opening_pack_120.jsonl']:f.write_bytes(raw)
    h=sha256(raw).hexdigest()
    bundled=dict(schemaVersion=1,packId=PACK,version=1,fixtureOnly=False,path='data/opening-pack-120.jsonl',bytes=len(raw),sha256=h)
    (ASSETS/'bundled-manifest.json').write_text(json.dumps(bundled))
    for f in [ROOT/'packs/opening-pack-120/manifest.json',ASSETS/'opening-pack-manifest.json']:
        m=json.loads(f.read_text());m.update(sha256=h,bytes=len(raw),packId=PACK);m['files'][0].update(sha256=h,bytes=len(raw),chunks=[dict(sha256=h,bytes=len(raw))]);m.update(downloadBytes=len(raw),installedBytes=len(raw));f.write_text(json.dumps(m,ensure_ascii=False,indent=2)+'\n')
    (ASSETS/'release-references.json').write_text(json.dumps({v:k for k,v in mapping.items()},ensure_ascii=False,indent=2)+'\n')
    rights=ASSETS/'rights/original.txt';rights.parent.mkdir(exist_ok=True)
    rights.write_text('LUGHZDAN original release illustrations. Copyright project owner. All rights reserved. LicenseRef-Lughzdan-Original. Generated reproducibly by scripts/prepare_release_pack.py.\n')
    print(json.dumps(dict(packId=PACK,puzzles=len(ps),bytes=len(raw),sha256=h)))
if __name__=='__main__':main()
