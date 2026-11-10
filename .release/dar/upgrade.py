from pathlib import Path
import subprocess,time,re,xml.etree.ElementTree as ET
def adb(*args):
 return subprocess.check_output(['adb',*map(str,args)],text=True,timeout=40).strip()
def nodes():
 adb('shell','uiautomator','dump','/sdcard/tree.xml')
 return list(ET.fromstring(adb('exec-out','cat','/sdcard/tree.xml')).iter('node'))
def find(label,scroll=False):
 for i in range(18):
  for n in nodes():
   if any(label == v or label in v.split('\n') for v in (n.get('text') or '',n.get('content-desc') or '')):return n
  if scroll and i in (2,5,8,11):
   x,y,r,b=map(int,re.findall(r'\d+',nodes()[0].get('bounds')));adb('shell','input','swipe',(r+x)//2,int(y+(b-y)*.78),(r+x)//2,int(y+(b-y)*.32),'400')
  time.sleep(.4)
 raise ValueError('missing '+label+': '+str([n.get('text') for n in nodes() if n.get('text')]))
def click(label,scroll=False):
 n=find(label,scroll);x,y,r,b=map(int,re.findall(r'\d+',n.get('bounds')));adb('shell','input','tap',(x+r)//2,(y+b)//2)

import json
evidence=Path('metrics');evidence.mkdir(exist_ok=True)
def launch(pkg,ready):
 info=adb('shell','am','start','-W','-n',pkg+'/com.lughzdan.app.MainActivity')
 find(ready,True)
 return int(re.search(r'TotalTime:\s*(\d+)',info).group(1))
def cold(pkg,ready):
 samples=[]
 for _ in range(3):
  adb('shell','am','force-stop',pkg);samples.append(launch(pkg,ready))
 return samples
def screen(name):
 with open(evidence/name,'wb') as f:subprocess.run(['adb','exec-out','screencap','-p'],stdout=f,check=True)
def enter(label,ready):
 t=time.monotonic();click(label,True);find(ready,True);return round(1000*(time.monotonic()-t))
adb('shell','wm','size','1080x1920');adb('shell','wm','density','200')
adb('shell','svc','wifi','disable');adb('shell','svc','data','disable')
results={}
for old in sorted(Path('old').glob('*.apk')):
 suffix='-Diwan' if '-Diwan-' in old.name else ''
 pkg='com.lughzdan.app'+('.preview.diwan' if suffix else '')
 assert 'Success' in adb('install',old)
 oldtimes=cold(pkg,'فضولك بداية الرحلة')
 oldnav=enter('ديوان العرب','السيف أصدق أنباء من الكتب')
 click('السيف أصدق أنباء من الكتب');click('احفظ في ديواني',True)
 find('إزالة من ديواني',True)
 adb('shell','am','force-stop',pkg);launch(pkg,'فضولك بداية الرحلة');click('مكتبتي');click('ابدأ الجولة',True)
 find('العودة · تُحفظ حركاتك تلقائيًا')
 adb('shell','am','force-stop',pkg)
 assert 'Success' in adb('install','-r',Path('new')/f'Lughzdan-3.1.0-beta.1{suffix}-ARM64.apk')
 newtimes=cold(pkg,'دار الأثر')
 click('متابعة آخر نشاط',True);find('العودة · تُحفظ حركاتك تلقائيًا')
 adb('shell','am','force-stop',pkg);launch(pkg,'دار الأثر')
 newnav=enter('ديواني','السيف أصدق أنباء من الكتب')
 click('محفوظاتي');click('السيف أصدق أنباء من الكتب');find('إزالة من ديواني',True)
 adb('shell','am','force-stop',pkg);launch(pkg,'دار الأثر')
 if not suffix:
  click('حدّ السيف وحدّ المعنى',True); find('اختر قرينتك الأولى',True); screen('dar-manuscript-light.png')
  adb('shell','am','force-stop',pkg);launch(pkg,'دار الأثر')
  click('ملفي');click('الإعدادات')
  label=find('الوضع الداكن',True);y=re.findall(r'\d+',label.get('bounds'));cy=(int(y[1])+int(y[3]))//2
  switches=[n for n in nodes() if n.get('checkable')=='true']
  def center(n):
   x,y,r,b=map(int,re.findall(r'\d+',n.get('bounds')));return ((x+r)//2,(y+b)//2)
  switch=min(switches,key=lambda n:abs(center(n)[1]-cy))
  if switch.get('checked')=='false':adb('shell','input','tap',*center(switch))
  adb('shell','am','force-stop',pkg);launch(pkg,'دار الأثر')
 click('ظلّ في فناء تعز',True);click('افحص الشاخص',True);click('يقصر الظل',True)
 if not suffix:screen('dar-shadow-dark.png')
 adb('shell','dumpsys','gfxinfo',pkg,'reset')
 for _ in range(8):click('رفع الضوء',True)
 (evidence/(pkg+'-frames.txt')).write_text(adb('shell','dumpsys','gfxinfo',pkg,'framestats'))
 results[pkg]={'oldColdStartMs':oldtimes,'newColdStartMs':newtimes,'oldLibraryViaUiAutomatorMs':oldnav,'newLibraryViaUiAutomatorMs':newnav}
 (evidence/'bench.json').write_text(json.dumps(results,indent=2))
 adb('shell','am','force-stop',pkg);launch(pkg,'دار الأثر');click('ظلّ في فناء تعز',True);find('ضع العلامة الأولى',True)
 if not suffix:
  adb('shell','wm','size','720x1280');adb('shell','wm','density','320');adb('shell','settings','put','system','font_scale','2.0')
  adb('shell','am','force-stop',pkg);launch(pkg,'دار الأثر');click('ظلّ في فناء تعز',True);find('ضع العلامة الأولى',True);screen('dar-360dp-200pct.png')
  adb('shell','settings','put','system','font_scale','1.0');adb('shell','wm','size','1080x1920');adb('shell','wm','density','200')
 print('PASS',pkg,'old game, old saved verse, scene after process death')
