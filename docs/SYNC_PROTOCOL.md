# بروتوكول المزامنة v1

Guest له profileId/deviceId عشوائي محلي، بلا Android ID أو advertising ID. الحساب الاختياري OIDC Authorization Code + PKCE من متصفح النظام؛ bearer قصير العمر. الخادم يطابق issuer+subject إلى users.id ولا يقبل userId من body. الجهاز يسجل authenticated، unique(userId,deviceId). account linking لا يثق بمعرف ضيف: بعد auth يعرض استيراد التقدم المحلي ويُرسل كأحداث self-reported؛ لا يرقى إلى نقاط رسمية.

## Push

العقد `contracts/sync.schema.json`. كل operationId UUID وdeviceSeq متزايد، deviceId مسجل. max100/طلب و256KiB. Room يحفظ state+progress+outbox في transaction. إرسال JSON من outbox لا يعاد توليده بمعرف جديد بعد timeout. الخادم transaction لكل عملية (batch جزئي مقصود): قفل user sync clock، تحقق device ownership، insert sync_operations UNIQUE(userId,operationId) وUNIQUE(userId,deviceId,deviceSeq)، payloadHash من JCS(kind+payload+deviceId+seq). conflict نفس id مع نفس hash → cached result؛ مختلف → 409 ID_REUSE بلا تطبيق. response لكل عملية accepted/duplicate/rejected/conflict مع serverVersion.

لا يُستخدم رقم أكبر sequence لإسقاط الأرقام الأصغر: تصل 8 قبل7 وتُقبل الاثنتان إذا لم تتكررا؛ يمنع gaps من فقد التقدم. عند overflow يطلب جهاز جديد عبر التسجيل لا إعادة عداد جهاز قديم. unknown puzzle/revision → rejected UNKNOWN_CONTENT مع إمكانية إعادة التنزيل، لا حذف محلي. عمليات permanently rejected تنتقل deadletter مرئي ولا تعاد بلا نهاية. transient DB failures لا تسجل ACK نهائيًا.

`complete_session`: sessionId فريد للمستخدم؛ نفس sessionId بمحتوى مختلف مرفوض حتى لو operationId جديد. يدقق ranges/version/outcome ويربط puzzle revision ثم يحفظ self_reported فقط. bestScore=max، completed=OR، completedAt أول server receipt، attempts=عدد sessionIds المميزة. لا trust لوقت العميل. لا مكافآت مالية/عملات؛ achievement الفريد يقيد PK(userId,achievementId)، reward_ledger له UNIQUE(userId,rewardKey) في نفس transaction لمنع الصرف مرتين عبر operationId أو sessionId مختلفين.

`save_word`: CAS baseVersion؛ conflict يرجع القيمة الحالية/version. `set_preference`: CAS لكل key؛ theme يسمح light/dark/system، sound/reducedMotion يسمح on/off فقط؛ لا global LWW بالساعة. unresolved conflict يعرض اختيار المحلي/السحابي؛ القرار يولد operation جديدة مع baseVersion الأحدث. bookmarks tombstone saved=false يحتفظ به كسجل لا حذف يختفي عن جهاز قديم.

## Pull وترتيب commit

sync_changes لكل مستخدم serverVersion تصاعدي؛ يمنع استخدام sequence global كcursor لأن transaction قد تتأخر. داخل كل transaction: SELECT user_sync_clock FOR UPDATE، increment counter، كتابة projection+changes+ack ثم commit. الترتيب per-user بهذا القفل يعكس commits ويمنع skip. GET changes(cursor,limit<=100) يرجع changes مرتبة + nextCursor + hasMore؛ token opaque موقّع مرتبط userId/version/snapshot. الحفظ في Room والتحديث cursor بنفس transaction.

السجل التفصيلي changes يحتفظ 90 يومًا. expired cursor →410 SNAPSHOT_REQUIRED. snapshot يبدأ بقفل user clock وقراءة highWater داخل repeatable-read، ثم يكتب payload ثابتًا في sync_snapshots قبل release، TTL24h؛ صفحات لاحقة من هذا snapshot لا جدول حي. عند آخر صفحة يعتمد client highWater ثم يسحب ما بعده؛ outbox غير المعترف به يبقى ويعاد تطبيقه فوق snapshot. عدد changes/version يمكن أكثر من صف وله PK(userId,version,ordinal)؛ cursor يقف عند نهاية version فقط.

