BEGIN;
GRANT SELECT(manifest) ON content.content_pack_versions TO api_catalog;
GRANT SELECT ON content.pack_dependencies TO api_catalog;
CREATE POLICY catalog_pack_dependencies ON content.pack_dependencies FOR SELECT TO api_catalog USING (EXISTS(SELECT 1 FROM content.content_pack_versions v WHERE v.pack_id=pack_dependencies.pack_id AND v.version=pack_dependencies.version));
DO $$ BEGIN IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='api_sync') THEN CREATE ROLE api_sync NOLOGIN; END IF; END $$;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname='api_sync' AND (rolsuper OR rolbypassrls OR rolreplication)) THEN RAISE EXCEPTION 'Unsafe api_sync role'; END IF; END $$;
ALTER ROLE api_sync NOLOGIN NOCREATEDB NOCREATEROLE;
GRANT USAGE ON SCHEMA identity,play,ops,content TO api_sync;
GRANT SELECT ON identity.devices TO api_sync;
CREATE POLICY sync_devices ON identity.devices FOR SELECT TO api_sync USING(user_id=current_setting('app.user_id',true)::uuid AND revoked_at IS NULL);
GRANT SELECT ON content.puzzle_versions,content.achievements TO api_sync;
GRANT SELECT ON content.publications TO api_sync;
CREATE POLICY sync_publications ON content.publications FOR SELECT TO api_sync USING(withdrawn_at IS NULL);
CREATE POLICY sync_puzzles ON content.puzzle_versions FOR SELECT TO api_sync USING(status='published' AND definition->'fixtureOnly'='false'::jsonb AND EXISTS(SELECT 1 FROM content.publications p WHERE p.puzzle_id=puzzle_versions.puzzle_id AND p.revision=puzzle_versions.revision AND p.withdrawn_at IS NULL));
CREATE POLICY sync_achievement_rules ON content.achievements FOR SELECT TO api_sync USING(true);
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['play.player_sessions','play.player_progress','play.preferences','play.saved_words','play.player_achievements','ops.user_sync_clock','ops.sync_operations','ops.sync_changes','ops.sync_snapshots'] LOOP
 EXECUTE format('GRANT SELECT,INSERT,UPDATE ON %s TO api_sync',t);
 EXECUTE format('CREATE POLICY sync_owner ON %s TO api_sync USING(user_id=current_setting(''app.user_id'',true)::uuid) WITH CHECK(user_id=current_setting(''app.user_id'',true)::uuid)',t);
 END LOOP;
END $$;
COMMIT;
