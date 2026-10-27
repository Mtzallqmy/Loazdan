"""Conservative tracked-source secret guard; supplements GitHub secret scanning."""
import pathlib,re,subprocess,sys
patterns=[re.compile(rb'postgres(?:ql)?://[^\s:]+:[^\s@]+@'),re.compile(rb'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'),re.compile(rb'gh[pousr]_[A-Za-z0-9]{30,}')]
failures=[]
for name in subprocess.check_output(['git','ls-files'],text=True).splitlines():
 p=pathlib.Path(name)
 if not p.is_file() or name==__file__:continue
 data=p.read_bytes()
 if any(pattern.search(data) for pattern in patterns):failures.append(name)
 if name.startswith('apps/android/') and p.suffix in {'.kt','.xml','.properties'} and b'DATABASE_URL' in data:failures.append(name)
if failures:print('Secret-shaped content requires review:',*failures,sep='\n');sys.exit(1)
print('Tracked source secret-pattern scan passed (not a full credential audit).')
