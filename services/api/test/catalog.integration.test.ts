import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { before, after, test } from 'node:test';
import type { FastifyInstance } from 'fastify';
import { buildApp } from '../src/app.js';
import { CatalogRepository } from '../src/repository.js';
import { migrate } from '../src/migrations.js';
import { startTestDatabase } from './postgres.js';

const id = (n: number) => `20000000-0000-4000-8000-${n.toString().padStart(12, '0')}`;
let db: Awaited<ReturnType<typeof startTestDatabase>>;
let app: FastifyInstance;
const definition = (n: number, fixtureOnly = false) => ({ schemaVersion: 1, puzzleId: id(n), revision: 1, gameId: id(3), type: 'choice', world: 'logic', difficulty: 1, rulesVersion: 1, normalization: 'arabic_basic_v1', title: 'سؤال رياضي أصلي للاختبار المحلي', status: 'published', fixtureOnly, sourceRefs: [id(90)], explanation: 'اختبار أصلي؛ ليس اعتماد نشر حقيقيًا.', hints: [], payload: { prompt: 'كم حاصل ٢ + ٢؟', options: [{ id: 'a', text: '٣' }, { id: 'b', text: '٤' }], correctId: 'b' } });
before(async () => {
  db = await startTestDatabase();
  const owner = db.owner;
  // Synthetic test records exist only in a throwaway loopback PostgreSQL cluster.
  await owner.query(`INSERT INTO content.categories(id,slug,title) VALUES($1,'logic','منطق'),($2,'science','علوم')`, [id(1), id(2)]);
  await owner.query(`INSERT INTO content.game_types(id,engine_version,schema_version,title) VALUES('choice',1,1,'اختيار')`);
  await owner.query(`INSERT INTO content.games(id,game_type_id,category_id,slug,title,enabled) VALUES($1,'choice',$2,'choice','اختيار',true),($3,'choice',$2,'hidden','مخفي',false)`, [id(3), id(1), id(4)]);
  for (const n of [10,11,12,13,14,15]) {
    await owner.query('INSERT INTO content.puzzles(id,game_id) VALUES($1,$2)', [id(n), n === 15 ? id(4) : id(3)]);
    const published = n !== 12;
    await owner.query(`INSERT INTO content.puzzle_versions(puzzle_id,revision,schema_version,rules_version,difficulty,normalization,definition,definition_sha256,status) VALUES($1,1,1,1,1,'arabic_basic_v1',$2,repeat('a',64),$3)`, [id(n), { ...definition(n,n === 14), status: published ? 'published' : 'draft' }, published ? 'published' : 'draft']);
    if (published) await owner.query(`INSERT INTO content.publications(puzzle_id,revision,published_at,withdrawn_at) VALUES($1,1,now(),CASE WHEN $2 THEN now() ELSE NULL END)`, [id(n), n === 13]);
  }
  await owner.query(`INSERT INTO content.content_packs(id,slug,category_id,title) VALUES($1,'math',$2,'حزمة منطق'),($3,'draft-pack',$2,'مسودة')`, [id(20),id(1),id(21)]);
  for (const version of [1,2,3]) await owner.query(`INSERT INTO content.content_pack_versions(pack_id,version,schema_version,min_app_version_code,manifest,manifest_sha256,download_bytes,installed_bytes,object_key,status,published_at) VALUES($1,$2,1,$2,'{}',repeat('b',64),4096,8192,$3,$4,CASE WHEN $2<3 THEN now() ELSE NULL END)`, [id(20),version,'internal/private-pack/'+version,version < 3 ? 'published' : 'building']);
  await owner.query(`INSERT INTO content.content_pack_versions(pack_id,version,schema_version,min_app_version_code,manifest,manifest_sha256,download_bytes,installed_bytes,object_key,status) VALUES($1,1,1,1,'{}',repeat('b',64),1,1,'internal/draft','building')`, [id(21)]);
  await owner.query(`INSERT INTO content.daily_challenges(id,challenge_date,starts_at,ends_at,rules_version,private_definition) VALUES($1,current_date,now(),now()+interval '1 hour',1,'{"correctId":"TOP_SECRET_OFFICIAL"}')`, [id(80)]);
  app = buildApp(new CatalogRepository(db.catalog)); await app.ready();
});
after(async () => { if (app) await app.close(); if (db) await db.stop(); });

