import { createHash } from 'node:crypto';
import type { Page } from '../../../packages/contracts/catalog.js';
import { ApiError } from './errors.js';

const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/;
export function scopeFor(route: string, filters: Record<string, unknown>): string {
  const normalized = Object.fromEntries(Object.entries(filters).filter(([, value]) => value !== undefined).sort(([a], [b]) => a.localeCompare(b)));
  return createHash('sha256').update(JSON.stringify([route, normalized])).digest('hex');
}
export function decodeCursor(cursor: string | undefined, scope: string, mode: 'uuid' | 'version' = 'uuid'): string | number | null {
  if (!cursor) return null;
  try {
    if (!/^[A-Za-z0-9_-]+$/.test(cursor) || cursor.length > 1024) throw new Error();
    const value: unknown = JSON.parse(Buffer.from(cursor, 'base64url').toString('utf8'));
    if (!value || typeof value !== 'object' || Array.isArray(value)) throw new Error();
    const row = value as Record<string, unknown>;
    if (Object.keys(row).sort().join(',') !== 'key,scope,v' || row.v !== 1 || row.scope !== scope) throw new Error();
    if (mode === 'uuid' && typeof row.key === 'string' && uuidPattern.test(row.key)) return row.key;
    if (mode === 'version' && typeof row.key === 'number' && Number.isInteger(row.key) && row.key > 0 && row.key <= 2147483647) return row.key;
    throw new Error();
  } catch { throw new ApiError(400, 'INVALID_CURSOR', 'Invalid pagination cursor'); }
}
export function pageOf<T>(rows: T[], limit: number, scope: string, key: (row: T) => string | number): Page<T> {
  const items = rows.slice(0, limit);
  const last = items.at(-1);
  const hasMore = rows.length > limit;
  return { items, hasMore, nextCursor: hasMore && last ? Buffer.from(JSON.stringify({ v: 1, scope, key: key(last) })).toString('base64url') : null };
}
