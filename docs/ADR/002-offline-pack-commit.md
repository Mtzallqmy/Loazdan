# ADR002 — Room pointer مع immutable files

الحالة: معتمد في التصميم. السياق: SQLite والملفات لا يملكان commit مشتركًا. القرار: staging→validate→fsync→rename→Room transaction لتبديل pointer، journal recovery وsession pins. البديل المرفوض: حذف الحزمة القديمة ثم فك الجديدة أو UPDATE in place. النتيجة: مساحة مؤقتة أكبر محسوبة بوضوح؛ نسخة سابقة تبقى حتى تحقق الفتح وGC آمن، لا فقد تقدم عند delete pack.
