"""Author 16 original vector clues and four semantic four-picture replacements.
Requires Inkscape for SVG rasterization and Pillow for deterministic WebP output.
"""
import hashlib,io,json,subprocess,tempfile,uuid
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[1];folder=ROOT/'packs/atlas-600'
uid=lambda text:str(uuid.uuid5(uuid.NAMESPACE_URL,'https://lughzdan.app/atlas/'+text))
source=uid('source/atlas-original-visuals')
ink='#14233b';teal='#176f65';gold='#d99537';blue='#5bafc7';white='#fffdf6'
def rect(x,y,w,h,fill=teal,r=7):return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{r}" fill="{fill}"/>'
def circle(x,y,r,fill=gold):return f'<circle cx="{x}" cy="{y}" r="{r}" fill="{fill}"/>'
def path(d,fill=teal):return f'<path d="{d}" fill="{fill}"/>'
def line(x,y,a,b,color=ink,width=4):return f'<path d="M{x} {y}L{a} {b}" fill="none" stroke="{color}" stroke-width="{width}" stroke-linecap="round"/>'
def cloud(x=60,y=70):return circle(x,y,20,white)+circle(x+22,y-12,25,white)+circle(x+46,y,20,white)+rect(x,y,46,20,white,0)
scenes={}
sun=circle(96,78,31,'#efb447')
for x,y,a,b in [(96,29,96,17),(96,128,96,140),(46,78,34,78),(145,78,157,78),(60,43,52,35),(132,113,140,121),(60,113,52,121),(132,43,140,35)]:sun+=line(x,y,a,b,gold,5)
scenes['sun']=('شمس تشرق فوق الأفق',sun+path('M0 152Q45 127 90 150T192 148V192H0Z','#95c5ae'))
scenes['lamp']=('مصباح مكتبي يضيء كتابًا',path('M58 56L124 56L142 96H40Z',gold)+line(92,99,92,145,teal,9)+rect(61,146,64,9,teal)+path('M52 103L133 103L156 145H27Z','#fae8ab')+path('M21 157Q56 143 91 158V181Q56 166 21 179Z',white)+path('M91 158Q131 143 169 157V179Q128 166 91 181Z','#cce1d4')+line(91,160,91,179,ink,2))
scenes['candle']=('شمعة مشتعلة',rect(75,89,42,65,white)+path('M95 81C64 65 94 45 98 24C124 51 128 74 95 81Z',gold)+path('M96 77C85 65 98 52 100 45C113 62 109 72 96 77Z','#ffdd7b')+line(96,82,96,91,ink,3)+rect(58,154,76,13,teal)+path('M75 93Q88 109 91 94L99 99L103 92', '#e5d4ba'))
scenes['lighthouse']=('منارة ساحلية ترسل شعاعًا',path('M82 59L111 59L126 155H67Z',white)+path('M78 82H115L119 104H75Z',gold)+rect(76,40,42,21,teal)+path('M70 40L97 21L124 40Z',ink)+path('M120 45L182 22V83Z','#f4d17a')+rect(88,128,17,27,ink)+path('M0 157Q22 140 43 157T86 157T129 157T172 157T215 157V192H0Z',blue))
scenes['rain']=('سحابة يتساقط منها المطر',cloud(53,57)+''.join(line(x,y,x-8,y+23,blue,5) for x,y in [(61,101),(91,108),(121,101),(78,143),(112,140)])+path('M16 181Q96 150 176 181Z','#7bb59b'))
scenes['river']=('نهر يجري بين ضفتين',path('M0 105L43 42L91 102L135 30L192 103V192H0Z','#86b7a1')+path('M135 30L153 65L131 60L119 66Z',white)+path('M86 99Q130 123 92 145Q47 170 103 192H159Q82 168 122 148Q164 122 105 99Z',blue)+circle(33,31,12,gold))
scenes['glass']=('كأس فيه ماء',path('M49 42H143L129 167H63Z','#d5e9e9')+path('M56 91Q96 103 136 91L128 161H64Z',blue)+path('M49 42H143L129 167H63Z','none')+f'<path d="M49 42H143L129 167H63Z" fill="none" stroke="{ink}" stroke-width="4"/>'+line(70,55,77,142,white,5)+circle(108,120,5,'#b2dfeb'))
scenes['droplet']=('قطرة ماء زرقاء على ورقة',path('M21 163Q45 118 90 148Q133 115 176 163Q99 193 21 163Z','#8db99d')+path('M98 29C89 57 57 84 57 113C57 163 140 163 140 113C140 83 109 57 98 29Z',blue)+path('M78 105Q66 128 90 137',white)+line(35,163,159,163,teal,3))
scenes['hourglass']=('ساعة رملية',rect(53,26,86,12,teal)+rect(53,156,86,12,teal)+path('M63 41H129Q129 78 102 94Q129 116 129 153H63Q63 118 90 94Q63 78 63 41Z','#d7e8e5')+path('M69 53H123Q121 73 96 87Q73 73 69 53Z',gold)+path('M67 150Q79 123 96 111Q115 128 125 150Z',gold)+line(96,92,96,111,gold,3))
scenes['watch']=('ساعة يد بعقارب',rect(76,13,40,167,teal)+rect(80,18,32,19,ink)+circle(96,96,48,gold)+circle(96,96,40,white)+line(96,96,96,69,ink,5)+line(96,96,119,108,ink,5)+circle(96,96,4,teal)+''.join(line(x,y,a,b,ink,2) for x,y,a,b in [(96,62,96,67),(96,125,96,130),(62,96,67,96),(125,96,130,96)]))
calendar=rect(30,42,132,120,white)+rect(30,42,132,31,teal)+line(63,30,63,52,ink,6)+line(128,30,128,52,ink,6)
for row in range(3):
 for col in range(4):calendar+=rect(46+col*28,88+row*22,15,12,gold if(row,col)==(1,2) else '#c5dace',3)
