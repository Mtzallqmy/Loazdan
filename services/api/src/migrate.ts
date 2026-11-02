import { readFile } from 'node:fs/promises';
import { Client } from 'pg';
import { databaseConfig } from './config.js';
import { migrate } from './migrations.js';

const url = process.env.MIGRATION_DATABASE_URL;
if (!url) throw new Error('MIGRATION_DATABASE_URL is required; never use migration credentials in API runtime');
const config = databaseConfig(url, process.env.NODE_ENV === 'production');
const client = new Client({ ...config, options: '-c statement_timeout=60000' });
try {
  await client.connect();
  const applied = await migrate(client, process.argv.includes('--baseline-existing'));
  console.log(JSON.stringify({ applied }));
  if (process.argv.includes('--seed-development')) {
    if (process.env.ALLOW_DEVELOPMENT_SEED !== '1' || process.env.NODE_ENV === 'production') throw new Error('Development seed requires ALLOW_DEVELOPMENT_SEED=1 outside production');
    await client.query(await readFile(new URL('../../../database/seeds/001_development.sql', import.meta.url), 'utf8'));
    console.log('Development draft fixtures seeded; no content published');
  }
} catch {
  console.error('Migration failed. Check role permissions, migration checksums, and reviewed baseline prerequisites; connection details are never printed.');
  process.exitCode = 1;
} finally { await client.end(); }
