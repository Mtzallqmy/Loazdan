-- DESIGN BLUEPRINT ONLY: not executed against any database.
BEGIN;
CREATE SCHEMA identity;
CREATE SCHEMA content;
CREATE SCHEMA play;
CREATE SCHEMA ops;
CREATE SCHEMA editorial;

-- الحساب الاختياري دون تخزين كلمة مرور
CREATE TABLE identity.users (
  id uuid PRIMARY KEY,
  oidc_issuer text NOT NULL,
  oidc_subject text NOT NULL,
  display_name text,
  created_at timestamptz NOT NULL DEFAULT now(),
  deletion_requested_at timestamptz,
  UNIQUE(oidc_issuer,oidc_subject)
);
ALTER TABLE identity.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.users FORCE ROW LEVEL SECURITY;

-- ربط جهاز عشوائي بحساب
CREATE TABLE identity.devices (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  device_id uuid NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  revoked_at timestamptz,
  PRIMARY KEY(user_id,device_id)
);
ALTER TABLE identity.devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.devices FORCE ROW LEVEL SECURITY;

-- RBAC التحرير
CREATE TABLE identity.staff_roles (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  role text NOT NULL CHECK(role IN ('owner','editor','linguist','scholar','publisher')),
  PRIMARY KEY(user_id,role)
);
ALTER TABLE identity.staff_roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.staff_roles FORCE ROW LEVEL SECURITY;

-- تسجيل الأنواع
CREATE TABLE content.game_types (
  id text PRIMARY KEY,
  engine_version integer NOT NULL CHECK(engine_version>0),
  schema_version integer NOT NULL CHECK(schema_version>0),
  title text NOT NULL
);
ALTER TABLE content.game_types ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.game_types FORCE ROW LEVEL SECURITY;

-- العوالم الستة والفروع؛ فحص الدورات في API
CREATE TABLE content.categories (
  id uuid PRIMARY KEY,
  parent_id uuid REFERENCES content.categories(id),
  slug text NOT NULL UNIQUE,
  title text NOT NULL,
  sort_order integer NOT NULL DEFAULT 0
);
CREATE INDEX content_categories_0_idx ON content.categories (parent_id);
ALTER TABLE content.categories ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.categories FORCE ROW LEVEL SECURITY;

-- النوع مقابل اللعبة التحريرية
CREATE TABLE content.games (
  id uuid PRIMARY KEY,
  game_type_id text NOT NULL REFERENCES content.game_types(id),
  category_id uuid NOT NULL REFERENCES content.categories(id),
  slug text NOT NULL UNIQUE,
  title text NOT NULL,
  enabled boolean NOT NULL DEFAULT false
);
CREATE INDEX content_games_0_idx ON content.games (game_type_id);
CREATE INDEX content_games_1_idx ON content.games (category_id);
ALTER TABLE content.games ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.games FORCE ROW LEVEL SECURITY;

-- التدرج داخل اللعبة
CREATE TABLE content.levels (
  id uuid PRIMARY KEY,
  game_id uuid NOT NULL REFERENCES content.games(id),
  ordinal integer NOT NULL CHECK(ordinal>0),
  difficulty smallint NOT NULL CHECK(difficulty BETWEEN 1 AND 5),
  title text NOT NULL,
  UNIQUE(game_id,ordinal)
);
ALTER TABLE content.levels ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.levels FORCE ROW LEVEL SECURITY;

-- سجل الحقوق العلمي
CREATE TABLE content.content_sources (
  id uuid PRIMARY KEY,
  slug text NOT NULL UNIQUE,
  name text NOT NULL,
  author text NOT NULL,
  url text,
  edition text,
  license_code text NOT NULL,
  license_evidence_url text,
  permitted_uses jsonb NOT NULL CHECK(jsonb_typeof(permitted_uses)='array'),
  checked_on date,
  approval_status text NOT NULL CHECK(approval_status IN ('candidate','rights_verified','approved','blocked','withdrawn')),
  notes text NOT NULL DEFAULT ''
);
ALTER TABLE content.content_sources ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.content_sources FORCE ROW LEVEL SECURITY;

