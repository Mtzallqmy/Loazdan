# عقد API v1

المصدر الآلي الكامل OpenAPI 3.1.0: `contracts/openapi.json`، JSON Schema 2020-12. المسارات نسبية فلا توجد خدمة وهمية. Request/response camelCase، SQL snake_case mapping صريح. UUID lowercase string؛ timestamps RFC3339 UTC؛ bigint cursor serialized opaque لا رقم JS خارج safe range. الاستجابات UTF-8.

## جدول المسارات

| method | path | عملية | صلاحية |
|---|---|---|---|
| GET | `/health/live` | Process liveness | public |
| GET | `/health/ready` | Dependency readiness without details | public |
| GET | `/v1/bootstrap` | Public app compatibility and catalog settings | public |
| GET | `/v1/games` | Enabled game catalog | public |
| GET | `/v1/packs` | Published compatible content packs | public |
| GET | `/v1/packs/{packId}/versions` | Published version metadata | public |
| GET | `/v1/packs/{packId}/versions/{version}/manifest` | Signed immutable manifest | public |
| POST | `/v1/packs/{packId}/versions/{version}/download-session` | Temporary links for manifest-owned chunks | public |
| GET | `/v1/sources/{sourceId}` | Public rights and provenance record | public |
| POST | `/v1/auth/exchange` | Exchange OIDC code with PKCE; configured provider only | public |
| POST | `/v1/auth/refresh` | Rotate refresh credential | public |
| POST | `/v1/auth/logout` | Revoke current refresh family | authenticated |
| GET | `/v1/me` | Current optional account | authenticated |
| DELETE | `/v1/me` | Request account erasure and revoke sessions | authenticated |
| POST | `/v1/devices` | Register random device ID | authenticated |
| POST | `/v1/auth/guest-upgrade` | Authorize import; local outbox still required | authenticated |
| POST | `/v1/sync/operations` | Idempotent per-operation transaction batch | authenticated |
| GET | `/v1/progress` | Practice progress, never official scores | authenticated |
| GET | `/v1/achievements` | Earned achievements | authenticated |
| GET | `/v1/progress/changes` | Commit-ordered changes after cursor | authenticated |
| GET | `/v1/progress/snapshot` | Immutable snapshot page with high-water cursor | authenticated |
| GET | `/v1/challenges/daily` | Daily challenge metadata; UTC schedule | public |
| POST | `/v1/challenges/{challengeId}/start` | Create or return single official attempt | authenticated |
| POST | `/v1/challenges/{challengeId}/submit` | Server evaluates answers and deduplicates reward | authenticated |
| GET | `/v1/challenges/{challengeId}/leaderboard` | Opt-in pseudonymous ranking | public |
| GET | `/v1/admin/puzzles` | Editorial drafts and revisions | editor|linguist|scholar|publisher|owner |
| POST | `/v1/admin/puzzles` | Create initial draft; no publishing side effect | editor|owner |
| POST | `/v1/admin/puzzles/{puzzleId}/revisions` | Create next draft revision, never overwrite published | editor|owner |
| POST | `/v1/admin/puzzles/{puzzleId}/revisions/{revision}/validate` | Structural and semantic checks | editor|owner |
| POST | `/v1/admin/puzzles/{puzzleId}/revisions/{revision}/reviews` | Role-bound independent review | linguist|scholar|publisher|owner |
| POST | `/v1/admin/puzzles/{puzzleId}/revisions/{revision}/publish` | Publish only exact independently approved content | publisher|owner |
| POST | `/v1/admin/puzzles/{puzzleId}/revisions/{revision}/withdraw` | Withdraw and audit; retains history and progress | publisher|owner |
| POST | `/v1/admin/packs` | Create catalog pack identity | publisher|owner |
| POST | `/v1/admin/packs/{packId}/builds` | Queue deterministic verified pack publication | publisher|owner |
| GET | `/v1/admin/jobs/{jobId}` | Inspect pack build; no sensitive logs | publisher|owner |
| GET | `/v1/admin/audit` | Read immutable audit metadata | owner |
| POST | `/v1/admin/sources` | Register candidate rights evidence | editor|owner |
| POST | `/v1/admin/sources/{sourceId}/review` | Record licensing approval; owner duty | owner |
| POST | `/v1/admin/media` | Register sanitized asset after upload quarantine | editor|owner |

## قواعد مشتركة

Fastify validation يعتمد schema نفسها ويُرفض additionalProperties. لا يمزج body مع query. semantic validators تفحص references/crosswords/source approvals. ETag للقراءات العامة؛ Cache-Control public,max-age=300 للفهرس وimmutable للـmanifest، private,no-store للتقدم والمصادقة والإدارة وروابط التنزيل. signed URLs صالحة 15 دقيقة وتسمح GET فقط للـdigest المسجل؛ refresh لا يغير manifest. لا CORS wildcard للإدارة ولا credentials wildcard.