scenes['calendar']=('تقويم أيام وشهور',calendar)
scenes['dawn']=('شروق يعلن بداية يوم جديد',circle(96,110,40,gold)+path('M0 129L42 72L82 126L141 64L192 124V192H0Z',teal)+path('M11 170Q89 142 179 169V192H11Z','#8cbb9d')+line(93,40,93,24,gold,4)+line(44,63,34,49,gold,4)+line(143,58,153,45,gold,4))
scenes['suitcase']=('حقيبة رحلات بعجلات',f'<path d="M74 53V36Q96 22 118 36V53" fill="none" stroke="{ink}" stroke-width="7"/>'+rect(44,51,105,106,teal)+rect(57,63,80,79,'#2c8277')+line(70,71,70,133,'#a7d4bc',4)+line(119,71,119,133,'#a7d4bc',4)+circle(67,165,7,ink)+circle(128,165,7,ink)+rect(91,77,29,23,gold,2))
scenes['plane']=('طائرة بين السحب',cloud(8,140)+cloud(124,42)+path('M24 86L87 81L109 32L121 32L110 80L165 91L165 103L110 106L119 143L110 146L88 107L26 108L42 96Z',teal)+line(64,91,145,96,gold,3))
scenes['train']=('قطار يسير على سكة',rect(24,62,97,75,teal)+rect(122,83,48,54,gold)+rect(35,75,29,30,'#b4dce6')+rect(75,75,29,30,'#b4dce6')+rect(126,94,29,21,white)+circle(49,143,13,ink)+circle(101,143,13,ink)+circle(150,143,13,ink)+rect(145,57,15,29,ink,2)+cloud(125,24)+line(11,160,181,160,ink,5)+line(18,175,181,175,ink,5))
scenes['passport']=('جواز سفر وبطاقة صعود',rect(31,31,101,132,teal)+circle(82,88,29,gold)+circle(82,88,24,teal)+f'<ellipse cx="82" cy="88" rx="12" ry="25" fill="none" stroke="{gold}" stroke-width="2"/>'+line(58,88,106,88,gold,2)+line(82,64,82,112,gold,2)+rect(93,121,81,35,white,2)+''.join(rect(102+i*7,130,3,17,ink,0) for i in range(8)))
sets=[('نور',['sun','lamp','candle','lighthouse'],'تجمعها فكرة الإضاءة: الشمس والمصباح والشمعة والمنارة مصادر للنور.'),('ماء',['rain','river','glass','droplet'],'المطر والنهر وما في الكأس والقطرة صور مختلفة للماء.'),('وقت',['hourglass','watch','calendar','dawn'],'الساعة الرملية وساعة اليد والتقويم وتعاقب الأيام تربطها فكرة الوقت.'),('سفر',['suitcase','plane','train','passport'],'الحقيبة والطائرة والقطار وجواز السفر قرائن مرتبطة بالسفر.')]
original=[json.loads(l) for l in (ROOT/'packs/opening-pack-120/data/puzzles.jsonl').read_text().splitlines()]
(folder/'media').mkdir(exist_ok=True);(folder/'media-src').mkdir(exist_ok=True)
assets=[];puzzles=[]
for index,(answer,names,explanation) in enumerate(sets):
 p=original[20+index];old=p['puzzleId'];p['puzzleId']=uid('corrected/'+old);p['revision']=3;p['title']='أربع قرائن وفكرة';p['sourceRefs']=[source];p['tags']=['supersedes:'+old];p['explanation']=explanation
 images=[]
 for name in names:
  alt,art=scenes[name]
  svg=f'<svg xmlns="http://www.w3.org/2000/svg" width="384" height="384" viewBox="0 0 192 192"><rect width="192" height="192" rx="18" fill="#f7f3eb"/>{art}</svg>'
  source_path=folder/'media-src'/f'{name}.svg';source_path.write_text(svg)
  with tempfile.TemporaryDirectory() as temp:
   png=Path(temp)/'image.png';subprocess.run(['inkscape',str(source_path),'--export-type=png','--export-filename='+str(png)],check=True,capture_output=True)
   out=io.BytesIO();Image.open(png).convert('RGB').save(out,format='WEBP',lossless=True,method=6);data=out.getvalue()
  digest=hashlib.sha256(data).hexdigest();path='media/'+digest+'.webp';(folder/path).write_bytes(data)
  asset_id=uid('image/'+name)
  images.append(dict(id=asset_id,assetPath=path,alt=alt,role='clue',sourceRef=source,license='LicenseRef-Lughzdan-Literary',width=384,height=384,sha256=digest))
  assets.append(dict(assetId=asset_id,path=path,sourceReferenceId=source,altText=alt))
 p['hints']=[dict(id='h1',kind='step',text=f'الكلمة من {len(answer)} أحرف، وتجمع الصور في معنى واحد.',penalty=15)]
 p['payload']=dict(prompt='ما الكلمة التي تربط هذه القرائن الأربع؟',images=images,acceptedAnswers=[answer],tiles=list(answer)+['ل','ب','ش'],fixedSlots=[],sourceLabel='رسوم متجهية أصلية أعدت للُغْزَدان',sourceUrl='')
 puzzles.append(p)
(folder/'visual-puzzles.jsonl').write_text(''.join(json.dumps(p,ensure_ascii=False,separators=(',',':'))+'\n' for p in puzzles))
(folder/'visual-assets.json').write_text(json.dumps(assets,ensure_ascii=False,indent=2)+'\n')
(folder/'visual-sources.json').write_text(json.dumps([dict(key='atlas-original-visuals',referenceId=source,name='رسوم متجهية أصلية أعدت للُغْزَدان — قرائن مترابطة',url='')],ensure_ascii=False,indent=2)+'\n')
print('16 distinct original scenes and four semantic visual puzzles authored')