-- صفحة/آية/مدخل مع نسخة
CREATE TABLE content.source_references (
  id uuid PRIMARY KEY,
  source_id uuid NOT NULL REFERENCES content.content_sources(id),
  locator text NOT NULL,
  source_version text NOT NULL,
  excerpt_sha256 char(64) CHECK(excerpt_sha256 ~ '^[a-f0-9]{64}$'),
  attribution text NOT NULL,
  UNIQUE(source_id,locator,source_version)
);
CREATE INDEX content_source_references_0_idx ON content.source_references (source_id);
ALTER TABLE content.source_references ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.source_references FORCE ROW LEVEL SECURITY;

-- جذر موثق دون افتراض وحدانية التحليل
CREATE TABLE content.lexical_roots (
  id uuid PRIMARY KEY,
  root text NOT NULL,
  reference_id uuid NOT NULL REFERENCES content.source_references(id),
  UNIQUE(root,reference_id)
);
CREATE INDEX content_lexical_roots_0_idx ON content.lexical_roots (reference_id);
ALTER TABLE content.lexical_roots ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.lexical_roots FORCE ROW LEVEL SECURITY;

-- المعنى المحدد في المرجع لا لفظة غامضة
CREATE TABLE content.vocabulary (
  id uuid PRIMARY KEY,
  headword text NOT NULL,
  display_text text NOT NULL,
  normalized_key text NOT NULL,
  normalization_version integer NOT NULL CHECK(normalization_version>0),
  root_id uuid REFERENCES content.lexical_roots(id),
  reference_id uuid NOT NULL REFERENCES content.source_references(id),
  sense text NOT NULL,
  review_status text NOT NULL CHECK(review_status IN ('draft','approved','withdrawn'))
);
CREATE INDEX content_vocabulary_0_idx ON content.vocabulary (normalized_key);
CREATE INDEX content_vocabulary_1_idx ON content.vocabulary (root_id);
CREATE INDEX content_vocabulary_2_idx ON content.vocabulary (reference_id);
ALTER TABLE content.vocabulary ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.vocabulary FORCE ROW LEVEL SECURITY;

-- metadata الوسائط؛ الملف خارجي
CREATE TABLE content.media_assets (
  id uuid PRIMARY KEY,
  source_reference_id uuid NOT NULL REFERENCES content.source_references(id),
  sha256 char(64) NOT NULL CHECK(sha256 ~ '^[a-f0-9]{64}$'),
  object_key text NOT NULL UNIQUE,
  mime_type text NOT NULL CHECK(mime_type IN ('image/webp','image/png','image/jpeg','audio/ogg')),
  bytes bigint NOT NULL CHECK(bytes>0),
  width integer CHECK(width>0 AND width<=4096),
  height integer CHECK(height>0 AND height<=4096),
  alt_text text NOT NULL,
  status text NOT NULL CHECK(status IN ('draft','approved','withdrawn'))
);
CREATE INDEX content_media_assets_0_idx ON content.media_assets (source_reference_id);
CREATE INDEX content_media_assets_1_idx ON content.media_assets (sha256);
ALTER TABLE content.media_assets ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.media_assets FORCE ROW LEVEL SECURITY;

