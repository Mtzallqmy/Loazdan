BEGIN;
-- No new grants or policies: operator-only ingestion until a separately reviewed
-- deployment explicitly grants a role access. Existing Phase 1 sources are reused.
CREATE TABLE editorial.knowledge_reviewers (
 user_id uuid PRIMARY KEY REFERENCES identity.users(id),
 human_verified boolean NOT NULL DEFAULT false,
 active boolean NOT NULL DEFAULT false,
 roles text[] NOT NULL CHECK (roles <@ ARRAY['linguist','scholar','publisher']::text[])
);
CREATE TABLE content.knowledge_records (
 id uuid PRIMARY KEY,
 kind text NOT NULL CHECK(kind IN ('root','sense','relation','poetry','rhetoric','quran_verse','quran_meaning')),
 author_id uuid NOT NULL REFERENCES identity.users(id),
 lexical_root_id uuid REFERENCES content.lexical_roots(id),
 vocabulary_id uuid REFERENCES content.vocabulary(id),
 definition jsonb NOT NULL CHECK(jsonb_typeof(definition)='object'),
 -- Database digest includes the immutable reference/source and dependency snapshots.
 review_digest text NOT NULL CHECK(review_digest ~ '^[a-f0-9]{64}$'),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE content.knowledge_references (
 record_id uuid NOT NULL REFERENCES content.knowledge_records(id),
 reference_id uuid NOT NULL REFERENCES content.source_references(id),
 PRIMARY KEY(record_id,reference_id)
);
CREATE TABLE content.knowledge_links (
 record_id uuid NOT NULL REFERENCES content.knowledge_records(id),
 dependency_id uuid NOT NULL REFERENCES content.knowledge_records(id),
 PRIMARY KEY(record_id,dependency_id), CHECK(record_id<>dependency_id)
);
CREATE TABLE editorial.knowledge_reviews (
 record_id uuid NOT NULL REFERENCES content.knowledge_records(id),
 reviewer_id uuid NOT NULL REFERENCES editorial.knowledge_reviewers(user_id),
 role text NOT NULL CHECK(role IN ('linguist','scholar','publisher')),
 decision text NOT NULL CHECK(decision IN ('approved','rejected')),
 review_digest text NOT NULL,
 reviewed_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(record_id,reviewer_id,role)
);
CREATE TABLE content.knowledge_puzzle_refs (
 puzzle_id uuid NOT NULL,
 revision integer NOT NULL,
 record_id uuid NOT NULL REFERENCES content.knowledge_records(id),
 review_digest text NOT NULL,
 PRIMARY KEY(puzzle_id,revision,record_id),
 FOREIGN KEY(puzzle_id,revision) REFERENCES content.puzzle_versions(puzzle_id,revision)
);
CREATE FUNCTION editorial.knowledge_immutable() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN RAISE EXCEPTION 'knowledge snapshots and projections are append-only; create a new identity'; END $$;
CREATE TRIGGER knowledge_record_immutable BEFORE UPDATE OR DELETE ON content.knowledge_records FOR EACH ROW EXECUTE FUNCTION editorial.knowledge_immutable();
CREATE TRIGGER knowledge_review_immutable BEFORE UPDATE OR DELETE ON editorial.knowledge_reviews FOR EACH ROW EXECUTE FUNCTION editorial.knowledge_immutable();
CREATE TRIGGER knowledge_reference_immutable BEFORE UPDATE OR DELETE ON content.knowledge_references FOR EACH ROW EXECUTE FUNCTION editorial.knowledge_immutable();
CREATE TRIGGER knowledge_link_immutable BEFORE UPDATE OR DELETE ON content.knowledge_links FOR EACH ROW EXECUTE FUNCTION editorial.knowledge_immutable();
CREATE TRIGGER knowledge_puzzle_ref_immutable BEFORE UPDATE OR DELETE ON content.knowledge_puzzle_refs FOR EACH ROW EXECUTE FUNCTION editorial.knowledge_immutable();

-- definition envelope: {record:<strict importer record>, references:[{reference:
-- <database source_references row>,source:<database content_sources row>}],
-- dependencies:{uuid:database review digest}}. Arrays must be sorted by UUID.
CREATE FUNCTION editorial.check_knowledge_record() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE ref jsonb; dep record; actual jsonb; rec jsonb;
BEGIN
 IF NEW.review_digest<>encode(sha256(convert_to(NEW.definition::text,'UTF8')),'hex') THEN RAISE EXCEPTION 'knowledge digest mismatch'; END IF;
 rec:=NEW.definition->'record';
 IF rec->>'id' IS DISTINCT FROM NEW.id::text OR rec->>'kind' IS DISTINCT FROM NEW.kind OR rec->>'authorId' IS DISTINCT FROM NEW.author_id::text
    OR jsonb_typeof(rec->'fixtureOnly') IS DISTINCT FROM 'boolean'
    OR jsonb_typeof(rec->'referenceIds') IS DISTINCT FROM 'array' OR jsonb_array_length(rec->'referenceIds')=0
    OR jsonb_typeof(rec->'links') IS DISTINCT FROM 'array'
    OR jsonb_typeof(NEW.definition->'references') IS DISTINCT FROM 'array'
    OR jsonb_typeof(NEW.definition->'dependencies') IS DISTINCT FROM 'object' THEN RAISE EXCEPTION 'knowledge envelope mismatch'; END IF;
 SELECT jsonb_agg(jsonb_build_object('reference',to_jsonb(r),'source',to_jsonb(s)) ORDER BY r.id::text)
 INTO actual FROM content.source_references r JOIN content.content_sources s ON s.id=r.source_id
 WHERE r.id::text IN (SELECT jsonb_array_elements_text(rec->'referenceIds'));
 IF actual IS DISTINCT FROM NEW.definition->'references'
 OR jsonb_array_length(actual)<>jsonb_array_length(rec->'referenceIds') THEN RAISE EXCEPTION 'source snapshots mismatch'; END IF;
 SELECT coalesce(jsonb_object_agg(k.id::text,k.review_digest),'{}'::jsonb) INTO actual
 FROM content.knowledge_records k WHERE k.id::text IN (SELECT jsonb_array_elements_text(rec->'links'));
 IF actual IS DISTINCT FROM NEW.definition->'dependencies'
 OR (SELECT count(*) FROM jsonb_object_keys(actual))<>jsonb_array_length(rec->'links') THEN RAISE EXCEPTION 'dependency snapshots mismatch'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER knowledge_record_check BEFORE INSERT ON content.knowledge_records FOR EACH ROW EXECUTE FUNCTION editorial.check_knowledge_record();
CREATE FUNCTION editorial.check_knowledge_projection() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE d jsonb; key text; target text;
BEGIN
 SELECT definition->'record' INTO d FROM content.knowledge_records WHERE id=NEW.record_id;
 IF TG_TABLE_NAME='knowledge_references' THEN key:='referenceIds';target:=NEW.reference_id::text;
 ELSE key:='links';target:=NEW.dependency_id::text; END IF;
 IF NOT (d->key ? target) THEN RAISE EXCEPTION 'projection absent from immutable definition'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER knowledge_reference_check BEFORE INSERT ON content.knowledge_references FOR EACH ROW EXECUTE FUNCTION editorial.check_knowledge_projection();
CREATE TRIGGER knowledge_link_check BEFORE INSERT ON content.knowledge_links FOR EACH ROW EXECUTE FUNCTION editorial.check_knowledge_projection();
CREATE FUNCTION editorial.freeze_knowledge_source() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF (TG_TABLE_NAME='source_references' AND EXISTS(SELECT 1 FROM content.knowledge_records k,jsonb_array_elements(k.definition->'references') r WHERE r->'reference'->>'id'=OLD.id::text))
 OR (TG_TABLE_NAME='content_sources' AND EXISTS(SELECT 1 FROM content.knowledge_records k,jsonb_array_elements(k.definition->'references') r WHERE r->'source'->>'id'=OLD.id::text)) THEN
 RAISE EXCEPTION 'source captured by immutable knowledge snapshot; create a new source/version'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF; RETURN NEW;
END $$;
CREATE TRIGGER knowledge_source_freeze BEFORE UPDATE OR DELETE ON content.content_sources FOR EACH ROW EXECUTE FUNCTION editorial.freeze_knowledge_source();
CREATE TRIGGER knowledge_reference_freeze BEFORE UPDATE OR DELETE ON content.source_references FOR EACH ROW EXECUTE FUNCTION editorial.freeze_knowledge_source();
CREATE FUNCTION editorial.check_knowledge_review() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE k content.knowledge_records; reviewer editorial.knowledge_reviewers;
BEGIN
 SELECT * INTO STRICT k FROM content.knowledge_records WHERE id=NEW.record_id;
 SELECT * INTO STRICT reviewer FROM editorial.knowledge_reviewers WHERE user_id=NEW.reviewer_id;
 IF NOT reviewer.active OR NOT reviewer.human_verified OR NOT NEW.role=ANY(reviewer.roles)
 OR NEW.reviewer_id=k.author_id OR NEW.review_digest<>k.review_digest THEN RAISE EXCEPTION 'untrusted/self/stale review'; END IF;
 IF (SELECT count(*) FROM content.knowledge_references WHERE record_id=k.id)<>jsonb_array_length(k.definition->'record'->'referenceIds')
 OR (SELECT count(*) FROM content.knowledge_links WHERE record_id=k.id)<>jsonb_array_length(k.definition->'record'->'links') THEN RAISE EXCEPTION 'incomplete immutable projections'; END IF;
 IF EXISTS(SELECT 1 FROM editorial.knowledge_reviews v WHERE v.record_id=k.id AND v.reviewer_id=NEW.reviewer_id AND (v.role='publisher')<>(NEW.role='publisher')) THEN RAISE EXCEPTION 'publisher must be independent'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER knowledge_review_check BEFORE INSERT ON editorial.knowledge_reviews FOR EACH ROW EXECUTE FUNCTION editorial.check_knowledge_review();
CREATE FUNCTION editorial.knowledge_publishable(record uuid) RETURNS boolean LANGUAGE sql STABLE AS $$
 WITH RECURSIVE nodes(id) AS (SELECT record UNION SELECT l.dependency_id FROM content.knowledge_links l JOIN nodes n ON l.record_id=n.id)
 SELECT count(*)>0 AND coalesce(bool_and(
 k.definition->'record'->'fixtureOnly'='false'::jsonb
 AND NOT EXISTS(SELECT 1 FROM editorial.knowledge_reviews v WHERE v.record_id=k.id AND v.decision='rejected')
 AND NOT EXISTS(SELECT 1 FROM unnest(CASE WHEN k.kind IN ('poetry','rhetoric','quran_verse','quran_meaning') THEN ARRAY['linguist','scholar','publisher'] ELSE ARRAY['linguist','publisher'] END) required(role)
 WHERE NOT EXISTS(SELECT 1 FROM editorial.knowledge_reviews v JOIN editorial.knowledge_reviewers r ON r.user_id=v.reviewer_id WHERE v.record_id=k.id AND v.role=required.role AND v.decision='approved' AND r.active AND r.human_verified AND v.role=ANY(r.roles)))
 AND NOT EXISTS(SELECT 1 FROM content.knowledge_references kr JOIN content.source_references sr ON sr.id=kr.reference_id JOIN content.content_sources s ON s.id=sr.source_id WHERE kr.record_id=k.id AND (s.approval_status NOT IN ('rights_verified','approved') OR NOT s.permitted_uses @> '["display","adapt","redistribute"]'::jsonb))
 AND (SELECT count(*) FROM content.knowledge_references WHERE record_id=k.id)=jsonb_array_length(k.definition->'record'->'referenceIds')
 AND (SELECT count(*) FROM content.knowledge_links WHERE record_id=k.id)=jsonb_array_length(k.definition->'record'->'links')
 ),false) FROM nodes JOIN content.knowledge_records k USING(id)
$$;
CREATE FUNCTION editorial.check_knowledge_puzzle() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE k content.knowledge_records; p jsonb; expected jsonb;
BEGIN
 SELECT * INTO STRICT k FROM content.knowledge_records WHERE id=NEW.record_id;
 SELECT definition INTO STRICT p FROM content.puzzle_versions WHERE puzzle_id=NEW.puzzle_id AND revision=NEW.revision;
 IF NEW.review_digest<>k.review_digest OR editorial.knowledge_publishable(k.id) IS DISTINCT FROM true THEN RAISE EXCEPTION 'knowledge is not publishable'; END IF;
 -- Full puzzle must be part of reviewed definition; a caller cannot replace its answer.
 expected:=k.definition->'exportPuzzle';
 IF expected IS NULL OR expected<>p THEN RAISE EXCEPTION 'puzzle differs from reviewed immutable export'; END IF;
 IF (SELECT coalesce(jsonb_agg(pr.reference_id::text ORDER BY pr.reference_id::text),'[]'::jsonb) FROM content.puzzle_references pr WHERE pr.puzzle_id=NEW.puzzle_id AND pr.revision=NEW.revision) IS DISTINCT FROM p->'sourceRefs' THEN RAISE EXCEPTION 'puzzle reference projection mismatch'; END IF;
 RETURN NEW;
END $$;
CREATE TRIGGER knowledge_puzzle_check BEFORE INSERT ON content.knowledge_puzzle_refs FOR EACH ROW EXECUTE FUNCTION editorial.check_knowledge_puzzle();
CREATE FUNCTION editorial.freeze_knowledge_puzzle() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM content.knowledge_puzzle_refs k WHERE k.puzzle_id=OLD.puzzle_id AND k.revision=OLD.revision) THEN RAISE EXCEPTION 'knowledge-bound puzzle snapshot/projections immutable'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF; RETURN NEW;
END $$;
CREATE TRIGGER knowledge_puzzle_freeze BEFORE UPDATE OR DELETE ON content.puzzle_versions FOR EACH ROW EXECUTE FUNCTION editorial.freeze_knowledge_puzzle();
CREATE TRIGGER knowledge_puzzle_refs_freeze BEFORE UPDATE OR DELETE ON content.puzzle_references FOR EACH ROW EXECUTE FUNCTION editorial.freeze_knowledge_puzzle();
CREATE FUNCTION editorial.reject_extra_knowledge_puzzle_ref() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF EXISTS(SELECT 1 FROM content.knowledge_puzzle_refs k WHERE k.puzzle_id=NEW.puzzle_id AND k.revision=NEW.revision) THEN RAISE EXCEPTION 'knowledge-bound puzzle projections immutable'; END IF; RETURN NEW;
END $$;
CREATE TRIGGER knowledge_puzzle_refs_insert_freeze BEFORE INSERT ON content.puzzle_references FOR EACH ROW EXECUTE FUNCTION editorial.reject_extra_knowledge_puzzle_ref();
DO $$ DECLARE t text; BEGIN
 FOREACH t IN ARRAY ARRAY['content.knowledge_records','content.knowledge_references','content.knowledge_links','content.knowledge_puzzle_refs','editorial.knowledge_reviewers','editorial.knowledge_reviews'] LOOP
 EXECUTE 'ALTER TABLE '||t||' ENABLE ROW LEVEL SECURITY'; EXECUTE 'ALTER TABLE '||t||' FORCE ROW LEVEL SECURITY'; EXECUTE 'REVOKE ALL ON '||t||' FROM PUBLIC';
 END LOOP;
END $$;
REVOKE ALL ON FUNCTION editorial.knowledge_publishable(uuid) FROM PUBLIC;
COMMIT;
