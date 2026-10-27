import assert from 'node:assert/strict';
import { test } from 'node:test';
import { databaseConfig, listenConfig } from '../src/config.js';
import { decodeCursor, scopeFor } from '../src/pagination.js';
test('production TLS is verified despite a malicious disable flag', () => {
  const config = databaseConfig('postgres://catalog@db.invalid/app?sslmode=disable',true);
  assert.deepEqual(config.ssl,{rejectUnauthorized:true}); assert.ok(!config.connectionString?.includes('sslmode'));
  assert.equal(databaseConfig('postgres://catalog@localhost/app',false).ssl,false);
});
test('invalid configuration and cursor structures fail closed', () => {
  assert.throws(() => databaseConfig('https://example.invalid',false)); assert.throws(() => listenConfig({PORT:'-1'}));
  const scope = scopeFor('puzzles',{}); const forge=(key: unknown) => Buffer.from(JSON.stringify({v:1,scope,key})).toString('base64url');
  for (const key of [null,[],{},'x',2147483648]) assert.throws(() => decodeCursor(forge(key),scope));
  assert.equal(decodeCursor(undefined,scope),null);
});
