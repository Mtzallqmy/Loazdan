import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildApp } from '../src/app.js';
import type { CatalogRepository } from '../src/repository.js';

test('120 requests per minute per socket IP; forwarded IP spoofing cannot bypass limit', async () => {
  const app = buildApp({} as CatalogRepository);
  try {
    for (let n = 0; n < 120; n++) {
      const result = await app.inject({ url: '/health', remoteAddress: '192.0.2.1', headers: { 'x-forwarded-for': `198.51.100.${n}` } });
      assert.equal(result.statusCode, 200);
    }
    const limited = await app.inject({ url: '/health', remoteAddress: '192.0.2.1', headers: { 'x-forwarded-for': '203.0.113.1' } });
    assert.equal(limited.statusCode, 429);
    assert.equal(limited.json().code, 'RATE_LIMITED');
    assert.equal(limited.json().status, 429);
    assert.match(limited.json().requestId, /^[0-9a-f-]{36}$/);
    assert.equal(limited.headers['cache-control'], 'no-store');
    assert.match(String(limited.headers['content-type']), /^application\/problem\+json/);
    assert.ok(Number(limited.headers['retry-after']) >= 1);
    assert.ok(Number(limited.headers['retry-after']) <= 60);
    assert.equal((await app.inject({ url: '/health', remoteAddress: '192.0.2.2' })).statusCode, 200);
  } finally { await app.close(); }
});
