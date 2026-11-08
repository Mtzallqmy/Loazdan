"""Original short, quiet game cues, synthesized without external recordings."""
from pathlib import Path
import math,struct,wave
folder=Path(__file__).resolve().parents[1]/'apps/android/feature/games/src/main/res/raw'
folder.mkdir(parents=True,exist_ok=True)
rate=22050
cues={'answer_success':[(523.25,.08),(659.25,.08),(783.99,.13)],'answer_retry':[(392,.09),(329.63,.13)],'word_discovery':[(659.25,.07),(987.77,.12)]}
for name,notes in cues.items():
 samples=[]
 for frequency,duration in notes:
  count=round(rate*duration)
  for i in range(count):
   t=i/rate
   envelope=min(1,i/(rate*.006))*math.exp(-5*i/count)*min(1,(count-i)/(rate*.012))
   signal=(math.sin(2*math.pi*frequency*t)+.20*math.sin(2*math.pi*frequency*2*t))*.22*envelope
   samples.append(round(32767*signal))
 with wave.open(str(folder/(name+'.wav')),'wb') as out:
  out.setnchannels(1);out.setsampwidth(2);out.setframerate(rate);out.writeframes(struct.pack('<'+'h'*len(samples),*samples))
print('3 original PCM cues generated')
