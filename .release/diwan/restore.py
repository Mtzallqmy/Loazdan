import hashlib,json,os,shutil,subprocess,tempfile
from pathlib import Path
root=Path.cwd(); assets=root/'.release/diwan'; out=root/'dist';out.mkdir(exist_ok=True)
for i,m in enumerate(json.loads((assets/'manifest.json').read_text())):
 with tempfile.TemporaryDirectory() as d:
  f=Path(d)/'target.apk';shutil.copy(root/'inputs'/m['input'],f)
  subprocess.run(['git','apply','--no-index','--binary',str(assets/f'{i}.patch')],cwd=d,check=True)
  assert f.stat().st_size==m['bytes'] and hashlib.sha256(f.read_bytes()).hexdigest()==m['digest']
  sdk=Path(os.environ['ANDROID_HOME'])/'build-tools/36.0.0'
  cert=subprocess.check_output([sdk/'apksigner','verify','--verbose','--print-certs',f],text=True);assert m['certificate'] in cert
  subprocess.run(['python','scripts/verify_release_apk.py',f],check=True)
  shutil.copy(f,out/m['name'])
(out/'SHA256SUMS').write_text(''.join(f"{hashlib.sha256(f.read_bytes()).hexdigest()}  {f.name}\n" for f in sorted(out.glob('*.apk'))))