-- هوية دائمة مستقلة عن الحزم
CREATE TABLE content.puzzles (
  id uuid PRIMARY KEY,
  game_id uuid NOT NULL REFERENCES content.games(id),
  level_id uuid REFERENCES content.levels(id),
  created_by uuid REFERENCES identity.users(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX content_puzzles_0_idx ON content.puzzles (game_id);
CREATE INDEX content_puzzles_1_idx ON content.puzzles (level_id);
ALTER TABLE content.puzzles ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.puzzles FORCE ROW LEVEL SECURITY;

-- snapshot كامل وحيد مصدر export؛ JSON schema تحقق API/publisher
CREATE TABLE content.puzzle_versions (
  puzzle_id uuid NOT NULL REFERENCES content.puzzles(id),
  revision integer NOT NULL CHECK(revision>0),
  schema_version integer NOT NULL CHECK(schema_version>0),
  rules_version integer NOT NULL CHECK(rules_version>0),
  difficulty smallint NOT NULL CHECK(difficulty BETWEEN 1 AND 5),
  normalization text NOT NULL CHECK(normalization IN ('exact_v1','arabic_basic_v1')),
  definition jsonb NOT NULL CHECK(jsonb_typeof(definition)='object'),
  definition_sha256 char(64) NOT NULL CHECK(definition_sha256 ~ '^[a-f0-9]{64}$'),
  status text NOT NULL CHECK(status IN ('draft','validation','linguistic_review','scholarly_review','approved','published')),
  author_id uuid REFERENCES identity.users(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY(puzzle_id,revision)
);
CREATE INDEX content_puzzle_versions_0_idx ON content.puzzle_versions (status, created_at);
ALTER TABLE content.puzzle_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.puzzle_versions FORCE ROW LEVEL SECURITY;

-- إسقاط answer من definition للمراجعة، لا master ثان
CREATE TABLE content.puzzle_answers (
  puzzle_id uuid NOT NULL,
  revision integer NOT NULL,
  slot text NOT NULL,
  accepted jsonb NOT NULL CHECK(jsonb_typeof(accepted)='array'),
  PRIMARY KEY(puzzle_id,revision,slot),
  FOREIGN KEY(puzzle_id,revision) REFERENCES content.puzzle_versions(puzzle_id,revision)
);
ALTER TABLE content.puzzle_answers ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.puzzle_answers FORCE ROW LEVEL SECURITY;

-- إسقاط hints
CREATE TABLE content.puzzle_hints (
  puzzle_id uuid NOT NULL,
  revision integer NOT NULL,
  slot text NOT NULL,
  kind text NOT NULL,
  hint_text text NOT NULL,
  penalty integer NOT NULL CHECK(penalty BETWEEN 0 AND 100),
  PRIMARY KEY(puzzle_id,revision,slot),
  FOREIGN KEY(puzzle_id,revision) REFERENCES content.puzzle_versions(puzzle_id,revision)
);
ALTER TABLE content.puzzle_hints ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.puzzle_hints FORCE ROW LEVEL SECURITY;

-- إسقاط الشرح الموثق
CREATE TABLE content.puzzle_explanations (
  puzzle_id uuid NOT NULL,
  revision integer NOT NULL,
  slot text NOT NULL,
  body text NOT NULL,
  source_reference_id uuid NOT NULL REFERENCES content.source_references(id),
  PRIMARY KEY(puzzle_id,revision,slot),
  FOREIGN KEY(puzzle_id,revision) REFERENCES content.puzzle_versions(puzzle_id,revision)
);
ALTER TABLE content.puzzle_explanations ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.puzzle_explanations FORCE ROW LEVEL SECURITY;

-- مراجع متعدد لمتعدد
CREATE TABLE content.puzzle_references (
  puzzle_id uuid NOT NULL,
  revision integer NOT NULL,
  reference_id uuid NOT NULL REFERENCES content.source_references(id),
  PRIMARY KEY(puzzle_id,revision,reference_id),
  FOREIGN KEY(puzzle_id,revision) REFERENCES content.puzzle_versions(puzzle_id,revision)
);
CREATE INDEX content_puzzle_references_0_idx ON content.puzzle_references (reference_id);
ALTER TABLE content.puzzle_references ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.puzzle_references FORCE ROW LEVEL SECURITY;

-- بوابة القراءة؛ السحب دون تعديل snapshot
CREATE TABLE content.publications (
  puzzle_id uuid NOT NULL,
  revision integer NOT NULL,
  published_at timestamptz NOT NULL,
  withdrawn_at timestamptz,
  PRIMARY KEY(puzzle_id,revision),
  FOREIGN KEY(puzzle_id,revision) REFERENCES content.puzzle_versions(puzzle_id,revision),
  CHECK(withdrawn_at IS NULL OR withdrawn_at>=published_at)
);
ALTER TABLE content.publications ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.publications FORCE ROW LEVEL SECURITY;

-- هوية الحزمة
CREATE TABLE content.content_packs (
  id uuid PRIMARY KEY,
  slug text NOT NULL UNIQUE,
  category_id uuid NOT NULL REFERENCES content.categories(id),
  title text NOT NULL,
  is_core boolean NOT NULL DEFAULT false,
  withdrawn_at timestamptz
);
CREATE INDEX content_content_packs_0_idx ON content.content_packs (category_id);
ALTER TABLE content.content_packs ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.content_packs FORCE ROW LEVEL SECURITY;

-- manifest immutable بعد النشر؛ الملف الخارجي يحوي توقيعه
CREATE TABLE content.content_pack_versions (
  pack_id uuid NOT NULL REFERENCES content.content_packs(id),
  version integer NOT NULL CHECK(version>0),
  schema_version integer NOT NULL CHECK(schema_version>0),
  min_app_version_code integer NOT NULL CHECK(min_app_version_code>0),
  manifest jsonb NOT NULL,
  manifest_sha256 char(64) NOT NULL CHECK(manifest_sha256 ~ '^[a-f0-9]{64}$'),
  download_bytes bigint NOT NULL CHECK(download_bytes>0),
  installed_bytes bigint NOT NULL CHECK(installed_bytes>0),
  object_key text NOT NULL UNIQUE,
  status text NOT NULL CHECK(status IN ('building','verified','published','withdrawn')),
  published_at timestamptz,
  PRIMARY KEY(pack_id,version)
);
CREATE INDEX content_content_pack_versions_0_idx ON content.content_pack_versions (status,published_at);
ALTER TABLE content.content_pack_versions ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.content_pack_versions FORCE ROW LEVEL SECURITY;

-- نسخة دقيقة من اللغز داخل الحزمة
CREATE TABLE content.content_pack_items (
  pack_id uuid NOT NULL,
  pack_version integer NOT NULL,
  puzzle_id uuid NOT NULL,
  puzzle_revision integer NOT NULL,
  PRIMARY KEY(pack_id,pack_version,puzzle_id),
  FOREIGN KEY(pack_id,pack_version) REFERENCES content.content_pack_versions(pack_id,version),
  FOREIGN KEY(puzzle_id,puzzle_revision) REFERENCES content.puzzle_versions(puzzle_id,revision)
);
CREATE INDEX content_content_pack_items_0_idx ON content.content_pack_items (puzzle_id,puzzle_revision);
ALTER TABLE content.content_pack_items ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.content_pack_items FORCE ROW LEVEL SECURITY;

-- DAG التبعيات؛ cycles يتحقق publisher
CREATE TABLE content.pack_dependencies (
  pack_id uuid NOT NULL,
  version integer NOT NULL,
  dependency_pack_id uuid NOT NULL,
  dependency_version integer NOT NULL,
  PRIMARY KEY(pack_id,version,dependency_pack_id),
  FOREIGN KEY(pack_id,version) REFERENCES content.content_pack_versions(pack_id,version),
  FOREIGN KEY(dependency_pack_id,dependency_version) REFERENCES content.content_pack_versions(pack_id,version),
  CHECK(pack_id<>dependency_pack_id)
);
CREATE INDEX content_pack_dependencies_0_idx ON content.pack_dependencies (dependency_pack_id,dependency_version);
ALTER TABLE content.pack_dependencies ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.pack_dependencies FORCE ROW LEVEL SECURITY;

-- قواعد الإنجاز بيانات ذات types محددة لا code
CREATE TABLE content.achievements (
  id uuid PRIMARY KEY,
  code text NOT NULL UNIQUE,
  title text NOT NULL,
  rule_version integer NOT NULL CHECK(rule_version>0),
  rule jsonb NOT NULL
);
ALTER TABLE content.achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.achievements FORCE ROW LEVEL SECURITY;

-- بنك منافسة خاص لا يدخل الحزم
CREATE TABLE content.daily_challenges (
  id uuid PRIMARY KEY,
  challenge_date date NOT NULL UNIQUE,
  starts_at timestamptz NOT NULL,
  ends_at timestamptz NOT NULL,
  rules_version integer NOT NULL CHECK(rules_version>0),
  private_definition jsonb NOT NULL,
  CHECK(ends_at>starts_at)
);
ALTER TABLE content.daily_challenges ENABLE ROW LEVEL SECURITY;
ALTER TABLE content.daily_challenges FORCE ROW LEVEL SECURITY;

-- self-reported practice projection
CREATE TABLE play.player_progress (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  puzzle_id uuid NOT NULL REFERENCES content.puzzles(id),
  completed boolean NOT NULL DEFAULT false,
  best_score integer NOT NULL DEFAULT 0 CHECK(best_score BETWEEN 0 AND 300),
  attempts integer NOT NULL DEFAULT 0 CHECK(attempts>=0),
  server_version bigint NOT NULL CHECK(server_version>0 AND server_version<=9007199254740991),
  updated_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY(user_id,puzzle_id)
);
CREATE INDEX play_player_progress_0_idx ON play.player_progress (puzzle_id);
ALTER TABLE play.player_progress ENABLE ROW LEVEL SECURITY;
ALTER TABLE play.player_progress FORCE ROW LEVEL SECURITY;

-- dedup جلسة محلية؛ الحالة التفصيلية تبقى على الجهاز
CREATE TABLE play.player_sessions (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  id uuid NOT NULL,
  device_id uuid NOT NULL,
  puzzle_id uuid NOT NULL,
  puzzle_revision integer NOT NULL,
  outcome text NOT NULL CHECK(outcome IN ('won','lost','abandoned')),
  score integer NOT NULL CHECK(score BETWEEN 0 AND 300),
  result_hash char(64) NOT NULL CHECK(result_hash ~ '^[a-f0-9]{64}$'),
  trust text NOT NULL DEFAULT 'self_reported' CHECK(trust='self_reported'),
  received_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY(user_id,id),
  FOREIGN KEY(user_id,device_id) REFERENCES identity.devices(user_id,device_id),
  FOREIGN KEY(puzzle_id,puzzle_revision) REFERENCES content.puzzle_versions(puzzle_id,revision)
);
CREATE INDEX play_player_sessions_0_idx ON play.player_sessions (puzzle_id,puzzle_revision);
CREATE INDEX play_player_sessions_1_idx ON play.player_sessions (user_id,received_at);
ALTER TABLE play.player_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE play.player_sessions FORCE ROW LEVEL SECURITY;

-- union فريد
CREATE TABLE play.player_achievements (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  achievement_id uuid NOT NULL REFERENCES content.achievements(id),
  awarded_at timestamptz NOT NULL DEFAULT now(),
  server_version bigint NOT NULL CHECK(server_version>0 AND server_version<=9007199254740991),
  PRIMARY KEY(user_id,achievement_id)
);
CREATE INDEX play_player_achievements_0_idx ON play.player_achievements (achievement_id);
ALTER TABLE play.player_achievements ENABLE ROW LEVEL SECURITY;
ALTER TABLE play.player_achievements FORCE ROW LEVEL SECURITY;

-- حفظ وتراجع/tombstone
CREATE TABLE play.saved_words (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  vocabulary_id uuid NOT NULL REFERENCES content.vocabulary(id),
  saved boolean NOT NULL,
  server_version bigint NOT NULL CHECK(server_version>0 AND server_version<=9007199254740991),
  PRIMARY KEY(user_id,vocabulary_id)
);
CREATE INDEX play_saved_words_0_idx ON play.saved_words (vocabulary_id);
ALTER TABLE play.saved_words ENABLE ROW LEVEL SECURITY;
ALTER TABLE play.saved_words FORCE ROW LEVEL SECURITY;

-- CAS إعدادات
CREATE TABLE play.preferences (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  key text NOT NULL CHECK(key IN ('theme','sound','reducedMotion')),
  value text NOT NULL,
  server_version bigint NOT NULL CHECK(server_version>0 AND server_version<=9007199254740991),
  PRIMARY KEY(user_id,key),
  CHECK((key='theme' AND value IN ('light','dark','system')) OR (key IN ('sound','reducedMotion') AND value IN ('on','off')))
);
ALTER TABLE play.preferences ENABLE ROW LEVEL SECURITY;
ALTER TABLE play.preferences FORCE ROW LEVEL SECURITY;

-- attempt واحد رسمي
CREATE TABLE play.challenge_attempts (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  id uuid NOT NULL,
  challenge_id uuid NOT NULL REFERENCES content.daily_challenges(id),
  nonce_hash char(64) NOT NULL,
  started_at timestamptz NOT NULL DEFAULT now(),
  expires_at timestamptz NOT NULL,
  submitted_at timestamptz,
  transcript jsonb,
  submission_sha256 char(64) CHECK(submission_sha256 ~ '^[a-f0-9]{64}$'),
  terminal_result jsonb,
  state text NOT NULL DEFAULT 'started' CHECK(state IN ('started','submitted','expired')),
  pinned_definition jsonb NOT NULL,
  pinned_rules_version integer NOT NULL CHECK(pinned_rules_version>0),
  nonce_key_id text NOT NULL,
  verified_score integer CHECK(verified_score>=0),
  PRIMARY KEY(user_id,id),
  UNIQUE(user_id,challenge_id),
  CHECK(expires_at>started_at)
);
CREATE INDEX play_challenge_attempts_0_idx ON play.challenge_attempts (challenge_id);
ALTER TABLE play.challenge_attempts ENABLE ROW LEVEL SECURITY;
ALTER TABLE play.challenge_attempts FORCE ROW LEVEL SECURITY;

-- نقاط خادمية فقط؛ API لا يكشف userId
CREATE TABLE play.leaderboard_entries (
  challenge_id uuid NOT NULL REFERENCES content.daily_challenges(id),
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  attempt_id uuid NOT NULL,
  score integer NOT NULL CHECK(score>=0),
  achieved_at timestamptz NOT NULL,
  opted_in boolean NOT NULL DEFAULT false,
  PRIMARY KEY(challenge_id,user_id),
  FOREIGN KEY(user_id,attempt_id) REFERENCES play.challenge_attempts(user_id,id)
);
CREATE INDEX play_leaderboard_entries_0_idx ON play.leaderboard_entries (challenge_id,score DESC,achieved_at,user_id);
ALTER TABLE play.leaderboard_entries ENABLE ROW LEVEL SECURITY;
ALTER TABLE play.leaderboard_entries FORCE ROW LEVEL SECURITY;

-- منع تكرار مكافأة؛ ليس رصيد عملات في v1
CREATE TABLE play.reward_ledger (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  reward_key text NOT NULL,
  awarded_at timestamptz NOT NULL DEFAULT now(),
  details jsonb NOT NULL,
  PRIMARY KEY(user_id,reward_key)
);
ALTER TABLE play.reward_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE play.reward_ledger FORCE ROW LEVEL SECURITY;

-- قفل تسلسل commits
CREATE TABLE ops.user_sync_clock (
  user_id uuid PRIMARY KEY REFERENCES identity.users(id) ON DELETE CASCADE,
  version bigint NOT NULL DEFAULT 0 CHECK(version>=0 AND version<=9007199254740991)
);
ALTER TABLE ops.user_sync_clock ENABLE ROW LEVEL SECURITY;
ALTER TABLE ops.user_sync_clock FORCE ROW LEVEL SECURITY;

-- receipt دائم عمر الحساب
CREATE TABLE ops.sync_operations (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  operation_id uuid NOT NULL,
  device_id uuid NOT NULL,
  device_seq bigint NOT NULL CHECK(device_seq>0 AND device_seq<=2147483647),
  payload_sha256 char(64) NOT NULL CHECK(payload_sha256 ~ '^[a-f0-9]{64}$'),
  kind text NOT NULL CHECK(kind IN ('complete_session','save_word','set_preference')),
  result jsonb NOT NULL,
  received_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY(user_id,operation_id),
  UNIQUE(user_id,device_id,device_seq),
  FOREIGN KEY(user_id,device_id) REFERENCES identity.devices(user_id,device_id)
);
CREATE INDEX ops_sync_operations_0_idx ON ops.sync_operations (user_id,received_at);
ALTER TABLE ops.sync_operations ENABLE ROW LEVEL SECURITY;
ALTER TABLE ops.sync_operations FORCE ROW LEVEL SECURITY;

-- 90 يومًا pull log
CREATE TABLE ops.sync_changes (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  version bigint NOT NULL CHECK(version>0 AND version<=9007199254740991),
  ordinal integer NOT NULL CHECK(ordinal>=0),
  entity_type text NOT NULL,
  entity_id text NOT NULL,
  value jsonb NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  PRIMARY KEY(user_id,version,ordinal)
);
CREATE INDEX ops_sync_changes_0_idx ON ops.sync_changes (created_at);
ALTER TABLE ops.sync_changes ENABLE ROW LEVEL SECURITY;
ALTER TABLE ops.sync_changes FORCE ROW LEVEL SECURITY;

-- snapshot ثابت مع pagination؛ limit payload 20MiB حتى scale redesign
CREATE TABLE ops.sync_snapshots (
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  id uuid NOT NULL,
  high_water bigint NOT NULL CHECK(high_water>=0 AND high_water<=9007199254740991),
  payload jsonb NOT NULL,
  expires_at timestamptz NOT NULL,
  PRIMARY KEY(user_id,id)
);
CREATE INDEX ops_sync_snapshots_0_idx ON ops.sync_snapshots (expires_at);
ALTER TABLE ops.sync_snapshots ENABLE ROW LEVEL SECURITY;
ALTER TABLE ops.sync_snapshots FORCE ROW LEVEL SECURITY;

-- review مربوط digest يمنع نشر محتوى تغير بعد المراجعة
CREATE TABLE editorial.editorial_reviews (
  id uuid PRIMARY KEY,
  puzzle_id uuid NOT NULL,
  revision integer NOT NULL,
  reviewer_id uuid REFERENCES identity.users(id) ON DELETE SET NULL,
  stage text NOT NULL CHECK(stage IN ('validation','linguistic_review','scholarly_review','approved')),
  decision text NOT NULL CHECK(decision IN ('pass','reject')),
  definition_sha256 char(64) NOT NULL,
  notes text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY(puzzle_id,revision) REFERENCES content.puzzle_versions(puzzle_id,revision)
);
CREATE INDEX editorial_editorial_reviews_0_idx ON editorial.editorial_reviews (puzzle_id,revision,stage);
CREATE INDEX editorial_editorial_reviews_1_idx ON editorial.editorial_reviews (reviewer_id);
ALTER TABLE editorial.editorial_reviews ENABLE ROW LEVEL SECURITY;
ALTER TABLE editorial.editorial_reviews FORCE ROW LEVEL SECURITY;

-- سجل append-only دون أسرار أو نصوص شخصية
CREATE TABLE editorial.audit_logs (
  id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  actor_id uuid REFERENCES identity.users(id) ON DELETE SET NULL,
  action text NOT NULL,
  entity_type text NOT NULL,
  entity_id text NOT NULL,
  before_sha256 char(64),
  after_sha256 char(64),
  request_id uuid NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX editorial_audit_logs_0_idx ON editorial.audit_logs (entity_type,entity_id,created_at);
CREATE INDEX editorial_audit_logs_1_idx ON editorial.audit_logs (created_at);
CREATE INDEX editorial_audit_logs_2_idx ON editorial.audit_logs (actor_id);
ALTER TABLE editorial.audit_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE editorial.audit_logs FORCE ROW LEVEL SECURITY;

-- جلسات refresh وتدوير العائلة؛ لا token raw
CREATE TABLE identity.auth_sessions (
  id uuid PRIMARY KEY,
  user_id uuid NOT NULL REFERENCES identity.users(id) ON DELETE CASCADE,
  family_id uuid NOT NULL,
  refresh_hash char(64) NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  revoked_at timestamptz,
  rotated_to uuid REFERENCES identity.auth_sessions(id)
);
CREATE INDEX identity_auth_sessions_0_idx ON identity.auth_sessions (user_id);
CREATE INDEX identity_auth_sessions_1_idx ON identity.auth_sessions (family_id);
CREATE INDEX identity_auth_sessions_2_idx ON identity.auth_sessions (expires_at);
ALTER TABLE identity.auth_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE identity.auth_sessions FORCE ROW LEVEL SECURITY;

-- وظيفة بناء durable مع lease/retry
CREATE TABLE editorial.publication_jobs (
  id uuid PRIMARY KEY,
  pack_id uuid NOT NULL REFERENCES content.content_packs(id),
  version integer NOT NULL CHECK(version>0),
  requested_by uuid REFERENCES identity.users(id) ON DELETE SET NULL,
  state text NOT NULL CHECK(state IN ('queued','running','failed','verified','published')),
  request_payload jsonb NOT NULL,
  error_code text,
  lease_until timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE(pack_id,version)
);
CREATE INDEX editorial_publication_jobs_0_idx ON editorial.publication_jobs (state,lease_until);
ALTER TABLE editorial.publication_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE editorial.publication_jobs FORCE ROW LEVEL SECURITY;

-- No grants to PUBLIC. Deployment roles/policies are a separate, reviewed migration.
REVOKE ALL ON SCHEMA identity, content, play, ops, editorial FROM PUBLIC;
REVOKE ALL ON ALL TABLES IN SCHEMA identity, content, play, ops, editorial FROM PUBLIC;
CREATE FUNCTION content.reject_published_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF OLD.status = 'published' THEN
    IF TG_OP = 'UPDATE' AND OLD.author_id IS NOT NULL AND NEW.author_id IS NULL
       AND (to_jsonb(NEW) - 'author_id') = (to_jsonb(OLD) - 'author_id') THEN RETURN NEW; END IF;
    RAISE EXCEPTION 'Published puzzle revision is immutable';
  END IF;
  IF TG_OP = 'DELETE' THEN RETURN OLD; END IF;
  RETURN NEW;
END;
$$;
CREATE TRIGGER immutable_published_revision BEFORE UPDATE OR DELETE ON content.puzzle_versions
FOR EACH ROW EXECUTE FUNCTION content.reject_published_mutation();

CREATE TABLE play.leaderboard_profiles (
 user_id uuid PRIMARY KEY REFERENCES identity.users(id) ON DELETE CASCADE,
 opted_in boolean NOT NULL DEFAULT false,
 alias text CHECK(char_length(alias) BETWEEN 3 AND 24),
 moderation_status text NOT NULL DEFAULT 'none' CHECK(moderation_status IN ('none','pending','approved','rejected')),
 updated_at timestamptz NOT NULL DEFAULT now(),
 CHECK(NOT opted_in OR alias IS NOT NULL)
);
ALTER TABLE play.leaderboard_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE play.leaderboard_profiles FORCE ROW LEVEL SECURITY;
CREATE FUNCTION content.protect_pack_version() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
 IF OLD.status IN ('published','withdrawn') THEN
   IF TG_OP='UPDATE' AND OLD.status='published' AND NEW.status='withdrawn'
      AND (to_jsonb(NEW)-'status')=(to_jsonb(OLD)-'status') THEN RETURN NEW; END IF;
   RAISE EXCEPTION 'Published pack is immutable';
 END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER immutable_pack BEFORE UPDATE OR DELETE ON content.content_pack_versions
FOR EACH ROW EXECUTE FUNCTION content.protect_pack_version();
CREATE FUNCTION content.protect_pack_children() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE p uuid; v integer; s text;
BEGIN
 IF TG_OP='UPDATE' THEN RAISE EXCEPTION 'Replace draft pack child using delete/insert'; END IF;
 IF TG_OP='DELETE' THEN p=OLD.pack_id; ELSE p=NEW.pack_id; END IF;
 IF TG_TABLE_NAME='content_pack_items' THEN
   IF TG_OP='DELETE' THEN v=OLD.pack_version; ELSE v=NEW.pack_version; END IF;
 ELSE
   IF TG_OP='DELETE' THEN v=OLD.version; ELSE v=NEW.version; END IF;
 END IF;
 SELECT status INTO s FROM content.content_pack_versions WHERE pack_id=p AND version=v FOR UPDATE;
 IF s IN ('published','withdrawn') THEN RAISE EXCEPTION 'Published pack children immutable'; END IF;
 IF TG_OP='DELETE' THEN RETURN OLD; END IF;
 RETURN NEW;
END;
$$;
CREATE TRIGGER immutable_pack_items BEFORE INSERT OR UPDATE OR DELETE ON content.content_pack_items
FOR EACH ROW EXECUTE FUNCTION content.protect_pack_children();
CREATE TRIGGER immutable_pack_dependencies BEFORE INSERT OR UPDATE OR DELETE ON content.pack_dependencies
FOR EACH ROW EXECUTE FUNCTION content.protect_pack_children();
COMMIT;
