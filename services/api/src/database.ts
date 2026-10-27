import type { Pool, QueryResult, QueryResultRow } from 'pg';

// Every borrowed connection sets its role inside the transaction. This works
// with transaction pooling and cannot bleed authorization into the next request.
export class CatalogDatabase {
  constructor(private readonly pool: Pool) {}
  async query<R extends QueryResultRow>(sql: string, parameters: unknown[] = []): Promise<QueryResult<R>> {
    const client = await this.pool.connect();
    try {
      await client.query('BEGIN READ ONLY');
      await client.query('SET LOCAL ROLE api_catalog');
      await client.query("SET LOCAL statement_timeout='5s'");
      const result = await client.query<R>(sql, parameters);
      await client.query('COMMIT');
      return result;
    } catch (error) { await client.query('ROLLBACK'); throw error; }
    finally { client.release(); }
  }
}
