import manifestSchema from '../../../contracts/manifest.schema.json' with {type:'json'};
import {Ajv2020} from 'ajv/dist/2020.js';
import {createRequire} from 'node:module';
import type {FormatsPlugin} from 'ajv-formats';
const schemaAjv=new Ajv2020({strict:false});
(createRequire(import.meta.url)('ajv-formats') as FormatsPlugin)(schemaAjv);
const validateManifest=schemaAjv.compile(manifestSchema);
import { createHash, verify, type KeyObject } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { ApiError } from './errors.js';

// RFC8785 uses ECMAScript JSON number/string serialization and UTF-16 key order.
export function canonicalize(value: unknown): string {
  if (value === null || typeof value === 'boolean' || typeof value === 'string') {
    if (typeof value === 'string' && /[\uD800-\uDBFF](?![\uDC00-\uDFFF])|(?<![\uD800-\uDBFF])[\uDC00-\uDFFF]/u.test(value)) throw new Error('Invalid Unicode');
    return JSON.stringify(value);
  }
  if (typeof value === 'number') { if (!Number.isFinite(value) || !Number.isSafeInteger(value)) throw new Error('Unsafe manifest number'); return JSON.stringify(value); }
  if (Array.isArray(value)) return '[' + value.map(canonicalize).join(',') + ']';
  if (typeof value === 'object') return '{' + Object.keys(value).sort().map(key => canonicalize(key) + ':' + canonicalize((value as Record<string, unknown>)[key])).join(',') + '}';
  throw new Error('Unsupported JSON value');
}
export const digest = (value: unknown) => createHash('sha256').update(canonicalize(value)).digest('hex');
export interface StorageProvider { objectUrl(sha256: string): Promise<string>; }
export class HttpsStorageProvider implements StorageProvider {
  private readonly base: URL;
  constructor(baseUrl: string) { this.base = new URL(baseUrl); if (this.base.protocol !== 'https:' || this.base.username || this.base.password || this.base.search || this.base.hash) throw new Error('HTTPS object origin required'); }
  async objectUrl(hash: string) { assertHash(hash); return new URL(hash, this.base.href.replace(/\/?$/, '/')).href; }
}
export class LocalStorageProvider implements StorageProvider {
  constructor(private readonly directory: string, private readonly publicBaseUrl: string) { new HttpsStorageProvider(publicBaseUrl); }
  async objectUrl(hash: string) { assertHash(hash); const bytes = await readFile(resolve(this.directory, hash)); if (createHash('sha256').update(bytes).digest('hex') !== hash) throw new Error('Object hash mismatch'); return new HttpsStorageProvider(this.publicBaseUrl).objectUrl(hash); }
}
function assertHash(hash: string) { if (!/^[a-f0-9]{64}$/.test(hash)) throw new Error('Invalid object hash'); }
export type SignedManifest = Record<string, unknown> & { packId: string; version: number; keyId: string; signature: string; files: { sha256: string; bytes: number; path: string; chunks: { sha256: string; bytes: number }[] }[]; downloadBytes: number; installedBytes: number; };
export class ManifestVerifier {
  constructor(private readonly trust: ReadonlyMap<string, KeyObject>) {}
  verify(manifest: SignedManifest, expectedHash?: string) {
    if(!validateManifest(manifest))throw new ApiError(503,'INVALID_MANIFEST','Invalid manifest');
    const { signature, ...payload } = manifest;
    if(Buffer.from(signature,'base64url').toString('base64url')!==signature)throw new ApiError(503,'INVALID_MANIFEST','Invalid manifest');
    const key = this.trust.get(manifest.keyId);
    if (!key || key.asymmetricKeyType !== 'ed25519' || !/^[A-Za-z0-9_-]{86}$/.test(signature) || !verify(null, Buffer.from(canonicalize(payload)), key, Buffer.from(signature, 'base64url'))) throw new ApiError(503, 'INVALID_MANIFEST', 'Invalid manifest');
    const chunks = new Map<string, number>(); const paths = new Set<string>(); let installed = 0;
    for (const file of manifest.files) {
      if (paths.has(file.path) || !/^(data|media|rights)\/[A-Za-z0-9_/-]+\.(jsonl|webp|png|jpg|ogg|txt)$/.test(file.path) || file.path.includes('..')) throw new Error('Unsafe manifest path'); paths.add(file.path); assertHash(file.sha256); installed += file.bytes; if(!Number.isSafeInteger(installed))throw new Error('Unsafe installed size');
      let bytes = 0; for (const chunk of file.chunks) { assertHash(chunk.sha256); if (!Number.isSafeInteger(chunk.bytes) || chunk.bytes < 1 || chunk.bytes > 8388608 || (chunks.has(chunk.sha256) && chunks.get(chunk.sha256) !== chunk.bytes)) throw new Error('Invalid chunk'); chunks.set(chunk.sha256, chunk.bytes); bytes += chunk.bytes; if(!Number.isSafeInteger(bytes))throw new Error('Unsafe file size'); }
      if (bytes !== file.bytes) throw new Error('Invalid file size');
    }
    const download=[...chunks.values()].reduce((a,b)=>{const sum=a+b;if(!Number.isSafeInteger(sum))throw new Error('Unsafe download size');return sum;},0);
    if (installed !== manifest.installedBytes || download !== manifest.downloadBytes || (expectedHash && digest(manifest) !== expectedHash)) throw new Error('Manifest size/hash mismatch');
    return manifest;
  }
}
