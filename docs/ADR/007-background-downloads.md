# ADR007 — WorkManager مع مسار تنزيل مرئي بحسب Android

الحالة: معتمد في التصميم بعد فحص توثيق Android2026-10-26. القرار: WorkManager للمزامنة والمهام القصيرة والتعافي؛ UIDT علىAPI34+ لتنزيل كبير بدأه المستخدم، foreground WorkManager مقطع علىAPI26–33. السبب: الحصص والقيود الحديثة تمنع افتراض worker بلا حد. لا استبدال للمكدس ولا خدمة تشغيل دائمة. واجهة DownloadCoordinator واحدة تحافظ على checkpoint/networkPolicy. اختبار API26/34/36 قبل release.
