import type { QueryResult, QueryResultRow } from 'pg';
import type { Category, ContentPack, Game, PageQuery, PackQuery, PuzzleDefinition, PuzzleQuery, PuzzleSummary } from '../../../packages/contracts/catalog.js';
import { decodeCursor, pageOf, scopeFor } from './pagination.js';

type Db = { query<R extends QueryResultRow>(sql: string, parameters: unknown[]): Promise<QueryResult<R>> };
const packColumns = `p.id,p.slug,p.title,p.category_id AS "categoryId",p.is_core AS "isCore",v.version,v.min_app_version_code AS "minAppVersionCode",v.download_bytes::double precision AS "downloadBytes",v.installed_bytes::double precision AS "installedBytes",v.manifest_sha256 AS "manifestSha256",to_char(v.published_at AT TIME ZONE 'UTC','YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') AS "publishedAt"`;
const latestPack = `FROM content.content_packs p JOIN LATERAL (SELECT version,min_app_version_code,download_bytes,installed_bytes,manifest_sha256,published_at FROM content.content_pack_versions WHERE pack_id=p.id AND status='published' AND published_at IS NOT NULL AND ($2::integer IS NULL OR min_app_version_code<=$2) ORDER BY version DESC LIMIT 1) v ON true WHERE p.withdrawn_at IS NULL`;
const puzzleColumns = `p.id AS "puzzleId",v.revision,p.game_id AS "gameId",g.category_id AS "categoryId",g.game_type_id AS type,v.difficulty,v.definition->>'title' AS title,v.rules_version AS "rulesVersion",v.normalization`;
const publishedPuzzle = `FROM content.puzzles p JOIN content.games g ON g.id=p.game_id AND g.enabled JOIN LATERAL (SELECT revision,difficulty,definition,rules_version,normalization FROM content.puzzle_versions v WHERE v.puzzle_id=p.id AND v.status='published' AND v.definition->'fixtureOnly'='false'::jsonb AND EXISTS(SELECT 1 FROM content.publications pub WHERE pub.puzzle_id=v.puzzle_id AND pub.revision=v.revision AND pub.withdrawn_at IS NULL) ORDER BY v.revision DESC LIMIT 1) v ON true`;

export class CatalogRepository {
  constructor(private readonly db: Db) {}
  private async rows<T extends QueryResultRow>(sql: string, parameters: unknown[]): Promise<T[]> { return (await this.db.query<T>(sql, parameters)).rows; }
  async ready(): Promise<boolean> {
    const [row] = await this.rows<{ safe: boolean }>(`SELECT NOT r.rolsuper AND NOT r.rolbypassrls AND current_user='api_catalog' AS safe FROM pg_roles r WHERE rolname=current_user`, []);
    return row?.safe === true;
  }
  async categories(query: PageQuery) {
    const scope = scopeFor('categories', {}); const after = decodeCursor(query.cursor, scope); const limit = query.limit ?? 20;
    const rows = await this.rows<Category>(`SELECT id,parent_id AS "parentId",slug,title,sort_order AS "sortOrder" FROM content.categories WHERE ($1::uuid IS NULL OR id>$1) ORDER BY id LIMIT $2`, [after, limit + 1]);
    return pageOf(rows, limit, scope, row => row.id);
  }
  async games(query: PageQuery) {
    const scope = scopeFor('games', {}); const after = decodeCursor(query.cursor, scope); const limit = query.limit ?? 20;
    const rows = await this.rows<Game>(`SELECT id,game_type_id AS type,category_id AS "categoryId",slug,title,enabled FROM content.games WHERE enabled AND ($1::uuid IS NULL OR id>$1) ORDER BY id LIMIT $2`, [after, limit + 1]);
    return pageOf(rows, limit, scope, row => row.id);
  }
  async puzzles(query: PuzzleQuery) {
    const filters = { gameId: query.gameId, categoryId: query.categoryId, type: query.type, difficulty: query.difficulty };
    const scope = scopeFor('puzzles', filters); const after = decodeCursor(query.cursor, scope); const limit = query.limit ?? 20;
    const rows = await this.rows<PuzzleSummary>(`SELECT ${puzzleColumns} ${publishedPuzzle} WHERE ($1::uuid IS NULL OR p.id>$1) AND ($2::uuid IS NULL OR p.game_id=$2) AND ($3::uuid IS NULL OR g.category_id=$3) AND ($4::text IS NULL OR g.game_type_id=$4) AND ($5::integer IS NULL OR v.difficulty=$5) ORDER BY p.id LIMIT $6`, [after, query.gameId ?? null, query.categoryId ?? null, query.type ?? null, query.difficulty ?? null, limit + 1]);
    return pageOf(rows, limit, scope, row => row.puzzleId);
  }
  async puzzle(id: string): Promise<PuzzleDefinition | null> {
    const [row] = await this.rows<{ definition: PuzzleDefinition }>(`SELECT v.definition ${publishedPuzzle} WHERE p.id=$1::uuid`, [id]);
    return row?.definition ?? null;
  }
  async packs(query: PackQuery) {
    const filters = { categoryId: query.categoryId, appVersionCode: query.appVersionCode };
    const scope = scopeFor('packs', filters); const after = decodeCursor(query.cursor, scope); const limit = query.limit ?? 20;
    const rows = await this.rows<ContentPack>(`SELECT ${packColumns} ${latestPack} AND ($1::uuid IS NULL OR p.id>$1) AND ($3::uuid IS NULL OR p.category_id=$3) ORDER BY p.id LIMIT $4`, [after, query.appVersionCode ?? null, query.categoryId ?? null, limit + 1]);
    return pageOf(rows, limit, scope, row => row.id);
  }
  async pack(id: string): Promise<ContentPack | null> {
    const [row] = await this.rows<ContentPack>(`SELECT ${packColumns} ${latestPack} AND p.id=$1::uuid`, [id, null]);
    return row ?? null;
  }
  async packVersions(id: string, query: PageQuery) {
    const scope = scopeFor('pack-versions', { id }); const after = decodeCursor(query.cursor, scope, 'version'); const limit = query.limit ?? 20;
    const rows = await this.rows<ContentPack>(`SELECT ${packColumns} FROM content.content_packs p JOIN content.content_pack_versions v ON v.pack_id=p.id WHERE p.id=$1::uuid AND p.withdrawn_at IS NULL AND v.status='published' AND v.published_at IS NOT NULL AND ($2::integer IS NULL OR v.version<$2) ORDER BY v.version DESC LIMIT $3`, [id, after, limit + 1]);
    return pageOf(rows, limit, scope, row => row.version);
  }
}
