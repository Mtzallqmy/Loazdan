# وحدات Android المنفذة

المشروع `apps/android`، minSdk26 وcompile/target36، Java17، Gradle8.13، AGP8.13.2، Kotlin/Compose compiler2.2.21. النسخ مقفلة في version catalog؛ إصدارات مكتبات Android التي لم تنتقل إلى catalog مثبتة دون + أو latest.

| الوحدة | التنفيذ |
|---|---|
| core:game-engine | pure Kotlin، choice registry/reducer/codec/normalization/bundled pack verifier |
| domain | ProgressRepository، GameSessionManager، PlayRecord/Progress/InstalledPack |
| core:database | Room v1، ستة جداول وDAOs وفهارس؛ exportSchema |
| core:datastore | تفضيلات المظهر والصوت، DataStore Preferences |
| core:network | Retrofit/OkHttp، HTTPS فقط، مهلة10/15/25 ثانية، لا retry تلقائي |
| core:designsystem | Material3، RTL، ألوان فاتحة/داكنة |
| data | تهيئة الحزمة والتحقق، Room transaction للجلسة/التقدم/receipt، عقد worker لفحص الفهرس |
| feature:games | ViewModel/StateFlow، الرئيسية والفهرس والجلسة/نتيجتها والسجل والمكتبة والإعدادات |
| app | Hilt composition root، Activity، manifest، حزمة مسودات debug فقط |

لا وحدات فارغة للألعاب المستقبلية. domain لا يعرف Room أو HTTP. feature لا يستورد data. app يركب المحولات، والمحرك لا يعتمد Android.
الشاشة الأولى تهيئة محلية بلا شبكة. اختيار اللغز ثم اختيار إجابة لا يحتسب محاولة حتى تحقق. الثلاث الخاطئة تنهي الجولة؛ إعادة المحاولة جلسة مستقلة. النتائج تبقى ضمن route الجلسة فتستعاد دون اعتماد navigation event عابر.
