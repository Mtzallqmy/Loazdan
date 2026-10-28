# لُغْزَدان | LUGHZDAN

تطبيق ألعاب عربية أصلي Kotlin/Compose، مع محرك pure Kotlin وخادم Fastify وPostgreSQL. المرحلة الثالثة تضيف ست عائلات ألعاب بمحركات Kotlin مستقلة وواجهات تفاعلية. تحقق البناء والقبول الحالي موثق في docs/STAGE_03_REPORT.md؛ نتائج المرحلة الثانية محفوظة في docs/TEST_RESULTS.md. لا إصدار إنتاجي أو APK معتمد بعد.

## التشغيل

Android: JDK17 وAndroid SDK36، ثم:

```bash
cd apps/android
./gradlew :app:assembleDebug :core:game-engine:test :data:testDebugUnitTest :app:testDebugUnitTest :core:datastore:testDebugUnitTest :core:network:testDebugUnitTest :app:lintDebug
```

نسخة debug تتضمن 22 لغزًا في ست عائلات مؤلفة للاختبار ومعلّمة غير معتمدة علميًا. release لا يحتوي مسودات؛ يحتاج حزمة لغوية معتمدة قبل الإطلاق. لا تحتاج اللعبة الأساسية اتصالًا أو حسابًا.

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

- مفردات، كلمات متقاطعة، بحث عن كلمات، تركيب حروف، علاقات دلالية، وترتيب نصي؛ تلميحات ونقاط وإعادة محاولة وسجل.
- Room2: جلسات مثبتة على نسخة المحتوى، ترحيل يحافظ على بيانات Room1، XP وأفضل نتيجة وإنجاز لكل لعبة وreceipt محلي ذرّي.
- DataStore: مظهر وصوت. Compose RTL/Material3 والتنقل.
- JSON Schema وSHA256 للحزمة الأساسية. التنزيلات الخارجية والمزامنة مؤجلة صراحة.
- API قراءات منشورة من PostgreSQL مع validation وpagination وRLS.

[تقرير المرحلة الثانية](docs/STAGE_02_REPORT.md) · [نتائج الاختبارات](docs/TEST_RESULTS.md) · [الوحدات](docs/ANDROID_MODULES.md) · [الأمان](docs/SECURITY_NOTES.md).

مستودع المشروع: https://github.com/Mtzallqmy/Loazdan، فرع التنفيذ `stage-03-advanced-arabic-games`، وطلب المراجعة [#2](https://github.com/Mtzallqmy/Loazdan/pull/2) أساسه فرع المرحلة الثانية. مشروع Neon المخصص `young-hall-20561827`، قاعدة `lughzdan`؛ اختبار المرحلة3 في فرع Neon المعزول `stage03-integration`، دون تعديل فرع الإنتاج. لا استضافة API منشورة حاليًا. تقاريرStage01/02 تاريخية؛ [تقرير المرحلة3](docs/STAGE_03_REPORT.md) و[مراجعة Astra](docs/STAGE_03_ASTRA_REVIEW.md) يحددان الحالة الحالية.
