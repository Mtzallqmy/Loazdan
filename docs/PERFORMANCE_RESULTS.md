# سجل الأداء وحدود القياس

## الأطلس 2.0.0

التغييرات: الجولة النشطة تراقب سجلها مباشرة، والتاريخ يُحمّل 50 سجلًا في المرة، وتعريفات JSON لها ذاكرة مؤقتة محدودة. تهيئة التطبيق تقرأ الجولات النشطة فقط. لا يُنشأ GameRegistry في كل بطاقة. فك الصور والتحقق من بصماتها يجريان في Dispatchers.IO مع ذاكرة صور 8MiB وinSampleSize يبدأ بـ1. الأصوات ثلاث ملفات PCM صغيرة محلية تفكها SoundPool بصورة غير متزامنة. حفظ الحركة والنتيجة وتقدم الرحلة معاملة واحدة، والإدخال لا يتوقف أثناء حفظ كل حرف.

اختبار OfflineRepositoryTest.thousandRoundHistoryIsPagedAndOldRoundCanBeOpenedDirectly ينشئ ألف جولة ويتحقق من الصفحة 50 ثم100، والاستعادة المباشرة لجولة قديمة. يثبت سلوك التخزين والتصفح، ولا يقيس زمن اللمس أو الذاكرة على هاتف.

تجمع CI على محاكيات x86_64 API26/28/35 ملفات gfxinfo وmeminfo وبصمة بناء المحاكي بعد مسارات فعلية. Android35 يشغّل أيضًا واجهة بعرض320 و480dp وتكبير خط130%. بناء المسارات Debug مع الحزمة الموقعة الحقيقية، بينما فحص APK وAndroid unit Release منفصلان. يمكن تكرار المسار عبر `scripts/device_acceptance.sh`؛ لا تمثل مقاييسه أداء هاتف ARM64 فعليًا.

لم يسجّل خط أساس Release على جهاز متوسط قبل التعديل. لذلك لا توجد مقارنة قبل/بعد موثقة، ولا ادعاء بتحقيق بدء≤2.5s أو P95 لمس<100ms أو انتقال<300ms أو Jank<5%. التجربة على هاتف فعلي والقياس المتكرر لا تزال لازمة لاعتماد هذه الأهداف. أرقام المضيف التاريخية أدناه ليست نتائج الأطلس على الهاتف.

## أدلة تاريخية — لا تخص إصدار الأطلس


No physical midrange-device performance result is claimed. CI engine tests print generator_pair_ms p50/p95 over41 deterministic small dictionary seeds, with Java/OS metadata. This measures host generator work only, not Android UI startup or a worst-case dictionary. Generator nodes are capped50000 and grid15×15; generation is an authoring/off-main facility, not invoked on the Compose main thread. No two-second completion SLA is asserted.

The device script captures actual adb dumpsys gfxinfo and meminfo after interactive acceptance, plus screenshots. Results must be read from the completed device artifact before quoting frame/PSS values. No guessed memory/FPS/battery figure is provided here. Final measured host figures are added after CI execution.

Large-library paging and exhaustive low-memory stress/thermal/battery benchmarks remain part of Stage04/optimization work; the current debug library has22 records. Session restoration fetches pinned definitions instead of joining every revision, but the current catalog still loads the small installed list into memory. Do not claim multi-gigabyte library scalability from this test.

## Measured local JVM sample — 2026-10-28

Fresh engine compilation and an explicit classpath with engine.jar first passed47 tests. On this Linux/JVM17.0.20 host, the41-seed pair benchmark printed p50=4.660592ms and p95=30.518883ms with82 valid successes. These exact figures identify that one small-dictionary run; they are not a phone latency promise or a large-dictionary worst case. Final CI/emulator measurements remain pending.

CI run40704686380 atbe0d5a68 printed generator_pair_ms p50=5.598847 and p95=34.562145, runs41/successes82, Java17.0.21 Linux. Android build/Lint and71 tests passed. Device acceptance in that run stopped before the final capture phase, so no device PSS/frame result is inferred from it.
