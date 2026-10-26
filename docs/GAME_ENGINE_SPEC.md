# عقد محرك الألعاب

## دورة الحياة

`Created → Active ↔ Paused → Won | Lost | Abandoned`. restore يُعيد الحالة إلى Paused ثم resume صريح. Practice: ثلاث محاولات إرسال خاطئة، لا خصم لمجرد لمس حرف؛ بعد Lost يمكن retry بجلسة جديدة مع نفس اللغز. لا مسح أعلى نتيجة أو الإنجاز. بعد نهاية الجلسة لا تقبل أفعال إلا فتح التفسير أو retry خارجي. لا مؤقت إلزامي في التعلم؛ التحدي الرسمي له deadline خادمي منفصل.

`GameTypeRegistry` يربط type بمتحقق وتعريف serializer/reducer/evaluator/hints؛ unknown type أو schemaVersion → UnsupportedContent دون crash. `RendererRegistry` في UI يربط النوع بشاشة Compose. `PuzzleDefinition` immutable وله UUID ثابت + revision عددي موجب + rulesVersion؛ أي تعديل إجابة/مصدر/تفسير يولد revision جديدة. Semantic validator إضافي لـJSON Schema يفحص الترابط لا الشكل فقط.

الواجهات المكتملة كأمثلة في `examples/kotlin/Engine.kt`. لا I/O داخل reduce/evaluate؛ الوقت والseed بيانات مدخلة مسجلة. كل actionId UUID فريد داخل الجلسة، الأحداث المكررة no-op. StateCodec يرفض stateVersion مستقبلية ويحافظ على القديم لإعادة المحاولة دون حذف التقدم. الاسترجاع يستخدم pinned puzzle revision؛ إذا حُذفت الحزمة تُعرض إعادة تنزيلها، ولا يُفسّر state قديم بعقد مختلف.

## الإجابات والنقاط

سياسات التطبيع `exact_v1` و`arabic_basic_v1`: كلاهما NFC وtrim؛ basic يحذف التطويل والحركات U+064B..065F و0670، ويضغط المسافات، ولا يدمج أ/إ/آ مع ا، ولا ى/ي ولا ة/ه. أي بديل تسامح مضاف صراحة ضمن acceptedAnswers بعد مراجعة. العرض يحتفظ بالنص الأصلي؛ تطبيع الإجابة لا يكتب فوق النص القرآني أو المصدر. رفض bidi control characters وpresentation forms في بيانات التأليف؛ NFC لا يحول لام ألف العرضية تلقائيًا إلى خلية واحدة.

قيمة الأساس: difficulty 1..5 → 100،150،200،250،300. النتيجة = max(0, base − 10×wrongSubmissions − sum(hint.penalty)) عند الفوز، وإلا صفر. لا مكافأة زمنية في practice. hints كشف حرف/إقصاء مشتت/كشف علاقة أو خطوة، مرتبة وبمعرفات ثابتة، لا خصم ثان على إعادة نفس التلميح. جميع أنواع التلميحات deterministically seeded ومسجلة في state. لا عملات أو اقتصاد إنفاق في v1؛ يمنع مشكلة double spend دون اتصال. الإنجاز `first_win` مرة لكل مستخدم، و`ten_unique_puzzles` بعد 10 puzzleIds مختلفة؛ revise/retry لا يزيدان العدد.

التقدم المنزلي يظل self-reported؛ scoreboard لا يقبله. إعادة الحساب للخادم من transcript تستعمل rulesVersion ثابتة ومجموعة golden vectors مشتركة Kotlin/TS. اختبارات parity إلزامية قبل تفعيل leaderboard.

## الكلمات المتقاطعة

التوليد في أدوات التحرير/بناء الحزمة، لا على الهاتف في الإصدار الأول. المدخل مفردات reviewed مع vocabularyId، أصل مكتوب، gridTokens، مصدر تعريف وتلميح. لا توليد إجابات قرآنية. الأبعاد 3..15، الخلية حرف عربي base مع grapheme display؛ الحركات ليست خلايا، والشدة لا تضاعف الحرف. يحتفظ renderer بالشكل المشكول خارجه.

الإحداثيات zero-based من أعلى اليسار؛ across يبدأ عمودًا عاليًا ويمشي dc=-1 (RTL)، down يمشي dr=+1. array grid مخزن بالترتيب الفيزيائي left-to-right؛ ممنوع عكسه ثانيةً بسبب RTL العام. تختبر لوحة المفاتيح والقارئ الاتجاهين.

الخوارزمية: normalize grid candidates حسب سياسة واحدة، حذف تكرار اللفظة، ترتيب الأطول مع أقل شيوع للحروف أولًا، backtracking seeded لأماكن تقاطع أحرف متساوية. ترفض التعارض، الخروج من الحدود، parallel overlap، ملامسة جانبية تنشئ كلمات غير معرفة، الامتداد قبل/بعد الإجابة، والشبكة المنفصلة. توقف بعد 50k عقدة/ثانيتين في أداة البناء؛ إن لم تحصل شبكة صحيحة تفشل وتعرض للمحرر، لا تنشر شبكة ناقصة. الفوز بامتلاء كل occupied cell مطابقة للتوكن المرجعي؛ لا تحسب التقاطع مرتين.

invariants: كل entry length يساوي token count؛ كل run أفقي/عمودي بطول ≥2 يقابله entry بالضبط؛ كل occupied cell مستخدم؛ الرسم graph للكلمات متصل؛ تقاطع حرف واحد/زوج لا يختلف؛ كل clue مرتبط بمصدر؛ لا جوابين متماثلين بعد normalization. المثال المصغر `crossword.json` يضع قلم أفقيًا وعلم عموديًا؛ حالة هندسية مسودة وليست حزمة منشورة.

## خطة اختبارات المحرك

Unit: كل reducer/action، no-op مكرر، نتيجة نهائية immutable، hint idempotent، seed deterministic، codec roundtrip، exact/basic اختلافات أمل/امل، على/علي، رحمة/رحمه، normalization idempotent. Property tests: replay(transcript)=savedState، نتيجة داخل 0..base، تحديث puzzle revision لا يغير جلسة مثبتة. مولد الكلمات المتقاطعة: 1000 seed × قاموس معتمد لاحقًا، مع shrinking لحالة فشل؛ فشل التوليد مقبول، نشر شبكة غير صالحة غير مقبول.
