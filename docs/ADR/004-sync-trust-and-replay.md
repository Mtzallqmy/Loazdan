# ADR004 — فصل practice عن المنافسة

الحالة: معتمد في التصميم. القرار: practice self-reported مع durable outbox وunique session/operation/reward، CAS للخيارات، OR/max للتقدم، user clock lock لترتيب commit. المنافسة أسئلة غير موجودة بالحزم وتقييم خادمي مع pinned definition وsubmission digest دائم. البديل المرفوض: trust client score/clock أو token توقيع داخل APK. النتيجة: لا ترتيب رسمي offline، ولا عملات إنفاق في v1، حقل trust لا يتحول رسميًا بمجرد تسجيل الدخول.
