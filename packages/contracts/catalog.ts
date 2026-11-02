import puzzleDefinition from './puzzle.schema.json' with { type: 'json' };

export const gameTypes = ['crossword', 'wordsearch', 'anagram', 'image_guess', 'choice', 'matching', 'order', 'text_answer', 'sequence'] as const;
export type GameType = typeof gameTypes[number];
export interface PageQuery { limit?: number; cursor?: string }
export interface PuzzleQuery extends PageQuery { gameId?: string; categoryId?: string; type?: GameType; difficulty?: number }
export interface PackQuery extends PageQuery { categoryId?: string; appVersionCode?: number }
export interface Page<T> { items: T[]; nextCursor: string | null; hasMore: boolean }
export interface Category { id: string; parentId: string | null; slug: string; title: string; sortOrder: number }
export interface Game { id: string; type: GameType; categoryId: string; slug: string; title: string; enabled: boolean }
export interface PuzzleSummary { puzzleId: string; revision: number; gameId: string; categoryId: string; type: GameType; difficulty: number; title: string; rulesVersion: number; normalization: string }
export interface ContentPack { id: string; slug: string; title: string; categoryId: string; isCore: boolean; version: number; minAppVersionCode: number; downloadBytes: number; installedBytes: number; manifestSha256: string; publishedAt: string }
export type PuzzleDefinition = Record<string, unknown>;
export interface Problem { type: string; title: string; status: number; code: string; requestId: string }

export const uuid = { type: 'string', format: 'uuid', pattern: '^[0-9a-f-]{36}$' } as const;
const text = { type: 'string', minLength: 1, maxLength: 2000 } as const;
const integer = { type: 'integer', minimum: 1, maximum: 2147483647 } as const;
const safeBytes = { type: 'integer', minimum: 1, maximum: Number.MAX_SAFE_INTEGER } as const;
const nullable = (schema: object) => ({ anyOf: [schema, { type: 'null' }] });
const object = (properties: Record<string, unknown>) => ({ type: 'object', additionalProperties: false, properties, required: Object.keys(properties) });
const pageQueryProperties = { limit: { type: 'integer', minimum: 1, maximum: 100, default: 20 }, cursor: { type: 'string', minLength: 1, maxLength: 1024 } };
export const pageQuerySchema = { type: 'object', additionalProperties: false, properties: pageQueryProperties };
export const puzzleQuerySchema = { ...pageQuerySchema, properties: { ...pageQueryProperties, gameId: uuid, categoryId: uuid, type: { type: 'string', enum: gameTypes }, difficulty: { type: 'integer', minimum: 1, maximum: 5 } } };
export const packQuerySchema = { ...pageQuerySchema, properties: { ...pageQueryProperties, categoryId: uuid, appVersionCode: integer } };
export const idParamsSchema = object({ id: uuid });
export const categorySchema = object({ id: uuid, parentId: nullable(uuid), slug: text, title: text, sortOrder: { type: 'integer' } });
export const gameSchema = object({ id: uuid, type: { type: 'string', enum: gameTypes }, categoryId: uuid, slug: text, title: text, enabled: { const: true } });
export const puzzleSummarySchema = object({ puzzleId: uuid, revision: integer, gameId: uuid, categoryId: uuid, type: { type: 'string', enum: gameTypes }, difficulty: { type: 'integer', minimum: 1, maximum: 5 }, title: text, rulesVersion: integer, normalization: { type: 'string', enum: ['exact_v1', 'arabic_basic_v1'] } });
export const packSchema = object({ id: uuid, slug: text, title: text, categoryId: uuid, isCore: { type: 'boolean' }, version: integer, minAppVersionCode: integer, downloadBytes: safeBytes, installedBytes: safeBytes, manifestSha256: { type: 'string', pattern: '^[a-f0-9]{64}$' }, publishedAt: { type: 'string', format: 'date-time' } });
export const pageSchema = (items: object) => object({ items: { type: 'array', items, maxItems: 100 }, nextCursor: nullable({ type: 'string', maxLength: 1024 }), hasMore: { type: 'boolean' } });
// Draft 2020-12 input is compiled by the API's Ajv2020 instance.
export const puzzleDefinitionSchema = { ...puzzleDefinition, $id: 'lughzdan-public-practice-puzzle-v1' };
export const problemSchema = object({ type: text, title: text, status: { type: 'integer', minimum: 400, maximum: 599 }, code: text, requestId: { type: 'string', format: 'uuid' } });
