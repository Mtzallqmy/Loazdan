# ملاحظات الأمان للتنفيذ

Android: لا DATABASE_URL أو كلمات مرور أو API secret، cleartextTraffic=false، backup=false. Retrofit يرفض base URL غيرHTTPS، لا TrustAll ولا تعطيل hostname verification. التطبيق الحالي يعمل محليًا ولا يرسل تقدمًا. الروابط/حسابات المستخدمين/المنافسة غير مفعلة.

البيانات الأولية debug فقط وغير معتمدة؛ APK ليس مخزن أسرار الإجابات في ألعاب التدريب. API لا يملك بنك التحديات الخاص. صلاحيات runtime وRLS موثقة في BACKEND_SETUP/DATABASE_MIGRATIONS.

CI يجري مسح أنماط على tracked source، dependency review في PR، Android lint واختبارات العقود. هذه ليست شهادة خلو شامل من الثغرات. يلزم تفعيل required status checks في حماية الفرع من إعدادات GitHub قبل دمج إنتاجي؛ وجود YAML وحده لا يفرض حماية main.

No real secrets in .env.example؛ الإعداد بأسماء المتغيرات فقط. بيانات الاتصال الفعلية تأتي من البيئة الخادمة. لا استضافة API أو domain رسمي حتى يحدد المالك البيئة. لا صلاحيات runtime للهوية/المزامنة/التحرير مُفعلة صوريًا.

تحقق إضافي: npm audit --package-lock-only --audit-level=high على lockfile الحالي أعاد0 ثغرات (جميع الشدات) بين117 تبعية. أضيف npm audit إلى CI ليمنع مرور ثغرات high/critical. لا يعوض هذا فحص Android أو تفعيل Dependency graph المطلوب لفحص PR.
