import { mkdtemp, chown, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { createRequire } from 'node:module';
import { execFile } from 'node:child_process';
import { promisify } from 'node:util';
import { createServer } from 'node:net';
import { Client, Pool } from 'pg';
import { migrate } from '../src/migrations.js';
import { CatalogDatabase } from '../src/database.js';
import { PGlite } from '@electric-sql/pglite';
import { PGLiteSocketServer } from '@electric-sql/pglite-socket';

const execute = promisify(execFile);
async function availablePort(): Promise<number> {
  const server = createServer(); await new Promise<void>(resolve => server.listen(0, '127.0.0.1', resolve));
  const address = server.address();
  if (!address || typeof address === 'string') throw new Error('No TCP port available');
  await new Promise<void>((resolve, reject) => server.close(error => error ? reject(error) : resolve()));
  return address.port;
}
export async function startTestDatabase() {
  if (process.env.TEST_DATABASE_ENGINE === 'pglite') return startWasmDatabase();
  const root = await mkdtemp(join(tmpdir(), 'lughzdan-catalog-test-'));
  const port = await availablePort();
  const packageFile = createRequire(import.meta.url).resolve(`@embedded-postgres/${process.platform}-${process.arch}`);
  const binaries = join(dirname(packageFile), '../native/bin');
  const asNobody = process.platform === 'linux' && process.getuid?.() === 0;
  if (asNobody) {
    const uid = Number((await execute('id', ['-u', 'nobody'])).stdout.trim());
    const gid = Number((await execute('id', ['-g', 'nobody'])).stdout.trim());
    await chown(root, uid, gid);
  }
  const run = async (binary: string, args: string[]) => {
    const executable = join(binaries, binary);
    const command = asNobody ? 'runuser' : executable;
    const arguments_ = asNobody ? ['-u', 'nobody', '--', executable, ...args] : args;
    return execute(command, arguments_, { timeout: 30_000, env: { ...process.env, LANG: 'C', LC_ALL: 'C' } });
  };
  const data = join(root, 'data');
  let started = false;
  const owner = new Client({ host: '127.0.0.1', port, database: 'postgres', user: 'postgres' });
  const pool = new Pool({ host: '127.0.0.1', port, database: 'postgres', user: 'postgres', max: 3 });
  const catalog = new CatalogDatabase(pool);
  const stop = async () => {
    await pool.end(); await owner.end().catch(() => undefined);
    if (started) await run('pg_ctl', ['-D', data, 'stop', '-m', 'immediate', '-w']);
    await rm(root, { recursive: true, force: true });
  };
  try {
    await run('initdb', ['-D', data, '-U', 'postgres', '--auth=trust', '--encoding=UTF8', '--locale=C']);
    await run('pg_ctl', ['-D', data, '-l', join(root, 'server.log'), '-o', `-h 127.0.0.1 -p ${port} -k ${root}`, '-w', 'start']); started = true;
    await owner.connect(); await migrate(owner);
    return { owner, catalog, pool, stop, root };
  } catch (error) { await stop(); throw error; }
}

async function startWasmDatabase() {
  // Explicit alternative for containers whose uid map cannot run native initdb.
  // PostgreSQL SQL/RLS and the pg wire path run locally; process/concurrency
  // behavior still requires the native suite or isolated hosted PostgreSQL.
  const engine = await PGlite.create();
  const port = await availablePort();
  const server = new PGLiteSocketServer({ db: engine, port, host: '127.0.0.1', maxConnections: 4 });
  await server.start();
  const owner = new Client({ host: '127.0.0.1', port, database: 'postgres', user: 'postgres' });
  const pool = new Pool({ host: '127.0.0.1', port, database: 'postgres', user: 'postgres', max: 1 });
  const catalog = new CatalogDatabase(pool);
  const stop = async () => { await pool.end(); await owner.end(); await server.stop(); await engine.close(); };
  try { await owner.connect(); await migrate(owner); return { owner, catalog, pool, stop, root: 'memory://pglite-test' }; }
  catch (error) { await stop(); throw error; }
}
