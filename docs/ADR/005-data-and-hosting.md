# ADR005 — PostgreSQL موحد وخدمات مستقلة

الحالة: معتمد في التصميم؛ مزود استضافة API/object storage ينتظر المالك. القرار: Neon PostgreSQL بschemas content/play/identity/ops/editorial وSQL migrations؛ Fastify TS + pg؛ Next.js يستخدم API؛ S3-compatible files. pooled runtime/direct migrations. لا قواعد منفصلة لكل لعبة، لا binary media في DB، لا admin credential في APK أو Next browser. مزود الاستضافة لا يغير البروتوكولات أو schema؛ يجب إثبات Range/ETag/quotas قبل deployment.
