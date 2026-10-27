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
