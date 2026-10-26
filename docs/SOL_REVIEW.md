# مراجعة Sol 6.1 — مرحلة التأسيس الهندسي

## الحالة النهائية بعد إعادة المراجعة

أعيدت قراءة التصحيحات في 2026-10-26. **أُغلقت SOL-01 إلى SOL-08 على مستوى التصميم والعقود**، وأُغلقت ملاحظة إضافية SOL-09 ظهرت أثناء إعادة الفحص. لا يظهر مانع متبقٍ ضمن النطاق المراجع للانتقال إلى مرحلة التنفيذ. هذا اعتماد لحزمة التأسيس، وليس اعتمادًا لتشغيل الحسابات أو المنافسة أو النشر الإنتاجي.

| النتيجة | الحالة النهائية والدليل |
|---|---|
| SOL-01 | مغلقة: trigger يسمح بتصفير author_id وحده ويقارن بقية الصف كاملًا؛ تعديل snapshot المنشور يظل محظورًا. لم يختبر FK delete فعليًا. |
| SOL-02 | مغلقة: manifest.assets وmanifest.references registry موقّعان؛ توثق دلالات path/hash وحقوق النص وحل UUID عبر dependency closure. مثال pack محلي يختبر الربط والحجم وhash. |
| SOL-03 | مغلقة: Change.value مرتبط بنوع entity وSavedWord/Preference معرفان؛ OperationResult يضيف kind/currentVersion/current مع قواعد CAS وabsent row وتطابق النسخ في البروتوكول. |
| SOL-04 | مغلقة: attempt يحفظ submission_sha256 وterminal_result وpinned_definition وpinned_rules_version وnonce_key_id؛ البروتوكول يحدد إعادة إنتاج nonce والتحقق من المحاولة المكتملة قبل expiry والاحتفاظ بنتيجة نهائية. |
| SOL-05 | مغلقة: baseVersion وserverVersion وhighWater متوافقة ضمن safe integer حتى 2^53−1؛ CHECK مطابق في عدادات SQL، وdeviceSeq يحتفظ بمجاله المستقل. |
| SOL-06 | مغلقة: الشروط الآلية ترفض sound=dark وتفصل theme عن مفاتيح on/off في الطلب والكيان المسحوب. |
| SOL-07 | مغلقة: leaderboard_profiles ومسارات القراءة/الموافقة/سحبها والمراجعة موجودة؛ pending مخفي، والقراءة العامة تفحص profile.opted_in وmoderation_status، مع منع fallback إلى OIDC displayName. |
| SOL-08 | مغلقة تصميميًا: triggers تحمي parent ومحتويات الحزمة وتبعياتها بعد النشر، مع انتقال withdraw منفصل عن تعديل manifest. صلاحيات runtime واختبارات DB تظل من قبول مرحلة التنفيذ كما كان محددًا. |
| SOL-09 | مغلقة: تعارض شرط complete_session/currentVersion=null مع شرط conflict/integer عولج بقصر شرط integer على نوعي CAS. أُعيد serverVersion nullable لرفض العملية قبل تخصيص نسخة. |

أُعيد تشغيل `tools/validate.py` فعليًا بعد التصحيح الأخير: **53 فحصًا ناجحًا، صفر فشل؛ 42 جدولًا، 179 SQL statement محللًا، و42 عملية API**. بالإضافة إلى ذلك نُفذ فحص مستقل لمصفوفة OperationResult: complete_session+conflict يقبل currentVersion=null ويرفض0؛ save_word/set_preference+conflict يقبلان0 ويرفضانnull، بما يطابق دلالة CAS. كانت نتيجة السكربت قبل إصلاح SOL-09 هي51/51، ما يوضح أن النجاح البنيوي وحده لم يكن يغطي مصفوفة حالات العملية كاملة؛ أضيف الاختباران لاحقًا.

لم تنفذ قاعدة PostgreSQL أو Android أو خادم HTTP أو توقيع فعلي. لذلك إغلاق النتائج يعني أن التناقضات المعروضة حُسمت في الملفات، ويظل إثبات المعاملات وRLS وحذف الحساب وتوقيع الحزمة والتعافي من الانقطاع من اختبارات مرحلة التنفيذ. لا يوجد ادعاء بمكافحة replay مجرّبة على خدمة عاملة.

## سجل الجولة الأولى — محفوظ كتاريخ، لا يمثل الحالة الحالية

التاريخ: 2026-10-26. النطاق: قراءة SQL وOpenAPI وJSON Schemas وبروتوكولي الحزم والمزامنة والخصوصية، مع تحقق بنيوي محلي فقط. هذه مراجعة للحزمة التصميمية وليست شهادة أمان لخادم أو تطبيق منفذ.

