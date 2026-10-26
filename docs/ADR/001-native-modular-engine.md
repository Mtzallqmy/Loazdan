# ADR001 — Android ومحرك نقي

الحالة: معتمد في التصميم،2026-10-26. السياق: ألعاب متعددة تعمل offline على API26. القرار: Kotlin/JVM engine مع typed plugins؛ Compose منفصل وClean/MVVM UDF. البدائل المرفوضة: Unity/Flutter خارج المكدس المطلوب، وengine داخل ViewModel يجعل اختبار القواعد وإعادة الاستخدام صعبًا. النتيجة: core tests بلا Android؛ UI لا ينفذ تقييمًا سريًا مختلفًا؛ زيادة نوع تتطلب schema+plugin+renderer+fixtures ومراجعة نشر دون تعديل قواعد الأنواع القديمة.