sync_operations receipts/operation hashes تحتفظ عمر الحساب لمقاومة replay، مع إمكان أرشفة partition دون إزالة uniqueness؛ sessions وreward uniqueness لا تحذف قبل حذف الحساب. للحد من النمو max 10k عمليات/يوم للمستخدم ومراقبة الحجم؛ لا تنظيف 90 يومًا للـdedup. account deletion يسحب tokens والجهاز ثم يحذف بياناته ≤30 يومًا؛ حذف الحساب دائم لا يعاد استخدام userId.

## المنافسة اليومية

تحدي practice محلي ينشر مسبقًا ويمكن لعبه offline بلا ترتيب رسمي. الرسمي يتطلب حساب وموافقة على الظهور؛ server dates UTC ومفتاح challengeDate، عرض Asia/Aden. start يصدر attemptId/nonce/expiresAt ويربط المستخدم؛ الأسئلة تُسحب من bank غير موجود في packs وتنزع الحلول. submit يحوي answer IDs/transcript فقط، لا score موثوق. server evaluates/locks attempt، rewardKey=`daily:{challengeId}`، leaderboard PK(challengeId,userId). تحديات الرسمي النصية/choice في v1 لتكافؤ Kotlin/TS؛ لا ألعاب زمنية تنافسية في الخلفية. API لا يدعي منع معرفة الحل عبر مشاركة بشرية؛ يحد replay ويصنف المحاولات المشبوهة.

حالات اختبار: ضياع response بعد commit، 100 retry، جهازان يحلان نفس اللغز، seq معكوس، إعادة session بمعرف operation جديد، إعادة achievement، clock skew، expiry snapshot، guest upgrade مرتين، userId مزور. النتيجة المطلوبة سجل واحد للحدث/المكافأة وعدم فقد التقدم المحلي.

## تفاصيل العقد النهائية

serverVersion/baseVersion/highWater جميعها0..9007199254740991 وتُخزن Long/bigint مع CHECK مطابق؛ deviceSeq مجال مستقل1..2147483647. absent CAS row له version=0؛ حفظ saved=false ينشئ tombstone نسخة>0. OperationResult يشمل kind/currentVersion/current؛ conflict يرجع current typed (SavedWord/Preference) أو null إذا absent، مع currentVersion=0. currentVersion لا يستبدل version العام في cursor. complete_session لا يملك CAS وcurrent=null. Change.value typed بحسب entityType؛ يطابق value.serverVersion رقم Change.version، ويطابق entityId المعرف (puzzleId/achievementId/vocabularyId/key). snapshot يستعمل آخر serverVersion لكل كيان لا highWater لكل الصفوف. theme لا يقبلon/off وsound/reducedMotion لا يقبلانdark.

official start يحفظ pinned_definition وpinned_rules_version وnonce_key_id. nonce هو HMAC-SHA256 لخماسية ثابتة length-prefixed(userId,attemptId,challengeId,definitionDigest,expiresAt) بمفتاح خدمة keyId؛ يعاد إنتاجه لretry دون تخزين raw nonce. المفتاح يحتفظ حتى انتهاء كل المحاولات المرتبطة؛ هذه مواصفة وليست قيمة مفتاح. submit canonical digest يشمل attemptId/challengeId وanswers مرتبة بمعرف السؤال مع رفض duplicate question IDs، ولا يدخل nonce ضمن digest. submitted attempt: تحقق auth والملكية ثم nonce ثم قارن digest وأعد terminal_result نفسه حتى بعد expiry/حذف transcript؛ المختلف409. محاولة غير submitted فقط تفحص expiry ثم التقييم وتخزين submission_sha256/state/terminal_result/reward بنفس transaction.

الصدارة: GET/PUT /v1/me/leaderboard-profile؛ opt-in يتطلب alias3..24 وmoderation. تغيير alias يعيده pending ويخفي العرض؛ opt-out يخفي كل entries فورًا داخل معاملة أو query join مع profile.opted_in. مراجعة /v1/admin/leaderboard-profiles/{userId}/review تخص owner. GET leaderboard يفلتر profile.opted_in AND moderation_status=approved ولا يعتمد displayName من OIDC أو entries.opted_in وحده. offline achievements لا تحتاج alias.