test('migrations are idempotent and runtime is restricted, without owner bypass', async () => {
  assert.deepEqual(await migrate(db.owner), []);
  const result = await db.catalog.query('SELECT current_user'); assert.equal(result.rows[0]?.current_user,'api_catalog');
  assert.equal((await app.inject('/health')).statusCode,200);
  assert.equal((await app.inject('/health/ready')).statusCode,200);
});
test('all lists read real PostgreSQL and game/category projection is safe', async () => {
  const games = await app.inject('/api/v1/games'); assert.equal(games.statusCode,200); assert.equal(games.json().items.length,1); assert.equal(games.json().items[0].id,id(3));
  const categories = await app.inject('/api/v1/categories'); assert.equal(categories.statusCode,200); assert.equal(categories.json().items.length,2);
});
test('puzzle listings hide drafts, withdrawn, fixture-only, and disabled games at SQL and API layers', async () => {
  const result = await app.inject('/api/v1/puzzles'); assert.equal(result.statusCode,200); assert.deepEqual(result.json().items.map((p: { puzzleId: string }) => p.puzzleId),[id(10),id(11)]);
  assert.ok(!result.body.includes('correctId')); assert.ok(!result.body.includes('definition_sha256'));
  const raw = await db.catalog.query('SELECT puzzle_id FROM content.puzzle_versions ORDER BY puzzle_id'); assert.equal(raw.rows.length,2);
  for (const n of [12,13,14,15]) assert.equal((await app.inject('/api/v1/puzzles/'+id(n))).statusCode,404);
});
test('detail returns validated Arabic offline practice definition and never competition bank', async () => {
  const result = await app.inject('/api/v1/puzzles/'+id(10)); assert.equal(result.statusCode,200); assert.equal(result.json().payload.correctId,'b'); assert.equal(result.json().fixtureOnly,false);
  assert.ok(result.body.includes('كم حاصل')); assert.ok(!result.body.includes('TOP_SECRET_OFFICIAL'));
  assert.equal((await app.inject('/api/v1/challenges/daily')).statusCode,404);
  await assert.rejects(db.catalog.query('SELECT private_definition FROM content.daily_challenges'), /permission denied/);
  await assert.rejects(db.catalog.query('SELECT * FROM identity.users'), /permission denied/);
  await assert.rejects(db.catalog.query('SELECT object_key FROM content.content_pack_versions'), /permission denied/);
  await assert.rejects(db.catalog.query('SELECT author_id FROM content.puzzle_versions'), /permission denied/);
});
test('keyset pages are bounded, complete, scoped and reject malformed/reused cursors', async () => {
  const first = await app.inject('/api/v1/puzzles?limit=1'); const page = first.json(); assert.equal(page.items.length,1); assert.equal(page.hasMore,true); assert.ok(page.nextCursor);
  const second = await app.inject('/api/v1/puzzles?limit=1&cursor='+page.nextCursor); assert.equal(second.json().items[0].puzzleId,id(11)); assert.equal(second.json().nextCursor,null);
  assert.equal((await app.inject('/api/v1/games?cursor='+page.nextCursor)).statusCode,400);
  assert.equal((await app.inject('/api/v1/puzzles?difficulty=2&cursor='+page.nextCursor)).statusCode,400);
  assert.equal((await app.inject('/api/v1/puzzles?cursor=not-json')).statusCode,400);
});
test('filters are parameterized and pagination, IDs, enums and extra fields validate strictly', async () => {
  assert.equal((await app.inject('/api/v1/puzzles?difficulty=2')).json().items.length,0);
  assert.equal((await app.inject('/api/v1/puzzles?gameId='+id(3))).json().items.length,2);
  assert.equal((await app.inject('/api/v1/puzzles?categoryId='+id(2))).json().items.length,0);
  for (const query of ['limit=0','limit=101','limit=1.5','limit=banana','unknown=1','type=code','difficulty=6','gameId=%27%20OR%201=1--','limit=1&limit=2']) assert.equal((await app.inject('/api/v1/puzzles?'+query)).statusCode,400,query);
  assert.equal((await app.inject('/api/v1/puzzles/not-uuid')).statusCode,400);
});
test('pack latest compatible version and version pagination expose metadata only', async () => {
  const all = await app.inject('/api/v1/content-packs'); assert.equal(all.statusCode,200); assert.equal(all.json().items.length,1); assert.equal(all.json().items[0].version,2);
  const compatible = await app.inject('/api/v1/content-packs?appVersionCode=1'); assert.equal(compatible.json().items[0].version,1);
  const detail = await app.inject('/api/v1/content-packs/'+id(20)); assert.equal(detail.statusCode,200); assert.ok(!detail.body.includes('objectKey')); assert.ok(!detail.body.includes('internal/')); assert.ok(!detail.body.includes('manifest"'));
  const versions = await app.inject('/api/v1/content-packs/'+id(20)+'/versions?limit=1'); assert.equal(versions.json().items[0].version,2);
  const next = await app.inject('/api/v1/content-packs/'+id(20)+'/versions?cursor='+versions.json().nextCursor); assert.equal(next.json().items[0].version,1);
  assert.equal((await app.inject('/api/v1/content-packs/'+id(21))).statusCode,404); assert.equal((await app.inject('/api/v1/content-packs/'+id(21)+'/versions')).statusCode,404);
});
test('ETag supports unchanged responses, weak comparison, cache headers and request IDs', async () => {
  const first = await app.inject('/api/v1/games'); const tag = first.headers.etag; assert.equal(typeof tag,'string'); assert.equal(first.headers['cache-control'],'public, max-age=300');
  const second = await app.inject({ url: '/api/v1/games', headers: { 'if-none-match': 'W/'+tag } }); assert.equal(second.statusCode,304); assert.equal(second.body,'');
  assert.match(String(first.headers['x-request-id']),/^[0-9a-f-]{36}$/);
});
test('unknown, unauthenticated future writes and SQL errors reveal no internals', async () => {
  const missing = await app.inject('/api/v1/puzzles/'+id(99)); assert.equal(missing.statusCode,404); assert.equal(missing.json().code,'NOT_FOUND'); assert.equal(missing.headers['cache-control'],'no-store');
  assert.equal((await app.inject({ method: 'POST', url: '/api/v1/sync/operations', payload: {} })).statusCode,404);
  const broken = buildApp(new CatalogRepository({ query: async () => { throw new Error('postgres://password@internal SQL SELECT secret'); } } as unknown as ConstructorParameters<typeof CatalogRepository>[0]));
  try { const result = await broken.inject('/api/v1/games'); assert.equal(result.statusCode,503); assert.ok(!result.body.includes('password')); assert.ok(!result.body.includes('SELECT')); assert.equal(result.json().code,'TEMPORARY_UNAVAILABLE'); } finally { await broken.close(); }
});
test('runtime cannot write or inspect private game data', async () => {
  await assert.rejects(db.catalog.query(`UPDATE content.games SET title='changed'`), /read-only|permission denied/);
  await assert.rejects(db.catalog.query('SELECT * FROM play.challenge_attempts'), /permission denied/);
  const policy = await db.owner.query(`SELECT rolcanlogin,rolsuper,rolbypassrls FROM pg_roles WHERE rolname='api_catalog'`); assert.deepEqual(policy.rows[0],{ rolcanlogin:false,rolsuper:false,rolbypassrls:false });
});
test('published immutability preserves content while allowing author erasure', async () => {
  const author = id(95); const puzzle = id(96);
  await db.owner.query(`INSERT INTO identity.users(id,oidc_issuer,oidc_subject) VALUES($1,'test-local','author')`, [author]);
  await db.owner.query('INSERT INTO content.puzzles(id,game_id,created_by) VALUES($1,$2,$3)',[puzzle,id(3),author]);
  await db.owner.query(`INSERT INTO content.puzzle_versions(puzzle_id,revision,schema_version,rules_version,difficulty,normalization,definition,definition_sha256,status,author_id) VALUES($1,1,1,1,1,'arabic_basic_v1',$2,repeat('c',64),'published',$3)`,[puzzle,{...definition(96),puzzleId:puzzle},author]);
  await db.owner.query('DELETE FROM identity.users WHERE id=$1',[author]);
  assert.equal((await db.owner.query('SELECT author_id FROM content.puzzle_versions WHERE puzzle_id=$1',[puzzle])).rows[0]?.author_id,null);
  await assert.rejects(db.owner.query('UPDATE content.puzzle_versions SET difficulty=2 WHERE puzzle_id=$1',[puzzle]),/immutable/);
  await assert.rejects(db.owner.query(`UPDATE content.content_pack_versions SET download_bytes=1 WHERE pack_id=$1 AND version=1`,[id(20)]),/immutable/);
});
test('development seeds remain unapproved drafts and can be applied twice', async () => {
  const seed = await readFile(new URL('../../../database/seeds/001_development.sql',import.meta.url),'utf8'); await db.owner.query(seed); await db.owner.query(seed);
  const result = await db.owner.query(`SELECT count(*)::integer AS count FROM content.puzzle_versions WHERE definition->'fixtureOnly'='true'::jsonb AND status='draft'`); assert.equal(result.rows[0]?.count,9);
  assert.equal((await app.inject('/api/v1/puzzles')).json().items.length,2);
});
