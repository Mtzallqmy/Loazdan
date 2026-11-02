import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {createHash,generateKeyPairSync,createPrivateKey,createPublicKey,sign} from 'node:crypto';
import {canonicalize,ManifestVerifier,digest} from '../src/content.js';
const root=new URL('../../../',import.meta.url);
const assets=new URL('apps/android/app/src/main/assets/',root);
const keyPath=process.env.CONTENT_SIGNING_KEY_FILE;
if(!keyPath)throw new Error('CONTENT_SIGNING_KEY_FILE required outside the repository');
let key;
try{key=createPrivateKey(await readFile(keyPath));}catch(e:any){if(e.code!=='ENOENT')throw e;key=generateKeyPairSync('ed25519').privateKey;await writeFile(keyPath,key.export({type:'pkcs8',format:'pem'}),{mode:0o600});}
const publicKey=createPublicKey(key);
const bundled=JSON.parse(await readFile(new URL('bundled-manifest.json',assets),'utf8'));
const puzzles=(await readFile(new URL(bundled.path,assets),'utf8')).trim().split('\n').map(s=>JSON.parse(s));
const names=JSON.parse(await readFile(new URL('release-references.json',assets),'utf8'));
const images=new Map<string,any>();for(const p of puzzles)for(const image of p.payload.images??[])images.set(image.assetPath,image);
const refs=[...new Set<string>(puzzles.flatMap(p=>p.sourceRefs))].sort();
const source=images.values().next().value.sourceRef;
const paths=[bundled.path,...images.keys(),'rights/original.txt'];
const objects:Record<string,string>={};const files=[];
for(const path of paths){const b=await readFile(new URL(path,assets));const hash=createHash('sha256').update(b).digest('hex');objects[hash]=b.toString('base64');files.push({path,kind:path.startsWith('data/')?'puzzles':path.startsWith('media/')?'media':'rights',sha256:hash,bytes:b.length,chunks:[{sha256:hash,bytes:b.length}]});}
const bytes=files.reduce((n,f)=>n+f.bytes,0);
const payload={schemaVersion:1,packId:bundled.packId,version:bundled.version,minAppVersionCode:100,minEngineVersion:3,locale:'ar',publishedAt:'2026-11-02T00:00:00Z',files,downloadBytes:bytes,installedBytes:bytes,dependencies:[],sourceRefs:refs,keyId:'release2026',
 assets:[...images.values()].map(i=>({assetId:i.sha256.slice(0,8)+'-'+i.sha256.slice(8,12)+'-5'+i.sha256.slice(13,16)+'-a'+i.sha256.slice(17,20)+'-'+i.sha256.slice(20,32),path:i.assetPath,sourceReferenceId:i.sourceRef,altText:i.alt})),
 references:refs.map(id=>({referenceId:id,sourceId:id,sourceName:id===source?'لُغزدان — الرسوم الأصلية':names[id]??id,author:id===source?'مالك مشروع لُغزدان':'لم يحدد في سجل المصدر الأصلي',locator:id===source?'scripts/prepare_release_pack.py':names[id]??id,sourceVersion:'opening-2026',licenseCode:id===source?'LicenseRef-Lughzdan-Original':'LicenseRef-Source-Text',attribution:id===source?'Original illustrations for LUGHZDAN':names[id]??id,noticePath:'rights/original.txt'}))};
const manifest={...payload,signature:sign(null,Buffer.from(canonicalize(payload)),key).toString('base64url')};
new ManifestVerifier(new Map([['release2026',publicKey]])).verify(manifest,digest(manifest));
await mkdir(new URL('services/api/release/',root),{recursive:true});
for(const path of ['services/api/release/manifest.json','packs/opening-pack-120/manifest.json','apps/android/app/src/main/assets/opening-pack-manifest.json']) await writeFile(new URL(path,root),JSON.stringify(manifest,null,2)+'\n');
await writeFile(new URL('services/api/release/objects.json',root),JSON.stringify(objects));
await writeFile(new URL('services/api/release/trust.json',root),JSON.stringify({release2026:publicKey.export({type:'spki',format:'pem'})},null,2)+'\n');
const der=publicKey.export({type:'spki',format:'der'});
await writeFile(new URL('pack-public-keys.json',assets),JSON.stringify({release2026:der.subarray(-32).toString('base64')})+'\n');
console.log(JSON.stringify({packId:manifest.packId,puzzles:puzzles.length,files:files.length,bytes,manifestSha256:digest(manifest)}));
