# Stage04 handoff — acceptance gated

Available foundation: six pure Kotlin plugins and typed intents; SessionCodec v1/v2 with pinned puzzle and pack versions; Room2 explicit migration; retained content revisions; local completion receipts and deduplicated best-score XP; strict payload semantic validation; Compose renderers; metadata-only Fastify catalog and Neon runtime RLS.

Stage04 must implement the signed external manifests/chunks/journal and resumable transfer protocol from CONTENT_PACK_FORMAT/OFFLINE_FIRST without replacing the local engine or making play depend on HTTP. Require minAppVersionCode2/minEngineVersion2 for Stage03 optional variants (ADR010). Do not send current internal completion receipts as public sync requests; translate to the existing typed SYNC_PROTOCOL with authentication and idempotency. Official ranking remains server-verified.

External downloads, identity/sync and image games are not implemented in Stage03. Start Stage04 only after reviewing STAGE_03_REPORT and Astra review, clearing high-risk defects and accepting documented content/repository-admin gates. No permission to publish an official release is implied.
