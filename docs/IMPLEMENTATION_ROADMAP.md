# خطة التنفيذ واعتماد المراحل

هذه الوثيقة تكمل الخطة المتوازنة السابقة المؤرخة2026-10-26 وتحسم تعارضاتها لصالح طلب المرحلة الحالية. المستودع المحدد Mtzallqmy/Loazdan وفُحص فارغًا. لم يُدفع إليه شيء لأن التعليمات الحالية تحصر العمل في بيئة الوثائق. لا اختيار استضافة ولا إنشاء Neon أو أسرار.

## توزيع العمل المعتمد

| المرحلة | النطاق الملموس | المنفذ المقترح | المراجع | بوابة الانتقال |
|---|---|---|---|---|
|1 التأسيس | ملفات هذه الحزمة + عقود ومخططات وعينات وتحقيق | Astra للدور المعماري المطلوب | Sol6.1 | تقارير التحقق ومراجعة التناقضات؛ لا ادعاء تشغيل تطبيق |
|2 النواة | monorepo،Android app/core/engine،Room،9 contract adapters،Fastify skeleton،CI | Sol6.1 | Astra عند حدود المعمارية | A01 بمحركين أولًا ثم compile registry لكل الأنواع؛ auth deny-by-default |
|3 مدينة الكلمات | crossword/wordsearch/anagram/order/text،مولد شبكات،معجم | Sol6.1 | Astra للخوارزميات،Luna لتوسعة fixtures غير المعتمدة | property tests + 5 ألعاب فعلية دون شبكة |
|4 الخيال والمنطق | image_guess/matching/choice/sequence،صور مرخصة | Sol6.1 | Astra للحالات المعقدة | 9 renderers مع الوصول والتقييم الصحيح |
|5 المحتوى العلمي | شعر/بلاغة/ألفاظ القرآن ومصادر ومراجعات | Astra لتصميم الدلالة،Sol6.1 للتكامل | بشر لغوي/علمي إلزامي | حقوق وأدلة مراجعة كل item؛ لا اعتماد علمي بالنموذج |
|6 الحزم والخادم | publisher/signatures/download/Neon/API/administration/auth | Sol6.1 | Astra للأمان/الاتساق | A02–A08,A11–A13؛ fault injection |
|7 التحسين واللعب الممتد | achievements/daily/leaderboard/UX/performance | Sol6.1،Luna للمهام الصغيرة المحددة | Astra anti-cheat،Sol integration | قياسات حقيقية والأهداف متحققة أو ADR مبرر |
|8 الإصدار | regression،privacy،store readiness،signed ARM64 APK | Sol6.1 | Astra final review | security/rights/QA pass وموافقة نشر عند الحاجة |

التوزيع اقتراح تنفيذ لا ادعاء أن النماذج الثلاثة عملت كلها. المراجعة المستقلة المنفذة في هذه المرحلة تُوثق في SOL_REVIEW.md. لا تكليف Luna بتصميم التوقيع أو مصادر القرآن أو قرار صلاحيات؛ يناسب test cases/UI copy وتصنيف مسودات تحت review.

## backlog المرحلة الثانية — جاهز للتنفيذ

| ID | المهمة | ملفات الهدف | اعتماد | Definition of Done |
|---|---|---|---|---|
| S2-01 | إدخال الحزمة إلى فرع docs/stage-01 ثم scaffold بعد تفويض الرفع | docs/contracts/infra | تحديد نطاق GitHub/host | commit واضح،README،no secrets |
| S2-02 | تثبيت toolchain وdependency locks | gradle/version catalog،package lock | S2-01 | minSdk26،arm64،debug compile،no alpha |
| S2-03 | Kotlin engine contracts وtyped adapters | game/engine | S2-02 | registry9،unknown schema safe،normalizer/scoring unit pass |
| S2-04 | practice vertical slice anagram+sequence | game/anagram,sequence + feature/session | S2-03 | submit/hint/win/loss/retry فعلي |
| S2-05 | Room schema/migrations/repositories | core/database,data | S2-03 | transaction state+outbox،restore بعد kill،لا fallbackToDestructiveMigration |
| S2-06 | bundled fixture-only dev pack ومنع دخوله release | content/dev-fixtures،core/contentpacks | S2-05 | first-run airplane/A01 في debug؛ release validator يرفض fixtures |
| S2-07 | design system/navigation/accessibility | core/designsystem,feature/home | S2-04 | RTL/dark/font200/48dp،bottom actions |
| S2-08 | Fastify skeleton من OpenAPI | services/api,packages/contracts | S2-01 | validation/health/catalog repository mock معلن؛ auth routes disabled503 بدون config |
| S2-09 | migrations+access matrix للاختبار المعزول | infra/migrations | تحديد Neon بيئة اختبار | schema+RLS ownership tests؛ لا production migrations تلقائية |
| S2-10 | pack verifier/transfer interfaces وjournal reducers | core/contentpacks | S2-03,S2-05 | hash/type/path validation،signed vector API26،crash model tests |
| S2-11 | CI workflows وevidence index | .github/workflows,docs/test-evidence | S2-02 | lint/typecheck/tests/compile/security green على commit معين |

لا إعادة تصميم في المرحلة الثانية: family schema/UUID/revision/grid direction/hash/atomic commit/sync ownership/scoring كلها هنا. يمكن إنشاء skeleton للخدمات دون اتصال؛ التهيئة الفعلية للخادم/Neon تنتظر تحديد الاستضافة. لا يُعلن A01 production-approved إن كان يعمل بfixtures فقط؛ الاعتماد العلمي يأتي في مرحلة5.

## قرارات تحتاج صاحب المشروع قبل موارد/إطلاق

1. هل يرفع تسليم الوثائق إلى المستودع المحدد الآن أم مع بدء المرحلة الثانية؟ لا يمنع هذا قراءة الحزمة.
2. مزود الاستضافة/التخزين والمنطقة والميزانية؛ البروتوكول S3/HTTPS وPostgreSQL محسومان.
3. مزود OIDC واسم النطاق النهائي وapplicationId النهائي؛ مقترح package `app.lughzdan.android` لا يسجل أو يحجز الآن.
4. تكليف مراجعين لغوي وعلمي بشريين، وموافقات مصادر تحتاج تصريحًا؛ هذه بوابات محتوى لا قرارات مؤجلة في المعمارية.
5. سياسة حسابات القاصرين/الصدارة العلنية والتوزيع النهائي؛ الافتراضي guest local ومنافسة معطلة.

## قائمة تحقق الانتقال

- [x] بنية ووحدات وقواعد الألعاب والتطبيع محددة.
- [x] عقود قابلة للفحص وDDL وOpenAPI وعينات أصلية معلنة.
- [x] بروتوكولات التنزيل/crash recovery/المزامنة والأمان موثقة.
- [x] مهام المرحلة الثانية ومدخلاتها ومعايير قبولها محددة.
- [ ] مراجعة مالك نطاق الرفع والاستضافة قبل أي تغيير خارجي.
- [ ] مراجعة المصادر البشرية والحقوق قبل اعتماد المحتوى العام.
- [ ] تنفيذ Android/Backend والقياسات؛ ليست من أعمال المرحلة الأولى.
