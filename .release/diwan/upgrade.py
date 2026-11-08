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
   if label in (n.get('text'),n.get('content-desc')):return n
  if scroll and i in (2,5,8,11):adb('shell','input','swipe','500','1500','500','600','350')
  time.sleep(.4)
 raise ValueError('missing '+label+': '+str([n.get('text') for n in nodes() if n.get('text')]))
def click(label,scroll=False):
 n=find(label,scroll);x,y,r,b=map(int,re.findall(r'\d+',n.get('bounds')));adb('shell','input','tap',(x+r)//2,(y+b)//2)
def launch(pkg):
 adb('shell','am','start','-W','-n',pkg+'/com.lughzdan.app.MainActivity');find('فضولك بداية الرحلة')
adb('shell','wm','size','1080x1920');adb('shell','wm','density','360')
adb('shell','svc','wifi','disable');adb('shell','svc','data','disable')
for old in sorted(Path('old').glob('*.apk')):
 suffix='-Diwan' if '-Diwan-' in old.name else ''
 pkg='com.lughzdan.app'+('.preview.diwan' if suffix else '')
 assert 'Success' in adb('install',old)
 launch(pkg);click('جديد · ديوان الألغاز',True);click('ابدأ التحدي',True)
 find('العودة · تُحفظ حركاتك تلقائيًا')
 adb('shell','am','force-stop',pkg)
 assert 'Success' in adb('install','-r',Path('new')/f'Lughzdan-3.0.0-beta.1{suffix}-ARM64.apk')
 launch(pkg);click('جديد · ديوان الألغاز',True);click('تابع جولتك في الديوان');find('العودة · تُحفظ حركاتك تلقائيًا')
 adb('shell','am','force-stop',pkg);launch(pkg);click('ديوان العرب',True);click('السيف أصدق أنباء من الكتب');click('اختبر نفسك');find('حافة السيف ثم الفصل بين أمرين')
 print('Upgrade retained a real game round; anthology opens offline:',pkg,flush=True)
