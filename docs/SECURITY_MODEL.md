# الأمان والخصوصية

## حدود الثقة

APK والعميل غير موثوقين للنقاط الرسمية؛ حزم offline تحمل الحلول فلا تعتبر سرية. API المصدر الموثوق للهوية والمنافسة، publisher وحده للنشر، توقيع manifest يثبت المصدر وSHA256 يكتشف التلف. لا وصول Android أو Next.js للـDB مباشرة. قواعد البيانات والملفات السحابية خاصة والقراءة المنشورة عبر API/روابط محدودة.

| التهديد | الضابط | اختبار سلبي |
|---|---|---|
| تلاعب manifest/chunk | Ed25519 trusted keyId + digests + lengths | تغير بايت/مفتاح مجهول/حجم كاذب مرفوض |
| تنزيل ملف خارج الحزمة | allowlist chunk hashes من manifest + URL HTTPS origins محددة | hash من حزمة أخرى/SSRF/redirect خارجي |
| traversal/ملف نشط | paths نسبية strict، no symlinks/SVG/HTML/code | ../ وdouble encoding وMIME mismatch |
| استنزاف مساحة/ذاكرة | reservation، streaming، pixel≤16MP/side≤4096، document limits | أبعاد ضخمة، safe-integer overflow، nesting>32 |
| replay/تكرار مكافأة | operation/session/reward uniqueness في transaction |100 retry وجهازان |
| سرقة بيانات لاعب | RLS + scoped queries + token issuer/audience | مستخدمA يطلب IDs مستخدمB |
| تحرير دون مراجعة | digest-bound review + immutable revision + role | editor publishes own draft |
| حقن SQL/XSS | parameters؛ admin text sanitized؛ no remote WebView | query payload وHTML في التفسير |
| أسرار في APK/Git | server config فقط + secret scanning + release strings scan | patterns DATABASE_URL/private keys |

TLS فقط مع platform trust؛ لا TrustAll أو cleartextTraffic. لا certificate pinning صلب يمنع العمل عند التجديد في v1؛ دعم CA النظام وhostname verification. DB TLS verify-full، أسرار least privilege في مزود الاستضافة بعد تحديده. توقيع API tokens وmanifest مفاتيح منفصلة؛ لا توليد أي مفتاح في هذه المرحلة.

OIDC provider يتحدد مع الاستضافة؛ البروتوكول محسوم Authorization Code+PKCE. Android يتبادل عبر backend ويحتفظ access token في الذاكرة وrefresh مشفرًا بمفتاح AES-GCM في Android Keystore، rotation+family revocation. optional login لا يمنع استخدام ضيف. إعادة المصادقة للعمليات الحساسة مثل الحذف؛ لا اعتماد على البريد كدليل وحيد على ملكية حساب.

admin عبر SameSite/HttpOnly cookies في BFF وCSRF/origin validation؛ scopes لا تستنتج من UI. معدل requests موزع بين replicas؛ requestId لا يحتوي email أو token. أخطاء عامة لا stacktrace. uploads quarantine + mime/pixel/AV scan عند الدعم + metadata stripping، ثم اعتماد حقوق المصدر؛ تسجيل media objectKey لا يقره للنشر.

## تقليل البيانات

لا موقع/جهات اتصال/ميكروفون/اسم حقيقي إلزامي/advertising ID. alias اختياري يولده المستخدم مع moderation قبل ظهور الصدارة، لا ربط بالبريد. guest data محلي فقط. analytics off افتراضيًا، aggregate opt-in بلا نص أجوبة أو آيات/محتوى شخصي. log IP operational يحتفظ 7 أيام بعد إخفاء جزء منه؛ access logs30 يومًا دون Authorization/URLs الموقعة؛ audit365 يومًا بلا body شخصي. auth refresh expires30 يومًا وaccess15 دقيقة، يمكن السحب مبكرًا؛ أجهزة غير نشطة لا تحذف dedup الخاص بها.

sync_changes90 يومًا؛ snapshots24h؛ receipts/progress lifetime account؛ transcript منافسة30 يومًا ثم digest/verdict ضمن محاولة يحتفظ به لمنع إعادة التقديم. طلب حذف الحساب يحجب access فورًا، active rows≤30 يومًا، backups expire≤90 يومًا وفق مزود الاستضافة المختار؛ فترة الاحتفاظ النهائية تُعلن قبل production. عند حذف الحساب audit actor يصبح null ويحتفظ بوقائع سلامة المحتوى دون معرف شخصي.

للأطفال: لا دردشة/مراسلات/إعلانات مستهدفة؛ الصدارة والمزامنة اختيارية وغير مفعلة افتراضيًا؛ review قانونية لسياسة السن/موافقة ولي الأمر قبل إطلاق حسابات القاصرين في الأسواق المستهدفة. لا تدعي الوثيقة استيفاء تشريع بعينه. التصنيف العمري وسياسة الاسم/الصدارة قرار إطلاق لا حاجز أمام بناء core offline.

اختبارات الأمن في CI: secret scan، dependency scan/SBOM، SAST، SQL ownership، authorization matrix، fuzz schema/paths، fastify rate/replay، build artifact scan. كل findings عالية/حرجة تمنع release؛ وثيقة threat model تتحدث عند إضافة نوع ملف/خدمة/إعلانات.
