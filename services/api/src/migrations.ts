import { createHash } from 'node:crypto';
import { readFile, readdir } from 'node:fs/promises';
import type { Client } from 'pg';

const directory = new URL('../../../database/migrations/', import.meta.url);
const stripTransaction = (sql: string) => sql.replace(/^(\s*(?:--[^\n]*\n\s*)*)BEGIN;\s*/i, '$1').replace(/\bCOMMIT;\s*$/i, '');
export async function migrate(client: Client, baselineExisting = false): Promise<string[]> {
  const applied: string[] = [];
  await client.query('SELECT pg_advisory_lock(637194251)');
  try {
    await client.query(`CREATE TABLE IF NOT EXISTS public.lughzdan_schema_migrations(name text PRIMARY KEY,sha256 char(64) NOT NULL,applied_at timestamptz NOT NULL DEFAULT now())`);
    const files = (await readdir(directory)).filter(name => /^\d{3}_[a-z_]+\.sql$/.test(name)).sort();
    for (const name of files) {
      const sql = await readFile(new URL(name, directory), 'utf8');
      const hash = createHash('sha256').update(sql).digest('hex');
      const { rows } = await client.query<{ sha256: string }>('SELECT sha256 FROM public.lughzdan_schema_migrations WHERE name=$1', [name]);
      if (rows[0]) { if (rows[0].sha256 !== hash) throw new Error('Applied migration checksum mismatch: ' + name); continue; }
      await client.query('BEGIN');
      try {
        if (baselineExisting && name === '001_initial.sql') {
          // Explicit adoption only; never silently skip CREATE statements.
          const tables = [...sql.matchAll(/CREATE TABLE ([a-z_]+\.[a-z_]+)/g)].map(match => match[1]);
          const { rows: existing } = await client.query<{ count: string }>(`SELECT count(*)::text AS count FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname||'.'||c.relname=ANY($1::text[]) AND c.relkind='r' AND c.relrowsecurity AND c.relforcerowsecurity`, [tables]);
          if (Number(existing[0]?.count) !== tables.length) throw new Error('Baseline requires all Stage01 tables with forced RLS');
          for (const column of ['submission_sha256','terminal_result','pinned_definition','pinned_rules_version','nonce_key_id']) {
            const result = await client.query('SELECT 1 FROM information_schema.columns WHERE table_schema=$1 AND table_name=$2 AND column_name=$3', ['play', 'challenge_attempts', column]);
            if (result.rowCount !== 1) throw new Error('Baseline is missing a reviewed Stage01 column');
          }
          const triggers = await client.query(`SELECT count(*)::integer AS count FROM pg_trigger WHERE tgname=ANY($1::text[]) AND NOT tgisinternal`, [['immutable_published_revision','immutable_pack','immutable_pack_items','immutable_pack_dependencies']]);
          if (triggers.rows[0]?.count !== 4) throw new Error('Baseline is missing reviewed immutability triggers');
        } else await client.query(stripTransaction(sql));
        await client.query('INSERT INTO public.lughzdan_schema_migrations(name,sha256) VALUES($1,$2)', [name, hash]);
        await client.query('COMMIT'); applied.push(name);
      } catch (error) { await client.query('ROLLBACK'); throw error; }
    }
    return applied;
  } finally { await client.query('SELECT pg_advisory_unlock(637194251)'); }
}
