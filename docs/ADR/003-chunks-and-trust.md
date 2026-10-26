# ADR003 — تحديث بالقطع وتوقيع manifest

الحالة: معتمد في التصميم. القرار: ملفات بيانات بلا ZIP، chunks≤8MiB addressed by SHA256، JCS+Ed25519 للـmanifest وassets/references registries. البديل: bsdiff أرخص أحيانًا لكنه يضيف سطح تعقيد ومخاطر على مصادر محمية؛ deferred لا حاجة له لتحقيق partial updates. SHA256 وحده لا يثبت الناشر. النتيجة: إعادة تنزيل changed chunks فقط، roots/keys حقيقية لا تختلق أثناء التصميم، API26 verifier gate إلزامي.
