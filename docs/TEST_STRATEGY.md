# استراتيجية الاختبار وبوابات القبول

المرحلة الأولى تشغل validators للعقود والعينات وparser للمخطط فقط. لا تطبيق مبني ولا خادم عامل ولا benchmarks منجزة؛ تقرير `reports/validation.json` يبين التنفيذ الفعلي. الكود Kotlin حدود/أمثلة، واختبار compile سيأتي في المرحلة الثانية إذا لم يتوفر compiler هنا. لا مساواة لصحة OpenAPI مع صحة خدمة كاملة.

## مصفوفة الاختبارات المطلوبة

| الطبقة | الأدوات/السيناريو | بوابة النجاح |
|---|---|---|
| domain unit | JUnit/Kotest، deterministic reducers/hints/scoring/normalization | كل transitions وgolden vectors؛ zero failing |
| property | crossword 1000 seed، replay/no double hint | لا شبكة غير صالحة تقبل، reproducible failing seed |
| Room integration | instrumented SQLite، transaction outbox/progress، migration exports | no partial save، لا destructive migration |
| Fastify | inject + real isolated PostgreSQL لاحقًا | schemas/RBAC/RLS/transactions/idempotency |
| Compose UI | 9 renderers، RTL، TalkBack، fontScale200، IME | solve/resume لكل نوع؛ لا clipped controls |
| offline | fresh install airplane، restart/reboot | الأساسي وجميع installed packs تعمل دون شبكة |
| network failure | MockWebServer،206/200/416/429/403expiry،reorder | لا فساد، لا فقد progress، resume محدود |
| pack integrity | tampered manifest/signature/hash/path/bytes/unknown key | كلها ترفض قبل activation |
| crash consistency | kill عند كل journal phase، ENOSPC، concurrent delete | نسخة سليمة واحدة فعالة؛ recovery deterministic |
| migrations | empty/latest وN−1/N، data fixture conservation | rows preserved وconstraints enforced |
| content Arabic | أ/إ/ا،ة/ه،ى/ي،diacritics،Unicode/bidi | exact policy كما العقد، no canonical mutation |
| source publishing | missing license/review/hash، author=self reviewer | النشر يرفض 100% |
| security | OWASP-style ownership،injection،rate،secret scan/SBOM | لا critical/high غير معالج |
| performance | Macrobenchmark/BaselineProfile/JankStats/Perfetto | أهداف الجدول بقياسات raw موثقة |

## سيناريوهات قبول التنفيذ

| ID | Given / When | Then |
|---|---|---|
| A01 | تثبيت نظيف في وضع الطيران → فتح → حل → force stop → فتح | الحل وأعلى نتيجة والتلميحات محفوظة؛ لا login prompt |
| A02 | تنزيل pack → قطع الشبكة وسط chunk → reconnect → resume → airplane | bytes المكتملة لا تتكرر إلا chunk متغير، hashes صحيحة، كل ألعاب الحزمة تعمل |
| A03 | قديم صالح + تحديث → kill قبل/بعد rename/Room commit | القديمة أو الجديدة كاملة، لا hybrid؛ progress unchanged |
| A04 | امتلاء storage عند التجميع/فهرسة Room | pause واضح والقديمة سليمة؛ إعادة المحاولة بعد تحرير مساحة |
| A05 | حذف pack بجلسة معلقة → إعادة تنزيل الإصدار المثبت | achievement محفوظ والجلسة قابلة للاستعادة أو طلب الإصدار، لا إعادة تفسيرها بعقد جديد |
| A06 | إرسال completion 100 مرة وتغيير operationId مع sessionId ثابت | session واحدة، attempts+1 فقط، achievement/reward واحدة |
| A07 | جهازان، seq8 قبل7، قطع response بعد commit | كلا الحدثين الصحيحين يندمجان؛ dedup سليم |
| A08 | cursor90 يوم قديم مع outbox لم يرسل | snapshot ثابت + delta + replay local؛ لا فقد تغييرات أثناء pagination |
| A09 | إجابة «علي» لـ«على» أو «رحمه» لـ«رحمة» | غير صحيحة دون approved alternative؛ النص المصدر ثابت |
| A10 | official attempt offline/expired/tampered score | لا entry رسمية؛ practice يبقى متاحًا |
| A11 | unknown schema/type أو signature غير صالحة | رفض مع سبب وتبقى اللعبة الأساسية تعمل |
| A12 | source approval مسحوب أو reviewDigest قديم | publish يرفض وسجل audit يوضح السبب |
| A13 | user A GET/POST user B ids وتبديل deviceId |403/404 حسب سياسة الحجب، صفر صفوف مكشوفة/معدلة |
| A14 | هاتف360dp/RTL/font200% وقارئ شاشة | كل تفاعل له بديل accessible وحفظ حالة صحيح |

## ميزانيات الأداء المستهدفة، غير مقاسة بعد

الجهاز المرجعي الأساسي Redmi Note 8 Pro/Android10/6GB (متوافق مع جهاز المستخدم)، وجهاز منخفض API26/3GB + emulator API26 للتحقق الوظيفي فقط. build release دون debugger، بيانات محلية120 ثم100k لغز و5GiB وسائط،30 تكرارًا، median/p95 مع حرارة وحالة بطارية ثابتة. أداء Neon cold wake منفصل عن startup offline.

| المقياس | هدف القبول | القياس |
|---|---|---|
| cold start لاحق للتهيئة | p95≤3s إلى شاشة تفاعلية | Macrobenchmark startup |
| تهيئة أول تشغيل | p95≤5s مع تقدم واضح | أول install benchmark |
| فتح لغز مثبت | p95≤300ms نصي،≤700ms صور | trace UI+IO |
| حركة60Hz | p95 frame≤16.7ms؛ jank<5% | FrameTimingMetric |
| RAM PSS | ≤180MiB نصي،≤250MiB صور | memory profiler؛ لا حزمة كاملة بالذاكرة |
| بحث100k مدخل | p95≤150ms،debounce200ms | Room/FTS4 queries |
| تنزيل | buffer≤2MiB/worker بخلاف decode؛2 concurrent | heap traces |
| البطارية |≤5%/30دقيقة لعب نصي على المرجعي؛ idle≤1%/8ساعات | Battery Historian تحت سيناريو موحد |
| APK arm64 release |30–50MiB هدف؛≤70MiB gate أولي إلا ADR مبرر | APK Analyzer/R8 |
| التخزين |عرض دقيق±1% للحجم المثبت مع فصل index/cache؛ لا حد GB مصطنع | 5GiB fixture + low disk test |

الأرقام أهداف تصميم. إذا فشل الجهاز المنخفض تخفف المؤثرات وجودة preview دون تغيير الأجوبة أو إسقاط التحقق. لا يُدعى60fps على كل جهاز دون بيانات.

## CI المقترح

PR: contracts→lint/typecheck→domain tests→API inject→Postgres integration/migrations→Android debug compile→emulator smoke→content validators→secret/dependency scan. dependencies pinned + lockfiles، GitHub Actions permissions read-only افتراضيًا، no secrets للـfork PR، actions pinned by commit SHA عند إعداد workflow الفعلي. nightly: property/fault matrix/performance، artifacts reports وSBOM بلا secrets. release: manual gate بعد نجاح كل الفحوص وحقوق المحتوى، signed APK من secret store فقط في المرحلة الثامنة أو موعد يقره المالك؛ لا APK في المرحلة الأولى.
