import { registerContentRoutes, type ContentOptions } from './routes.js';
import { randomUUID, createHash } from 'node:crypto';
import { createRequire } from 'node:module';
import Fastify from 'fastify';
import rateLimit from '@fastify/rate-limit';
import { Ajv2020, type AnySchema } from 'ajv/dist/2020.js';
import type { FormatsPlugin } from 'ajv-formats';
import { categorySchema, gameSchema, idParamsSchema, packQuerySchema, packSchema, pageQuerySchema, pageSchema, problemSchema, puzzleDefinitionSchema, puzzleQuerySchema, puzzleSummarySchema } from '../../../packages/contracts/catalog.js';
import type { PageQuery, PackQuery, PuzzleQuery } from '../../../packages/contracts/catalog.js';
import { ApiError } from './errors.js';
import type { CatalogRepository } from './repository.js';
const addFormats = createRequire(import.meta.url)('ajv-formats') as FormatsPlugin;

export function buildApp(repository: CatalogRepository, options: ContentOptions & { logger?: boolean } = {}) {
  const app = Fastify({ logger: options.logger ?? false, trustProxy: false, disableRequestLogging: true, bodyLimit: 1024 * 1024, requestTimeout: 10_000, connectionTimeout: 10_000, genReqId: () => randomUUID(), requestIdHeader: false });
  // Bounded per-process LRU. Multi-instance deployments require gateway limits.
  app.register(rateLimit, { global: true, max: 120, timeWindow: 60_000, cache: 5000, keyGenerator: request => request.ip });
  const requestAjv = new Ajv2020({ coerceTypes: true, useDefaults: true, removeAdditional: false, allErrors: false, strict: false });
  const responseAjv = new Ajv2020({ strict: false, allErrors: false });
  addFormats(requestAjv); addFormats(responseAjv);
  app.setValidatorCompiler(({ schema }) => requestAjv.compile(schema as AnySchema));
  app.setSerializerCompiler(({ schema }) => {
    const validate = responseAjv.compile(schema as AnySchema);
    return data => {
      if (!validate(data)) throw new ApiError(503, 'TEMPORARY_UNAVAILABLE', 'Service temporarily unavailable');
      return JSON.stringify(data);
    };
  });
  app.addHook('onSend', async (_request, reply, payload) => {
    reply.header('x-content-type-options', 'nosniff').header('x-request-id', _request.id);
    return payload;
  });
  app.setErrorHandler((error, request, reply) => {
    const validation = typeof error === 'object' && error !== null && 'validation' in error;
    const tooLarge = typeof error === 'object' && error !== null && 'statusCode' in error && error.statusCode === 413;
    const rateLimited = typeof error === 'object' && error !== null && 'statusCode' in error && error.statusCode === 429;
    const status = rateLimited ? 429 : validation ? 400 : tooLarge ? 413 : error instanceof ApiError ? error.status : 503;
    const code = rateLimited ? 'RATE_LIMITED' : validation ? 'VALIDATION_FAILED' : tooLarge ? 'BODY_TOO_LARGE' : error instanceof ApiError ? error.code : 'TEMPORARY_UNAVAILABLE';
    const title = status === 429 ? 'Too many requests' : status === 400 ? 'Invalid request' : status === 404 ? 'Not found' : 'Service temporarily unavailable';
    // Do not serialize SQL, connection strings, database errors, or user inputs.
    if (status >= 500) request.log.error({ requestId: request.id, code }, 'Catalog request failed');
    reply.status(status).type('application/problem+json').header('cache-control', 'no-store').send({ type: 'urn:lughzdan:problem:' + code.toLowerCase(), title, status, code, requestId: request.id });
  });
  app.setNotFoundHandler((request, reply) => reply.status(404).header('cache-control', 'no-store').send({ type: 'urn:lughzdan:problem:not_found', title: 'Not found', status: 404, code: 'NOT_FOUND', requestId: request.id }));
  const response = (schema: object) => ({ 200: schema, 400: problemSchema, 404: problemSchema, 429: problemSchema, 503: problemSchema });
  const sendCatalog = (request: { headers: { 'if-none-match'?: string | undefined } }, reply: import('fastify').FastifyReply, data: unknown) => {
    const etag = '"' + createHash('sha256').update(JSON.stringify(data)).digest('hex') + '"';
    reply.header('etag', etag).header('cache-control', 'public, max-age=300');
    const candidates = request.headers['if-none-match']?.split(',').map(value => value.trim().replace(/^W\//, ''));
    if (candidates?.includes(etag) || candidates?.includes('*')) return reply.status(304).send();
    return reply.send(data);
  };
  app.after(() => {
  registerContentRoutes(app, repository, options);
  app.get('/health', { schema: { response: { 200: { type: 'object', additionalProperties: false, properties: { status: { const: 'ok' } }, required: ['status'] } } } }, async () => ({ status: 'ok' }));
  app.get('/health/ready', async () => {
    if (!await repository.ready()) throw new ApiError(503, 'TEMPORARY_UNAVAILABLE', 'Service temporarily unavailable');
    return { status: 'ready' };
  });
  app.get<{ Querystring: PageQuery }>('/api/v1/categories', { schema: { querystring: pageQuerySchema, response: response(pageSchema(categorySchema)) } }, async (request, reply) => sendCatalog(request, reply, await repository.categories(request.query)));
  app.get<{ Querystring: PageQuery }>('/api/v1/games', { schema: { querystring: pageQuerySchema, response: response(pageSchema(gameSchema)) } }, async (request, reply) => sendCatalog(request, reply, await repository.games(request.query)));
  app.get<{ Querystring: PuzzleQuery }>('/api/v1/puzzles', { schema: { querystring: puzzleQuerySchema, response: response(pageSchema(puzzleSummarySchema)) } }, async (request, reply) => sendCatalog(request, reply, await repository.puzzles(request.query)));
  app.get<{ Params: { id: string } }>('/api/v1/puzzles/:id', { schema: { params: idParamsSchema, response: response(puzzleDefinitionSchema) } }, async (request, reply) => {
    const definition = await repository.puzzle(request.params.id);
    if (!definition) throw new ApiError(404, 'NOT_FOUND', 'Not found');
    return sendCatalog(request, reply, definition);
  });
  app.get<{ Querystring: PackQuery }>('/api/v1/content-packs', { schema: { querystring: packQuerySchema, response: response(pageSchema(packSchema)) } }, async (request, reply) => sendCatalog(request, reply, await repository.packs(request.query)));
  app.get<{ Params: { id: string } }>('/api/v1/content-packs/:id', { schema: { params: idParamsSchema, response: response(packSchema) } }, async (request, reply) => {
    const pack = await repository.pack(request.params.id);
    if (!pack) throw new ApiError(404, 'NOT_FOUND', 'Not found');
    return sendCatalog(request, reply, pack);
  });
  app.get<{ Params: { id: string }; Querystring: PageQuery }>('/api/v1/content-packs/:id/versions', { schema: { params: idParamsSchema, querystring: pageQuerySchema, response: response(pageSchema(packSchema)) } }, async (request, reply) => {
    if (!await repository.pack(request.params.id)) throw new ApiError(404, 'NOT_FOUND', 'Not found');
    return sendCatalog(request, reply, await repository.packVersions(request.params.id, request.query));
  });
  });
  return app;
}
