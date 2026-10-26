# صيغة الحزم 1

العقد الآلي: `contracts/manifest.schema.json` و`puzzle.schema.json`. UUID للهوية، integer version متزايد 1..2^31−1، `schemaVersion` مستقل عن `minAppVersionCode` و`minEngineVersion`. عدد البايتات JSON integer حتى 2^53−1، Kotlin Long/Postgres bigint. أسماء الحزم وعناوينها في catalog وليست مسارات. locale=ar في v1.

## النقل والتوقيع

الحزمة Manifest + ملفات بيانات JSONL + media + attribution. لا ZIP على الهاتف في v1؛ بيانات غير مضغوطة مقطعة بحد 8 MiB، وصور WebP/PNG/JPEG وصوت Ogg مضغوطة أصلًا. هذا يقلل تعقيد zip bombs وdelta ثنائي. shard البيانات ≤8 MiB، السطر ≤256 KiB، manifest ≤16 MiB. حدود نوع الملف تطبق عبر magic bytes لا الامتداد وحده. كل chunk object immutable باسم SHA-256 على التخزين؛ manifest يحوي sha256/bytes لكل chunk ولكل ملف. يعاد تجميع chunks بالتسلسل، والتحقق من مجموع bytes ومن digest النهائي. الملفات لها مسارات نسبية تحت data/media/rights، ولا symlink أو .. أو backslash أو drive path أو تكرار مسار.

`downloadBytes` مجموع أحجام chunks الفريدة في الحزمة (لا تحسب reused مرتين). `installedBytes` مجموع file.bytes؛ لا يحتوي Room index overhead. المواصفة لا تشترط chunked files أن تكون media فقط؛ كل ملف يقسم. التحديث الجزئي تنزيل chunks الجديدة مقارنة بمخزن hashes المحلي؛ لا bsdiff ولا تنفيذ تعليمات patch. كل إصدار manifest كامل مستقل قابل للاسترجاع دون امتلاك نسخته السابقة.

التوقيع: RFC 8785 JCS على كائن manifest بعد حذف `signature` فقط؛ keyId جزء من البيانات الموقعة. UTF-8، رفض duplicate JSON keys وNaN وأعداد خارج safe integer. Ed25519 raw signature 64 bytes ممثل base64url بدون padding. public key bundled في trust store؛ private key في signing service/KMS وليس API أو مستودع. التوافق API26 يحتاج مزود Ed25519 مدقق داخل التطبيق؛ لا افتراض أن java.security في كل جهاز يدعمه. يثبت الاعتماد ويختبر على API26 في المرحلة الثانية. لا مفاتيح أو signature وهمية في هذه الحزمة؛ ملف manifest نموذجي غير موقّع يولّد فقط في أدوات الاختبار ولا يقبل نشرًا.

التدوير: تطبيق يحمل current+next public keys، keyId allowlist، trust metadata موقعة بالمفتاح الجذري الذي يحدّث عبر إصدار تطبيق؛ signing keys قابلة للتدوير ضمنها. الاحتفاظ بأعلى version رآها الجهاز يمنع rollback شبكي. rollback المحلي فقط إلى إصدار سبق التحقق منه عند فشل تثبيت/فتح الإصدار الجديد، مع حفظ high-water mark، ولا يفتح باب تثبيت نسخة قديمة من الشبكة. اكتشاف سحب مصدر عند الاتصال يمنع تنزيله مستقبلًا ويبلغ المستخدم؛ لا ادعاء إمكانية سحب نسخة على جهاز منفصل عن الشبكة.

## دلالات الملف

JSONL كل سطر PuzzleDefinition كامل مع إجابات وتلميحات وتفسير ومراجع؛ الإجابات مكشوفة عمدًا لتمكين offline، لذلك هذه الحزم لا تستخدم لمسابقات رسمية. محتوى المصادر المطلوب لشرح الاستشهاد وحقوقه داخل rights/*.txt وبيانات الفهرس؛ روابط الشبكة إضافية وليست شرطًا لقراءة التفسير. ملف dataset يسمح status=published وfixtureOnly=false فقط عند بناء حزمة production؛ fixtures تظل خارج `content/approved`.

التبعيات exact `(packId, version)`، max20، graph بلا دورات/تعارضات. التثبيت topology-first وتثبيت refs لكل إصدار؛ لا حذف dependency مستخدمة. مصدر أي puzzleRefs/assets يجب أن يكون في الحزمة أو dependency مثبتة. UUID stable لا يعاد استخدامه لمعنى آخر. تحديث إصدار لغز لا يلغي learned/completed على puzzleId؛ تغير جوهري للموضوع ينتج puzzleId جديدًا.

مراجع [JCS](https://www.rfc-editor.org/rfc/rfc8785)، [Ed25519](https://www.rfc-editor.org/rfc/rfc8032)، تحقق 2026-10-26. القرارات الحجمية هنا حدود دفاعية للحزمة وليست سقف مكتبة الجهاز.

## حل المعرفات محليًا — عقد نهائي

manifest.assets يربط assetId→path داخل files(kind=media) وsourceReferenceId وaltText؛ manifest.references يربط referenceId→source metadata/locator/license/attribution/noticePath داخل files(kind=rights). كلا registry موقّع ضمن manifest. ملف الصورة لا يحمل URL في Puzzle. يحل العميل UUID في الحزمة الحالية ثم dependency closure المثبتة بإصدارات exact؛ التعريف المكرر مسموح فقط إذا تطابق hash والحقول، وإلا يرفض conflict. لا يثبت pack قبل وجود كل UUID المرجعية والإشعارات محليًا. لا يحتاج Source API لعرض الاعتماد offline. assets paths لا تتكرر بمعرفات متعارضة، sourceRefs الأصلية UUID في production.

المثال `examples/pack/manifest-unsigned.json` له hashes وأحجام حقيقية وmedia وrights فعلية، لكنه candidate unsigned يحكمه manifest-payload.schema.json ويُرفض بعقد التثبيت الإنتاجي. timestamp فيه قيمة fixture معلنة وليس تاريخ نشر خدمة. لم ينشأ مفتاح أو توقيع وهمي.
