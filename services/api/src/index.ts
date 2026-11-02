import { Pool } from 'pg';
import { buildApp } from './app.js';
import { databaseConfig, listenConfig, contentConfig } from './config.js';
import { CatalogRepository } from './repository.js';
import { CatalogDatabase } from './database.js';

const connection = process.env.DATABASE_URL;
if (!connection) throw new Error('DATABASE_URL is required; use a dedicated catalog login');
const pool = new Pool(databaseConfig(connection, process.env.NODE_ENV === 'production'));
const repository = new CatalogRepository(new CatalogDatabase(pool));
const app = buildApp(repository, { logger: true, ...await contentConfig(process.env) });
app.addHook('onClose', async () => { await pool.end(); });
try {
  if (!await repository.ready()) throw new Error('Catalog role is not safe');
  await app.listen(listenConfig(process.env));
  for (const signal of ['SIGTERM', 'SIGINT'] as const) process.once(signal, () => { void app.close(); });
} catch {
  app.log.error('Catalog startup failed; verify database connectivity and restricted role configuration');
  await app.close();
  process.exitCode = 1;
}