النتيجة: الأساس متماسك في فصل اللعب المحلي عن المنافسة الرسمية، وقفل ترتيب commits لكل مستخدم، وإبقاء receipts عمر الحساب، والتثبيت بمرحلة staging ثم تبديل pointer في Room. يلزم إغلاق الملاحظات التالية قبل تنفيذ المسارات المعنية؛ أهمها تعارض حذف المؤلف مع trigger عدم التعديل، وغياب ربط أصل الصورة بمسارها داخل الحزمة.

## نتائج تمنع تنفيذ السلوك الموصوف

### SOL-01 — P1: حذف حساب مؤلف لمحتوى منشور يتعارض مع trigger

الدليل: `database/001_blueprint.sql:193` يحدد `puzzle_versions.author_id ... ON DELETE SET NULL`، بينما الدالة عند السطر 593 تمنع **أي** UPDATE إذا كان `OLD.status='published'`.

عند حذف مستخدم ألّف نسخة منشورة، ينفذ FK تحديثًا لتصفير `author_id`، ثم يرفضه trigger. بذلك يتعطل حذف الحساب خلاف سياسة الحذف والتجهيل في `SECURITY_MODEL.md` و`DATABASE_SCHEMA.md`. هذا استنتاج من قواعد SQL؛ لم ينفذ سيناريو الحذف على قاعدة بيانات.

التصحيح المطلوب: فصل هوية المؤلف القابلة للتجهيل عن snapshot المنشور، أو السماح فقط بانتقال `author_id` إلى NULL مع بقاء جميع حقول المحتوى والهوية والإصدار وdigest والحالة دون تغيير. يجب أيضًا تحديد قواعد حذف الهوية في المراجعات وaudit دون فتح باب تعديل المحتوى.

قبول التصحيح: حذف مستخدم يملك نسخة منشورة ينجح؛ تبقى definition/digest/publication والحقوق كما هي؛ تعديل حرف أو تغيير revision أو status المنشور يظل مرفوضًا.

### SOL-02 — P1: لا يستطيع renderer حل assetId إلى ملف محلي من عقد الحزمة

الدليل: `puzzle.schema.json` يرسل `image_guess.payload.assets[].assetId` فقط مع alt. `manifest.schema.json:34` يصف الملفات بـpath/kind/hash/bytes/chunks، بلا assetId. الوثائق لا تعرف اسمًا حتميًا للملف مشتقًا من UUID، ولا registry إنتاجيًا داخل الحزمة. `examples/media/asset-register.json` مثال مستقل وليس ملفًا معرفًا في عقد الحزمة.