الأساس 120/min/IP للعامة و60/min/user للمحمية؛ auth exchange/refresh 10/min/IP + 10/min/account hashed، submit الرسمي 5/min/user، admin publish 5/min/user. الحد يطبق في gateway/DB-backed limiter مشترك بين replicas؛ burst bounded لا in-memory فقط. sync ≤100 ops،256KiB؛ request عام≤1MiB؛ إنشاء pack ≤10k item وخاصية route body limit=2MiB. download-session≤100 hashes؛ كل hash يجب أن يظهر في manifest المحدد حتى لو له objectKey صالح.

JWT audience/issuer/exp/nbf/signature allowlist؛ refresh rotating opaque وحفظ hash فقط في identity.auth_sessions (محدد في schema)؛ PKCE verifier لا يسجل. authorization في كل route؛ owner لا يتجاوز منع مراجعة عمله بنفسه. admin browser عبر BFF cookies Secure/HttpOnly/SameSite=Lax + CSRF token/origin checks، Android bearer لا cookies.

POST create draft يفرض status=draft وfixtureOnly=false للproduction workspace؛ schema يسمح fixture للتطوير لكن publisher يمنعه. revision API يخصص max+1 تحت قفل puzzle row، body puzzleId/gameId مطابقان والbaseRevision عند التعارض409. reviewDigest ثابت للحقول المعرفية بعد استبعاد status. transitions موثقة في EDITORIAL_WORKFLOW؛ لا route عام لتغيير status arbitrary.

error codes: VALIDATION_FAILED(400)، UNAUTHENTICATED(401)، FORBIDDEN(403)، NOT_FOUND(404)، VERSION_CONFLICT/ID_REUSE(409)، SNAPSHOT_REQUIRED(410)، BODY_TOO_LARGE(413)، UNSUPPORTED_SCHEMA(422)، RATE_LIMITED(429)، TEMPORARY_UNAVAILABLE(503). batch sync يرجع200 للنتائج الجزئية؛ request invalid شكليًا400 قبل تطبيق أي عنصر. رفض عنصر semantic يسجل نتيجة قابلة للإعادة. transient failure على عنصر يرد503 مع إمكانية إعادة كامل batch؛ التي committed تعود duplicate.

المنافسة الرسمية في العقد الحالي choice فقط؛ لا payload solution/acceptedAnswers/correctId في start. قواعد اختيارية مستقبلًا توسع version. نهاية التحدي server time، start idempotent user+challenge. submit يقفل محاولة ويتحقق nonce/hash وexpiry ويرجع نفس النتيجة عند إعادة نفس body،409 عند body مختلف. ترتيب التعادل score DESC ثم achievedAt ASC ثم معرف داخلي لا يعرض.

GET /progress للعرض العادي؛ الاستعادة المتعددة الصفحات تستخدم /progress/snapshot وحده. jobs يحتاج جدول durable publication_jobs (محدد في schema) وليس ذاكرة process؛ نشر pack لا يكتمل بمجرد202. لا automatic external publishing من هذه الوثيقة.

## تغطية المرحلة الثانية

بناء API skeleton والتحقق من bootstrap/catalog/manifests وauth deny-by-default، لا تفعيل حسابات/نشر حزم إنتاج قبل config. Contract tests: no extra props، pagination limits، ownership، no answer leak الرسمي، hash allowlist signed URL، HTTP error schema،409 content concurrency. OpenAPI validation نتيجة مستقلة عن تشغيل خادم.

## إضافات المراجعة المقفلة

GET/PUT `/v1/me/leaderboard-profile` للموافقة وسحبها والاسم المستعار، POST `/v1/admin/leaderboard-profiles/{userId}/review` للمراجعة؛ العقود كاملة في OpenAPI. public leaderboard يعتمد JOIN على profile approved+optedIn. alias pending لا يظهر. profile الحالي لا يزامن offline وإنما setting online خاص بالمنافسة. sync changes/current أصبحت typed؛ رفض `{}` أو قيمة theme/sound غير متوافقة قبل التطبيق. عدادات النسخ جميعها safe integer 2^53−1، لا خلط مع deviceSeq.

challenge start يعيد nonce مشتقًا HMAC بمفتاح خادمي وحقول ثابتة؛ pinned_definition داخل attempt يمنع تغيير الحل بعد البداية. retry submit يقارن submission_sha256 ويعيد terminal_result المحفوظ قبل فحص expiry للمحاولة المكتملة؛ rank هو snapshot وقت النتيجة وقد يتغير ترتيب العرض الحالي. لا تخزن raw nonce. جداول leaderboard_profiles وauth_sessions/publication_jobs جزء من المخطط النهائي.

داخل batch: ID_REUSE على complete_session يرجع OperationResult.status=conflict/code=ID_REUSE/currentVersion=null/current=null؛ HTTP409 مخصص لتعارض طلب خارج نتيجة batch. conflict CAS فقط يحمل currentVersion عددًا. هذا يفصل تعارض معرف الجلسة عن تعارض نسخة حقل قابل للتحرير.
