BEGIN;
-- Group role only. A deployment operator provisions a separate LOGIN member.
DO $$ BEGIN
 IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname='api_catalog') THEN
   CREATE ROLE api_catalog NOLOGIN;
 END IF;
END $$;
-- Hosted Postgres owners cannot set superuser/replication/BYPASSRLS flags,
-- even to false. New roles default to false; reject unsafe preexisting roles.
DO $$ BEGIN
 IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='api_catalog'
            AND (rolsuper OR rolreplication OR rolbypassrls)) THEN
   RAISE EXCEPTION 'api_catalog must not have superuser, replication or BYPASSRLS';
 END IF;
END $$;
ALTER ROLE api_catalog NOLOGIN NOCREATEDB NOCREATEROLE;
REVOKE ALL ON SCHEMA identity,play,ops,editorial FROM api_catalog;
REVOKE ALL ON ALL TABLES IN SCHEMA identity,play,ops,editorial,content FROM api_catalog;
GRANT USAGE ON SCHEMA content TO api_catalog;

GRANT SELECT(id,parent_id,slug,title,sort_order) ON content.categories TO api_catalog;
GRANT SELECT(id,game_type_id,category_id,slug,title,enabled) ON content.games TO api_catalog;
GRANT SELECT(id,game_id,level_id) ON content.puzzles TO api_catalog;
GRANT SELECT(puzzle_id,revision,schema_version,rules_version,difficulty,normalization,definition,status) ON content.puzzle_versions TO api_catalog;
GRANT SELECT(puzzle_id,revision,published_at,withdrawn_at) ON content.publications TO api_catalog;
GRANT SELECT(id,slug,category_id,title,is_core,withdrawn_at) ON content.content_packs TO api_catalog;
GRANT SELECT(pack_id,version,min_app_version_code,manifest_sha256,download_bytes,installed_bytes,status,published_at) ON content.content_pack_versions TO api_catalog;

CREATE POLICY catalog_categories ON content.categories FOR SELECT TO api_catalog USING (true);
CREATE POLICY catalog_games ON content.games FOR SELECT TO api_catalog USING (enabled);
CREATE POLICY catalog_publications ON content.publications FOR SELECT TO api_catalog USING (withdrawn_at IS NULL);
CREATE POLICY catalog_puzzles ON content.puzzles FOR SELECT TO api_catalog USING (
 EXISTS(SELECT 1 FROM content.publications p WHERE p.puzzle_id=puzzles.id AND p.withdrawn_at IS NULL)
 AND EXISTS(SELECT 1 FROM content.games g WHERE g.id=puzzles.game_id AND g.enabled)
);
CREATE POLICY catalog_puzzle_versions ON content.puzzle_versions FOR SELECT TO api_catalog USING (
 status='published' AND definition->'fixtureOnly'='false'::jsonb AND definition->>'status'='published'
 AND EXISTS(SELECT 1 FROM content.publications p WHERE p.puzzle_id=puzzle_versions.puzzle_id AND p.revision=puzzle_versions.revision AND p.withdrawn_at IS NULL)
 AND EXISTS(SELECT 1 FROM content.puzzles p WHERE p.id=puzzle_versions.puzzle_id)
);
CREATE POLICY catalog_packs ON content.content_packs FOR SELECT TO api_catalog USING (withdrawn_at IS NULL);
CREATE POLICY catalog_pack_versions ON content.content_pack_versions FOR SELECT TO api_catalog USING (
 status='published' AND published_at IS NOT NULL
 AND EXISTS(SELECT 1 FROM content.content_packs p WHERE p.id=content_pack_versions.pack_id AND p.withdrawn_at IS NULL)
);

-- Keyset browsing begins with an indexed UUID; latest-version lookups use PKs.
CREATE INDEX IF NOT EXISTS catalog_published_puzzle_versions_idx ON content.puzzle_versions(puzzle_id,revision DESC) WHERE status='published';
CREATE INDEX IF NOT EXISTS catalog_published_pack_versions_idx ON content.content_pack_versions(pack_id,version DESC) WHERE status='published';
CREATE INDEX IF NOT EXISTS catalog_games_category_idx ON content.games(category_id,id) WHERE enabled;
COMMIT;