وجود mapping في SQL أو سجل build لا يكفي لتشغيل صورة offline على الهاتف. المشكلة نفسها تخص مادة المصدر: `sourceRefs` تحيل UUID، بينما rights/*.txt بلا عقد يربط المرجع بالنص المنقول/الاعتماد.

التصحيح المطلوب: إما تعريف manifest assets/references registry موقّع يربط UUID بـpath/hash ومعرف حزمة التبعية، أو فرض مسار حتمي موثق ومتحقق منه. يجب أن يحمل الهاتف هذا الربط كاملًا ويحلّه دون استدعاء API. لا يكفي اعتبار الرابط الشبكي بديلًا عن attribution المحلي.

قبول التصحيح: حزمة صورة مع dependency تحمل asset واحدًا تُثبت بلا شبكة ثم تعرض الصورة والاعتماد من UUID؛ UUID مجهول أو ربط خارج dependency closure مرفوض قبل commit.

### SOL-03 — P1 قبل تفعيل sync: قيم pull وCAS غير محددة آليًا

الدليل: `openapi.json` يعرّف `Change.value` كـ`{"type":"object"}` و`OperationResult.current` ككائن أو NULL بلا schema. لا توجد عقود `SavedWord` أو `Preference` للرد، ولا تحديد إلزامي لحقول version داخل current. snapshot يعيد استخدام Change نفسه.

اختبار محلي أكد أن change من نوع saved_word وقيمة `{}` يجتاز JSON Schema. عميل Android وخادم TypeScript يمكن أن يختلفا في أسماء حقول saved/version والقيم والحذف دون أن تكشفه contract tests. هذا يمنع بناء restore وconflict UI مستقلين على العقد الحالي.

التصحيح المطلوب: discriminated union لكل entityType مع value محدد وadditionalProperties=false؛ ومخطط current مطابق لنوع العملية مع currentVersion صريح. تحديد absent row وbaseVersion=0 وتمثيل tombstone وversion لكل كيان. احتفظ بـordinal وترتيب version كما هو.

قبول التصحيح: payload فارغ أو ناقص أو مخالف لنوع entity يفشل؛ snapshot يحتوي التقدم والإنجازات والإعدادات والحفظ والتراجع بصيغة واحدة؛ تعارض CAS يكفي وحده لبناء قرار إعادة المحاولة.

### SOL-04 — P1 قبل تفعيل المنافسة: حذف transcript يفقد دليل إعادة الإرسال

الدليل: `SECURITY_MODEL.md` يقرر حذف transcript بعد 30 يومًا وإبقاء digest/verdict. `API_CONTRACT.md` يعد بنفس نتيجة submit عند نفس body و409 عند body مختلف. `play.challenge_attempts` لا يحتوي submission hash دائمًا؛ الأعمدة الموجودة nonce_hash وtranscript وverified_score لا تحفظ بصمة body بعد تصفير transcript.

التصحيح المطلوب: إضافة بصمة canonical submission ثابتة ونتيجة نهائية قابلة للإعادة وحالة terminal واضحة؛ فصلها عن transcript ذي TTL. ربط المحاولة بإصدار أسئلة وقواعد ثابت لا يتغير بعد start. عرّف ترتيب الفحوص: محاولة submitted تقارن hash وتعيد النتيجة أولًا، ثم يطبق expiry على المحاولة غير المقدمة، كي لا يفسد retry المتأخر بعد timeout.

ملاحظة مرتبطة: start يوصف بأنه يعيد المحاولة نفسها مع nonce، لكن SQL يخزن nonce hash وحده؛ حدّد آلية إعادة إصدار nonce دون إبطال رد سابق صحيح أو وسيلة آمنة لإعادة إنتاج nonce. لا تخزن سرًا خامًا لمجرد سد هذه الفجوة.

قبول التصحيح: submit بعد commit وفقد الرد يعود بالنتيجة نفسها، بما في ذلك بعد expiry وبعد حذف transcript؛ body مختلف يرفض دائمًا ولا يمنح reward ثانية.

## تناقضات أصغر يجب حسمها في العقد

### SOL-05 — P2: مجال CAS أضيق من مجال النسخة التي يستقبلها العميل

`sync.schema.json` يقيد baseVersion إلى 2147483647، بينما Progress.serverVersion وChange.version وOperationResult.serverVersion تصل إلى 9007199254740991 وSQL يستخدم bigint. Achievement.serverVersion وحده مقيد إلى 2147483647 أيضًا. بعد تجاوز السقف لا يمكن للعميل إرسال نسخة قرأها في CAS. وحّد المجال والتمثيل لجميع حقول serverVersion/baseVersion، مع سقف صريح للعداد في DB أو string decimal إن تقرر دعم bigint الكامل. deviceSeq يمكن أن يبقى بحده المنفصل مع تدوير deviceId الموثق.

### SOL-06 — P2: عقد preference يقبل حالة ترفضها SQL

الاختبار المحلي أكد قبول `{key:"sound", value:"dark", baseVersion:0}`. SQL وSYNC_PROTOCOL يسمحان sound/reducedMotion بـon/off فقط وtheme بالقيم الثلاث الأخرى. أضف if/then أو oneOf إلى schema ومثيلها المضمن. لا يوجد ادعاء بأن هذا يتجاوز الخادم؛ هو تناقض validation وإمكانية توليد عميل غير صحيح.

### SOL-07 — P2 قبل نشر الصدارة: لا عقد للموافقة أو alias المراجع

السياسة تشترط alias اختياريًا مع moderation وموافقة على الظهور. SQL يبدأ `leaderboard_entries.opted_in=false`، وstart/submit لا يقبلان consent/alias ولا يوجد مسار تغيير/سحب موافقة أو مخزن لحالة مراجعة alias. لا تستخدم display_name القادم من OIDC كبديل تلقائي. أضف عقدًا صريحًا للموافقة والاسم المستعار ومراجعة الاسم وسحب الظهور، أو أبق الصدارة معطلة حتى توفيره كما تسمح رؤية المنتج.

### SOL-08 — P2: بعض ضمانات immutability مؤجلة دون آلية مماثلة

SQL يحمي puzzle_versions المنشور فقط. content_pack_versions.manifest/hash/object_key/items/dependencies قابلة للتعديل بحسب DDL الحالي. الصلاحيات والسياسات مؤجلة إلى migration 002 بوضوح، لذا هذا ليس ادعاء بوجود تسريب منشور. يجب أن تشمل acceptance المرحلة الثانية منع تحديث المحتويات المشار إليها بعد النشر مع السماح بwithdraw منفصل، وألا يعتمد publisher على convention وحده. كذلك ledger append-only وقيود ownership تحتاج اختبارات runtime فعلية عند توفير roles/policies.

## نقاط تمت مراجعتها ولم يظهر فيها تعارض مانع

- تطابق schemas الثلاثة المستقلة مع النسخ المضمنة في OpenAPI، بعد استبعاد `$schema` و`$id` الوصفيين.
- UUID وهوية الجهاز وقيود operationId/deviceSeq/sessionId/rewardKey متوافقة في التصميم. عدم إسقاط sequence أصغر عند وصولها متأخرة يمنع فقد أحداث الجهاز غير المرتبة.
- قفل user_sync_clock قبل كتابة projection/change والاحتفاظ بcursor عند نهاية version يعالجان خطر اختلاف ترتيب تخصيص sequence عن commit.
- snapshot ثابت وhigh-water مع إبقاء outbox غير المعترف به، وTTL منفصل عن receipts، خيارات مناسبة لاستعادة متعددة الصفحات.
- chunk hashes والحجم النهائي والتوقيع ونسخة manifest مثبتة تمنع استبدال bytes أثناء تجديد روابط التنزيل. semantic checks لازمة للأحجام والمراجع ومجموع chunks وتكرار المسارات؛ JSON Schema وحده لا يثبتها.
- بروتوكول staging → final rename → Room pointer يحدد commit point وتعافيًا واضحين؛ pins وGC المنفصلان يحميان جلسة مفتوحة وتبعياتها. يلزم اختبار انقطاع العملية عند كل حد fsync/rename/transaction في التنفيذ.
- بيانات الضيف محلية، telemetry اختيارية، والأحداث المنزلية self-reported منفصلة عن التقييم الرسمي. سياسات RLS الحالية deny-by-default دون ادعاء أنها سياسات تشغيل مكتملة.

## التحقق الذي نُفذ فعلًا

بيئة محلية فقط باستخدام Python مع `PYTHONPATH=/workspace/scratch/097e3d137fa1/.validation_deps`:

| الفحص | النتيجة |
|---|---|
| `openapi_spec_validator.validate_spec` على OpenAPI 3.1 | نجح |
| `Draft202012Validator.check_schema` للمخططات الثلاثة | نجح |
| مقارنة schemas المستقلة بالمضمنة | متطابقة |
| `pglast.parse_sql` | نجح تحليل 171 statement |
| أمثلة الألغاز التسعة مع FormatChecker | صفر أخطاء بنيوية |
| مثال sync-request مع FormatChecker | صفر أخطاء بنيوية |
| حالة sound=dark | قُبلت؛ تؤكد SOL-06 |
| Change saved_word بقيمة فارغة | قُبل؛ يؤكد SOL-03 |

فشل تشغيل فحص أولي بسبب اسم component افتراضي خاطئ في سكربت الفحص (`PuzzleDefinition` بدل `Puzzle`)، ثم صُحح السكربت وأعيدت جميع الفحوص أعلاه بنجاح. لم تعدل العقود لإخفاء الخطأ.

لم تُشغل PostgreSQL أو SQLite أو Android أو HTTP server، ولم تختبر سياسات RLS أو معاملات replay أو توقيع Ed25519 أو انقطاع install فعليًا. نجاح SQL parser ليس نجاح migration أو قيود تشغيلية. لم يحصل اتصال بخدمة خارجية أو تعديل GitHub/Neon في هذه المراجعة.

## قرار الجولة الأولى — تاريخي

الحزمة مناسبة لمواصلة foundation والـcore offline بعد حسم SOL-01 وSOL-02. لا يوصف sync أو المنافسة أو الصدارة بأنها جاهزة للإنتاج قبل إغلاق ملاحظاتها وتشغيل اختبارات التنفيذ المذكورة. المخاطر المذكورة قابلة للإصلاح داخل العقود والتصميم الحالي دون تغيير رؤية المنتج أو إدخال خدمة جديدة.

## قرار المراجعة النهائي

مقبول للانتقال إلى تنفيذ المرحلة التالية؛ أُغلقت نتائج المراجعة السابقة والتناقض الإضافي في OperationResult. يحافظ المشروع على gates الإنتاج المعلنة: roles/policies واختبارات ownership/replay على PostgreSQL حقيقي، وحذف حساب مؤلف، واختبارات Android للتوقيع والتحميل والتثبيت الذري، وتكافؤ التقييم قبل تفعيل المنافسة. لا تتطلب هذه الخاتمة خدمة جديدة أو اتصالًا خارجيًا ضمن مرحلة التأسيس.
