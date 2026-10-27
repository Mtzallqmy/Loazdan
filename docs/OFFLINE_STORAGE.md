# التخزين المحلي المنفذ

Room `lughzdan.db` v1 في تخزين التطبيق الخاص. installed_packs وpuzzles منفصلان منطقيًا عن sessions/progress/achievements/sync_operations. لا FK متسلسل من الإنجازات إلى الحزمة. `removeContent` يحذف الألغاز فقط؛ لا واجهة حذف للأساسي. كل submit يقرأ حالة الجلسة داخل `withTransaction` ثم يكتب الحالة والنتيجة وreceipt في المعاملة نفسها. UNIQUE(sessionId) يمنع تكرار اعتماد الجولة. bestScore=max ولا يتراكم عند retry. first_win فريد.

الجلسة تثبت puzzleId/revision وpackId/version. عند بدء repository تعاد ACTIVE إلى PAUSED مرة واحدة ويطلب الاستئناف. stateVersion غير المدعوم يحتفظ بالبايتات ويعرض تعذر الاستعادة؛ لا destructive fallback. تغير نسخة حزمة مثبتة دون migration يفشل بوضوح.

حزمة debug: manifest مبسط موثوق من APK، path strict وSHA256/byteCount قبل استيراد الألغاز. JSONL≤8MiB. `fixtureOnly` لا يسمح به خارج debug، وملفات المسودات لا تُجمع في release. صيغة الحزم الخارجية الموقعة في contracts تبقى تصميم المرحلة الرابعة؛ لا downloader خارجي ولا progress تحميل وهمي.

DataStore للمظهر والصوت فقط. لا انتظار API عند initialization أو start أو evaluate. worker لا يُجدول بلا endpoint HTTPS حقيقي ولا يثبت حزمة. outbox في المرحلة2 receipt محلي؛ payload جلسة داخلية **ليس** request مطابقًا لـsync.schema. تحويله إلى بروتوكول المزامنة ومصادقة الحساب عمل لاحق، ولا يُرسل الآن.

Room schema يبدأ v1؛ exported schema ضمن core/database/schemas. عند أي تغيير لاحق يجب Migration صريح واختبار previous→latest؛ ممنوع fallbackToDestructiveMigration. لا يوجد v0 منشور يحتاج migration وهمية. حذف التطبيق يفقد بيانات الضيف؛ allowBackup=false لتجنب نقل state غير متناسق.
