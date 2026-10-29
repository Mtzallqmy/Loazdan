import type { PoolConfig } from 'pg';

export function databaseConfig(connectionString: string, production: boolean): PoolConfig {
  const url = new URL(connectionString);
  if (!['postgres:', 'postgresql:'].includes(url.protocol)) throw new Error('A PostgreSQL connection is required');
  const local = ['localhost', '127.0.0.1', '[::1]'].includes(url.hostname);
  // pg's connection string sslmode can override a supplied ssl object. Normalize
  // those parameters first so production cannot silently disable verification.
  for (const key of ['sslmode', 'sslcert', 'sslkey', 'sslrootcert']) url.searchParams.delete(key);
  return { connectionString: url.toString(), ssl: production || !local ? { rejectUnauthorized: true } : false, max: 5, idleTimeoutMillis: 30_000, connectionTimeoutMillis: 5_000, application_name: 'lughzdan-catalog-api' };
}
export function listenConfig(environment: NodeJS.ProcessEnv) {
  const port = Number(environment.PORT ?? 3001);
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error('PORT must be an integer from 1 to 65535');
  return { port, host: environment.HOST ?? '127.0.0.1' };
}

// Public Ed25519 SPKI keys only. Authentication is deliberately a separate port.
export async function contentConfig(environment:NodeJS.ProcessEnv):Promise<import('./routes.js').ContentOptions>{
 const {readFile}=await import('node:fs/promises');const {createPublicKey}=await import('node:crypto');const {ManifestVerifier,HttpsStorageProvider}=await import('./content.js');
 const trustFile=environment.CONTENT_TRUST_FILE;const storageBase=environment.CONTENT_STORAGE_BASE_URL;
 if(!trustFile&&!storageBase)return {};
 if(!trustFile||!storageBase)throw new Error('CONTENT_TRUST_FILE and CONTENT_STORAGE_BASE_URL must be configured together');
 const raw:unknown=JSON.parse(await readFile(trustFile,'utf8'));if(!raw||typeof raw!=='object'||Array.isArray(raw))throw new Error('Trust map object required');
 const entries=Object.entries(raw);if(entries.length<1||entries.length>100)throw new Error('Trust map size invalid');
 const keys=new Map<string,import('node:crypto').KeyObject>();for(const [id,pem] of entries){if(!/^[A-Za-z0-9_-]{1,64}$/.test(id)||typeof pem!=='string'||!pem.startsWith('-----BEGIN PUBLIC KEY-----')||pem.length>4096)throw new Error('Public trust key required');const key=createPublicKey(pem);if(key.asymmetricKeyType!=='ed25519')throw new Error('Ed25519 key required');keys.set(id,key);}
 return {manifests:new ManifestVerifier(keys),storage:new HttpsStorageProvider(storageBase)};
}
