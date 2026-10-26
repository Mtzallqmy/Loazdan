# سجل المخاطر

| ID | المخاطرة | الاحتمال/الأثر | الإجراء والمالك | بوابة الإغلاق |
|---|---|---|---|---|
| R01 | حقوق Surah/طبعات التراث غير محسومة | عال/عال | مسؤول المحتوى يجمع إذنًا خاصًا وlocator | لا production pack قبل الحقوق |
| R02 | صحة معنى قرآني/نسبة شعر | متوسط/عال | مراجع لغوي وعلمي مستقلان | approval digest ومرجع معتمد |
| R03 | تلف/مساحة منخفضة أثناء pack update | عال/عال | مهندس offline،journal/pins/reservation | fault tests لكل commit boundary |
| R04 | Ed25519 على API26 | متوسط/عال | Android/security يثبت verifier مدقق | test vectors + جهازAPI26؛ لا homemade crypto |
| R05 | الغش بتفكيك APK/حزم الحلول | عال/متوسط | backend يفصل official bank ويقيم | replay/score forgery tests |
| R06 | حصص تنزيل بالخلفية/مصنّعو الهواتف | عال/متوسط | Android UIDT/foreground checkpoint | real-device network matrix |
| R07 | نمو receipt storage lifetime | متوسط/متوسط | data engineer quotas/partition عند القياس | load test وتكلفة حقيقية؛ لا حذف dedup عشوائي |
| R08 | أرقام أداء غير محققة | متوسط/عال | QA يقيس المرجعي والمنخفض | reports raw لا ادعاء نظري |
| R09 | ضبط الخط/RTL/chars يغير الإجابة | متوسط/عال | لغوي/Android golden vectors | NFC/grapheme/cross-platform parity |
| R10 | حسابات أطفال/alias عام | متوسط/عال | صاحب المشروع/privacy | تبقى disabled حتى السياسة والمراجعة |
| R11 | كلفة GB egress/object storage | متوسط/متوسط | المالك/DevOps يحدد مزودًا وميزانية | تجربة Range/renew/bandwidth قبل نشر |
| R12 | حذف حساب مع محتوى منشور | متوسط/عال | backend؛ author anonymization exception | SQL runtime delete test S2-09 |

التصحيحات SOL-01..08 تحسم العقد؛ اختبارات التنفيذ الفعلية لها بوابات مستقبلية ولا تختلط بإتمام تصميم المرحلة الأولى. لا يوجد blocker يتطلب اختراع خدمة أو أسرار. اختيار الاستضافة والحقوق/المراجعين قرارات خارج هذه الحزمة ولا نغيرها نيابة عن المالك.
