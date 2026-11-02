import { Pool } from 'pg';
import { buildApp } from './app.js';
import { databaseConfig } from './config.js';
import { CatalogDatabase } from './database.js';
import { CatalogRepository } from './repository.js';
import { createPublicKey } from 'node:crypto';
import { ManifestVerifier, HttpsStorageProvider } from './content.js';
import trust from '../release/trust.json' with { type: 'json' };
import objects from '../release/objects.json' with { type: 'json' };

// The function uses a dedicated LOGIN that can only SET ROLE api_catalog.
const connection = process.env.CATALOG_DATABASE_URL;
if (!connection) throw new Error('CATALOG_DATABASE_URL is required');
const pool = new Pool(databaseConfig(connection, true));
pool.on('error', () => console.error('Idle catalog connection closed'));
const origin = process.env.PUBLIC_API_URL;
if (!origin?.startsWith('https://')) throw new Error('PUBLIC_API_URL required');
const app = buildApp(new CatalogRepository(new CatalogDatabase(pool)), {
  manifests: new ManifestVerifier(new Map(Object.entries(trust).map(([id, pem]) => [id, createPublicKey(pem)]))),
  storage: new HttpsStorageProvider(origin + 'objects/')
});
const ready = app.ready();

export default {
  async fetch(request: Request): Promise<Response> {
    await ready;
    const url = new URL(request.url);
    if (url.pathname.startsWith('/objects/') && ['GET', 'HEAD'].includes(request.method)) {
      const hash = url.pathname.slice('/objects/'.length);
      const data = (objects as Record<string, string>)[hash];
      if (!/^[a-f0-9]{64}$/.test(hash) || !data) return new Response(null, {status: 404});
      const bytes = Buffer.from(data, 'base64');
      const etag = '"' + hash + '"';
      const headers = new Headers({'content-type':'application/octet-stream', 'cache-control':'public, max-age=31536000, immutable', 'etag':etag, 'accept-ranges':'bytes', 'x-content-type-options':'nosniff'});
      if (request.headers.get('if-none-match') === etag) return new Response(null, {status:304,headers});
      const range = request.headers.get('range');
      const match = range?.match(/^bytes=(\d+)-$/);
      if (range && !match) return new Response(null, {status:416});
      const offset = match && (!request.headers.has('if-range') || request.headers.get('if-range') === etag) ? Number(match[1]) : 0;
      if (!Number.isSafeInteger(offset) || offset >= bytes.length) return new Response(null, {status:416,headers:{'content-range':`bytes */${bytes.length}`}});
      const partial = !!match && offset > 0;
      if (partial) headers.set('content-range', `bytes ${offset}-${bytes.length-1}/${bytes.length}`);
      headers.set('content-length', String(bytes.length-offset));
      return new Response(request.method === 'HEAD' ? null : Uint8Array.from(bytes.subarray(offset)), {status:partial?206:200,headers});
    }
    // Preserve Fastify schemas, size limits, ETags and closed sync authentication.
    if (request.method !== 'GET' && request.method !== 'HEAD') {
      return Response.json({ code: 'METHOD_NOT_ALLOWED' }, { status: 405, headers: { allow: 'GET, HEAD' } });
    }
    const response = await app.inject({ method: request.method, url: url.pathname + url.search,
      headers: { 'if-none-match': request.headers.get('if-none-match') ?? '' } });
    const headers = new Headers();
    for (const [name, value] of Object.entries(response.headers)) {
      if (value !== undefined) headers.set(name, Array.isArray(value) ? value.join(', ') : String(value));
    }
    return new Response(request.method === 'HEAD' || response.statusCode === 304 ? null : Uint8Array.from(response.rawPayload),
      { status: response.statusCode, headers });
  }
};
