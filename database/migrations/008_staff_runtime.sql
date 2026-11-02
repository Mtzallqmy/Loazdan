BEGIN;
DO $$ BEGIN IF NOT EXISTS(SELECT 1 FROM pg_roles WHERE rolname='api_staff') THEN CREATE ROLE api_staff NOLOGIN; END IF; END $$;
DO $$ BEGIN IF EXISTS(SELECT 1 FROM pg_roles WHERE rolname='api_staff' AND (rolsuper OR rolbypassrls OR rolreplication)) THEN RAISE EXCEPTION 'Unsafe api_staff role'; END IF; END $$;
ALTER ROLE api_staff NOLOGIN NOCREATEDB NOCREATEROLE;

GRANT USAGE ON SCHEMA identity, content, play, ops, editorial TO api_staff;
GRANT SELECT, INSERT, UPDATE ON identity.users TO api_staff;
GRANT SELECT, INSERT, UPDATE, DELETE ON identity.devices TO api_staff;
GRANT SELECT, INSERT, UPDATE, DELETE ON identity.staff_roles TO api_staff;

GRANT SELECT, INSERT, UPDATE ON content.categories TO api_staff;
GRANT SELECT, INSERT, UPDATE ON content.games TO api_staff;
GRANT SELECT, INSERT, UPDATE ON content.game_types TO api_staff;
GRANT SELECT, INSERT, UPDATE ON content.puzzles TO api_staff;
GRANT SELECT, INSERT, UPDATE ON content.puzzle_versions TO api_staff;
GRANT SELECT, INSERT, UPDATE ON content.content_packs TO api_staff;
GRANT SELECT, INSERT, UPDATE ON content.content_pack_versions TO api_staff;
GRANT SELECT, INSERT, UPDATE ON content.publications TO api_staff;

GRANT SELECT, INSERT, UPDATE, DELETE ON editorial.editorial_reviews TO api_staff;
GRANT SELECT, INSERT ON editorial.audit_logs TO api_staff;
GRANT SELECT, INSERT, UPDATE ON editorial.publication_jobs TO api_staff;

GRANT SELECT, INSERT, UPDATE ON content.daily_challenges TO api_staff;
GRANT SELECT, INSERT ON play.challenge_attempts TO api_staff;

CREATE POLICY staff_puzzle_versions ON content.puzzle_versions FOR ALL TO api_staff USING (true) WITH CHECK (true);
CREATE POLICY staff_puzzles ON content.puzzles FOR ALL TO api_staff USING (true) WITH CHECK (true);
CREATE POLICY staff_editorial_reviews ON editorial.editorial_reviews FOR ALL TO api_staff USING (true) WITH CHECK (true);
CREATE POLICY staff_audit_logs ON editorial.audit_logs FOR ALL TO api_staff USING (true) WITH CHECK (true);
CREATE POLICY staff_publication_jobs ON editorial.publication_jobs FOR ALL TO api_staff USING (true) WITH CHECK (true);
CREATE POLICY staff_daily_challenges ON content.daily_challenges FOR ALL TO api_staff USING (true) WITH CHECK (true);
CREATE POLICY staff_challenge_attempts ON play.challenge_attempts FOR ALL TO api_staff USING (true) WITH CHECK (true);

COMMIT;
