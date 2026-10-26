# المحتوى والحقوق والمراجع

المصدر ليس مجرد رابط. كل assertion مرتبط source_reference: author/edition/version/page أو sura:aya:word، license evidence، permitted uses، verification date، reviewer identity، approval. لا يكفي أن المؤلف توفي قديمًا لإثبات حقوق طبعة حديثة أو تحقيقها أو OCR أو الصور.

## سجل فحص أولي — 2026-10-26

| المصدر/الجهة | الرابط أو الطبعة | ما تحقق | الاستخدام المقترح | الحالة |
|---|---|---|---|---|
| Tanzil Project | https://tanzil.net/docs/text_license | الصفحة تسمي CC BY 3.0 وتشترط النقل الحرفي وعدم تغيير النص والإسناد والرابط وإبقاء إشعار الحقوق | نص قرآني canonical حرفي منفصل؛ فهرس بحث مشتق لا يعرض بدل الأصل | rights_verified؛ اعتماد dataset/checksums والمراجع ما زال مطلوبًا |
| تطبيق سورة | https://dev.surahapp.com/api/docs/ | صفحة Swagger متاحة، لم يثبت من الصفحة إذن إعادة توزيع المحتوى offline | مصدر مرشح فقط؛ لا bulk import أو media caching حتى نص إذن محدد | blocked_pending_permission |
| Quranic Arabic Corpus / Kais Dukes | https://corpus.quran.com/download/ | تنزيل v0.4 يذكر GNU وشروط النقل الحرفي والإسناد؛ لا نفترض أن كل مشتق مباح بلا شروط | دراسة annotation بعد مراجعة توافق الترخيص وشروطه على الملف المحدد | rights_review_required |
| لسان العرب / ابن منظور | الطبعة لم تحدد | لا إثبات ترخيص لملف/طبعة بعينها | تحقيق مدخل لغوي ومعنى مع locator بعد الحقوق | candidate |
| مفردات ألفاظ القرآن / الراغب الأصفهاني | الطبعة لم تحدد | لم ينقل نص ولم يعتمد تحقيق | معاني سياقية مع مراجعة علمية | candidate |
| غريب القرآن / ابن قتيبة | الطبعة لم تحدد | لم تنقل نصوص | شرح دلالة موثق بعد اختيار طبعة مباحة | candidate |
| نزهة الأعين النواظر / ابن الجوزي | الطبعة لم تحدد | مرشح للوجوه والنظائر لا مرجع معتمد بعد | مقارنة الدلالة مع توضيح الاختلاف | candidate |
| دلائل الإعجاز/أسرار البلاغة / عبد القاهر الجرجاني | الطبعة لم تحدد | لا افتراض إباحة طبعة رقمية | شرح أصلي مع إحالة بعد المراجعة | candidate |
| دواوين الشعر | شاعر/ديوان/طبعة لكل شاهد | لا وجود لديوان معتمد في المرحلة الأولى | أبيات تحقق نسبتها وحقوق طبعتها | candidate |
| Wikimedia Commons | https://commons.wikimedia.org/wiki/Commons:Licensing | لم يفحص ملف صورة بعينه | CC0/CC BY/CC BY-SA بعد مراجعة asset-level والتعديلات والإسناد | candidate |
| رسوم المشروع الهندسية | examples/media/three-circles.svg | رسم أصلي أنشئ للحزمة؛ لا نقل خارجي | fixture هندسي يتحول لصورة raster بعد اعتماد | fixture_only |
| original-math-fixture | ملفات الأمثلة في هذه الحزمة | مسائل حسابية أصلية مع برهان بسيط؛ لا ادعاء مراجعة بشرية | اختبار العقود فقط | draft |
| original-lexical-draft | قلم/علم في أمثلة هذه الحزمة | صياغة أصلية؛ علم متعددة الضبط والمعنى ولذلك clue يحدد معرفة | اختبار تقاطعات لا محتوى معجمي معتمد | draft |

للتوافق مع SQL: candidate/rights_verified/approved/blocked/withdrawn هي قيم approval_status؛ blocked_pending_permission وrights_review_required تسميات تقرير تتحول blocked وcandidate مع notes، وليست enum جديدة في API. sourceRefs الرمزية بالأمثلة تُحل من `examples/source-register.json`؛ الإنتاج UUID من DB فقط.

## سياسة النص القرآني

لا يكتب النموذج آيات من الذاكرة ولا يعدل رسمها. تنزيل dataset مصرح به وحفظ raw bytes/version/hash وإشعار الرخصة؛ تثبيت checksum على مستوى الآية والسورة ودراسة البسملة وترقيم الآيات قبل seed. displayed canonical text منفصل عن normalized search/answer. لا ألعاب إعادة ترتيب كلمات الآية أو قصها بطريقة تغير المعنى، ولا استبدال حرف في الآية. السؤال على المفردة/السياق، والشرح لا يدعي حصر أقوال المفسرين إذا وقع اختلاف معتبر.

أمثلة هذه الحزمة لا تحتوي آية كاملة أو نقلًا محميًا. تسليم العينات الهندسية صحيح لا يعني أن مصدرًا علميًا اعتمد. ملف source-register يثبت حدودها؛ بيانات الاعتماد البشرية لا تختلق لملء schema. بوابة الإصدار العام تتطلب عينات قرآنية وشعرية حقيقية من المصدر المعتمد ومع المراجعة؛ مرحلة التأسيس توفر العقد والسير والحقول كاملة.

## صيغة سجل المراجع

لكل reference: sourceId، locator، sourceVersion، attribution، excerptSha256 عند استيراد النص. لكل media: author، source URL، license code/version، license URL، modifications، attributionText، retrievedAt، reviewerId. الحقوق غير القابلة لإعادة التوزيع لا تدرج في pack حتى إن كان API عامًا أو الوصول مجانيًا. لا scraping يتجاوز rate limits أو شروط الخدمة.

تدفق AI: draft_generation → automated structural checks → linguistic review → scholarly review → publisher. يسمح باقتراح ألغاز وشروح أصلية ويجب وسم generatedBy/ reviewedBy في سجل التحرير؛ لا جعل إجابة النموذج مصدرًا علميًا. الاحتفاظ بحقوق المصادر وأثر التعديلات داخل الحزمة يعمل offline.

المراجع أعلاه فحص حقوق أولي لا رأي قانوني نهائي في شروط مركبة. لم تُجر طلبات موافقة أو تنزيل datasets في هذه المرحلة.
