# المحرك المنفذ

`core/game-engine/.../Engine.kt`: ChoicePlugin فقط في GameRegistry. validatePuzzle يتحقق type/schema/rules/revision/difficulty ومعرفات الاختيارات وصحة correctId والمصدر/التفسير والتلميحات. JSON decoder strict يرفض حقولًا مجهولة. نوع choice عام2..6 اختيارات، ومحتوى المفردات الحالي4.

Select وSubmit وUseHint وResume أفعال immutable بمعرفات. processedActions يدعم no-op للمعرف المعاد. WON/LOST نهائيان. Pause عند recovery خارج reducer؛ Resume صريح. ScoringEngine: 50+50*difficulty−10*wrong−hint penalties، مقيد≥0، وصفر للخسارة. تكرار hintId لا يعيد الخصم.

ArabicText يحتفظ بالمصدر ويعيد قيمة مشتقة NFC. إزالة الحركات والتطويل وفصل zero-width مشتقات مقارنة فقط. دمج أشكال الألف أوى/ي opt-in في policy تلبية طلب المرحلة2، لا يُستخدم افتراضيًا ولا في الاختيارات (تُقارن IDs). ة/ه لا تُدمجان. bidi controls ترفض. لا نص قرآني مولد أو مخزن في هذه المرحلة.

SessionCodec encode/decode/stateVersion1 واستعادة paused. repository يتحقق الهوية والنسخ قبل dispatch. لا clock أو شبكة في قواعد التقييم؛ timestamp فقط ترتيب عرض التخزين. UUID action/session تولد في حدود التطبيق، لا تدخل مصدر الحكم.

إضافة لعبة: plugin مستقل، typed payload+validator مع schema version، golden vectors واختبارات reducer، ثم renderer وregistry entry. لا تسجل اللعبة قبل وجود renderer عامل. المتقاطعة لم تبدأ.
