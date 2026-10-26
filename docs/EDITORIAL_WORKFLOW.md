# سير العمل التحريري

Draft → Validation → Linguistic Review → Scholarly Review → Approved → Published.

| الانتقال | المنفذ | المدخل/شرط النجاح | الفشل |
|---|---|---|---|
| Draft → Validation | editor | snapshot كامل + citations + rights evidence | يبقى draft مع أخطاء paths |
| Validation → Linguistic Review | validator service | JSON/semantics/uniqueness/normalization/media integrity | يعود draft |
| Linguistic → Scholarly | linguist مستقل | سلامة العربية وضبط الجواب والبدائل وعدم الالتباس | رفض موثق إلى draft |
| Scholarly → Approved | scholar مستقل للمحتوى المعرفي | مرجع محدد والتحقق من نسبة الشعر/دلالة القرآن/حقوق الأصل | reject مع مرجع التصحيح |
| Approved → Published | publisher | reviewDigest متطابق، مصدر approved، شخصان مستقلان، build validation | لا نشر جزئي |

للألغاز الحسابية الأصلية يمر Scholarly Review بمراجع منطقي مسمى، فلا تخطي صامت للمرحلة. لا صاحب المسودة يراجعها؛ يمكن شخص واحد لغوي+علمي فقط للمحتوى العام إذا كان مختلفًا عن الكاتب؛ القرآن يتطلب linguist وscholar بشريين مختلفين، وpublisher ثالث أو أحدهما وفق الصلاحيات لكن ليس الكاتب. owner لا يتجاوز gates. نتائج AI مسودات/اقتراحات لا توقيع مراجعة بشرية.

reviewDigest = SHA256(JCS(definition مع حذف status فقط)). أي تغيير معرفي يلغي كل approvals؛ author/puzzleId/revision جزء منه. publish يحدث status=published ثم full definition_sha256 مختلف متوقع، ويحفظ mapping. package item يستخدم full digest. status المستند وعمود DB يتغيران في نفس المعاملة. فحص `reviewer_id != author_id` وdigest/stage permissions في transaction قبل نشر publications. SQL FK لا يكفي لإثبات الاعتماد.

حماية التسابق: SELECT puzzle revision FOR UPDATE، optimistic baseRevision، fingerprint للتحقق، كتابة audit في المعاملة نفسها. publish لا يضع bytes قبل verify؛ publisher job يبني ويرفع staging objects، يتحقق ثم ينشر catalog pointer. فشل الخدمة يبقي job قابل retry. الرجوع عن النشر ينشئ withdrawn_at وسجل تدقيق ويوقف downloads المستقبلية؛ لا يمحو تاريخ التقدم أو الدليل. تحديث نسخة منشورة ينشئ revision جديدة من الأصل لا UPDATE.

لوحة الإدارة: قائمة مع مرشحات عالم/نوع/حالة/حقوق؛ محرر payload متخصص لكل نوع؛ معاينة بنفس renderer المنطقي دون ادعاء تجربة Android؛ صفحات للمصادر والتراخيص، الاصطلاحات، المراجعات diff حسب digest، بناء الحزم، التدقيق. لا زر «اعتماد الكل بالذكاء الاصطناعي». المصادر القرآنية تعرض النص canonical read-only والشرح في حقل مستقل؛ منع paste كنص آية غير محقق.

قاعدة الإلغاء: محتوى مشكوك فيه يتحول withdrawn، يظهر سبب سحب غير تشهيري؛ أجهزة offline قد تحتفظ به حتى اتصال، والواجهة لا تدعي مراجعة أحدث من تاريخ الحزمة. مسار البلاغ المعرفي داخل الإعدادات يفتح إرسال نص اختياري، لا يرسل بيانات شخصية تلقائيًا.
