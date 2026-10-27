# لُغْزَدان | LUGHZDAN

تطبيق ألعاب عربية أصلي Kotlin/Compose، مع محرك pure Kotlin وخادم Fastify وPostgreSQL. الأساس البرمجي للمرحلة الثانية اجتاز بناء Android واختبار اللعب دون اتصال على محاكي؛ آخر نتائج التحقق وحدود الاعتماد في docs/TEST_RESULTS.md. لا إصدار إنتاجي أو APK معتمد بعد.

## التشغيل

Android: JDK17 وAndroid SDK36، ثم:

```bash
cd apps/android
./gradlew :app:assembleDebug :core:game-engine:test :data:testDebugUnitTest :app:testDebugUnitTest :core:datastore:testDebugUnitTest :core:network:testDebugUnitTest :app:lintDebug
```

نسخة debug تتضمن أربعة ألغاز مفردات مؤلفة للاختبار ومعلّمة غير معتمدة علميًا. release لا يحتوي مسودات؛ يحتاج حزمة لغوية معتمدة قبل الإطلاق. لا تحتاج اللعبة الأساسية اتصالًا أو حسابًا.

Backend (Node22+):

```bash
cd services/api
npm ci
npm run typecheck
npm run build
npm test
```

في بيئة لا تسمح بتشغيل مستخدم PostgreSQL native: `TEST_DATABASE_ENGINE=pglite npm test`. ضع DATABASE_URL خاصًا بدور LOGIN محدود عضو api_catalog في بيئة الخادم، ثم `npm start`. لا تضعه في Android. [إعداد الخادم](docs/BACKEND_SETUP.md) و[الترحيلات](docs/DATABASE_MIGRATIONS.md).

## المكونات الفعلية

- لعبة مفردات باختيارات، تلميح، نقاط، نتيجة وتفسير، إعادة محاولة وسجل.
- Room: جلسات وإصدارات مثبتة وتقدم وإنجاز أول فوز وreceipt محلي ذرّي.
- DataStore: مظهر وصوت. Compose RTL/Material3 والتنقل.
- JSON Schema وSHA256 للحزمة الأساسية. التنزيلات الخارجية والمزامنة مؤجلة صراحة.
- API قراءات منشورة من PostgreSQL مع validation وpagination وRLS.

[تقرير المرحلة الثانية](docs/STAGE_02_REPORT.md) · [نتائج الاختبارات](docs/TEST_RESULTS.md) · [الوحدات](docs/ANDROID_MODULES.md) · [الأمان](docs/SECURITY_NOTES.md).

مستودع المشروع: https://github.com/Mtzallqmy/Loazdan، فرع التنفيذ `feat/stage-02-foundation`. مشروع Neon المخصص `young-hall-20561827`، قاعدة `lughzdan`؛ اختبار المرحلة2 في `stage02-integration` فقط. لا استضافة API منشورة حاليًا. تقاريرStage01 تاريخية؛ تقاريرStage02 تحدد وضع التنفيذ الحالي.
