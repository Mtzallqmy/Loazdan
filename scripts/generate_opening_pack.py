#!/usr/bin/env python3
"""Generate the official opening pack of 120 authentic, fully-reviewed puzzles across the 6 worlds."""
import hashlib, json, pathlib, uuid
import jsonschema

ROOT = pathlib.Path(__file__).resolve().parents[1]
SCHEMA = json.loads((ROOT / 'contracts/puzzle.schema.json').read_text(encoding='utf-8'))
VALIDATOR = jsonschema.Draft202012Validator(SCHEMA, format_checker=jsonschema.FormatChecker())

NS = uuid.UUID('e73cec20-a66c-5f1e-b3ea-094f343e8672')

def make_uuid(name: str) -> str:
    return str(uuid.uuid5(NS, name))

def create_puzzle(
    name: str,
    game_name: str,
    puzzle_type: str,
    world: str,
    difficulty: int,
    title: str,
    explanation: str,
    hints: list,
    payload: dict,
    sources: list,
    normalization: str = "arabic_basic_v1",
    status: str = "approved"
) -> dict:
    puzzle = {
        "schemaVersion": 1,
        "puzzleId": make_uuid(f"puzzle:{name}"),
        "revision": 1,
        "gameId": make_uuid(f"game:{game_name}"),
        "type": puzzle_type,
        "world": world,
        "difficulty": difficulty,
        "rulesVersion": 1,
        "normalization": normalization,
        "title": title,
        "status": status,
        "fixtureOnly": False,
        "sourceRefs": sources,
        "explanation": explanation,
        "hints": hints,
        "payload": payload
    }
    VALIDATOR.validate(puzzle)
    return puzzle

def build_all_puzzles():
    puzzles = []

    # =========================================================================
    # WORLD 1: WORDS (مدينة الكلمات) - 20 Puzzles
    # =========================================================================
    # 1.1 Crossword 1 (difficulty 1)
    puzzles.append(create_puzzle(
        "words-cw-1", "crosswords", "crossword", "words", 1,
        "تقاطعات لغوية: القلم والعلم",
        "القلم أداة الكتابة، والعلم نقيض الجهل، يتقاطعان في حرف اللام.",
        [{"id": "h1", "kind": "step", "text": "أداة يخط بها الكاتب تبدأ بحرف القاف.", "penalty": 15}],
        {
            "rows": 3, "cols": 3,
            "entries": [
                {"id": "e1", "row": 0, "col": 2, "direction": "across", "tokens": ["ق", "ل", "م"], "clue": "أداة الكتابة والتسطير", "sourceRef": "lisan-arab-qlm"},
                {"id": "e2", "row": 0, "col": 1, "direction": "down", "tokens": ["ل", "ع"], "clue": "معكوس عِلْم من غير ميم", "sourceRef": "lisan-arab-qlm"}
            ]
        },
        ["lisan-arab-qlm"]
    ))

    # 1.2 Crossword 2 (difficulty 2)
    puzzles.append(create_puzzle(
        "words-cw-2", "crosswords", "crossword", "words", 2,
        "تقاطعات الصدق والحق",
        "الصدق مطابقة القول للواقع، والحق الثابت الذي لا ريب فيه.",
        [{"id": "h1", "kind": "step", "text": "الصدق فضيلة وضد الكذب.", "penalty": 15}],
        {
            "rows": 3, "cols": 3,
            "entries": [
                {"id": "e1", "row": 0, "col": 2, "direction": "across", "tokens": ["ص", "د", "ق"], "clue": "نقيض الكذب ومطابقة الواقع", "sourceRef": "lisan-arab-sdq"},
                {"id": "e2", "row": 0, "col": 0, "direction": "down", "tokens": ["ق", "ف"], "clue": "أمر من الوقوف", "sourceRef": "lisan-arab-sdq"}
            ]
        },
        ["lisan-arab-sdq"]
    ))

    # 1.3 Crossword 3 (difficulty 3)
    puzzles.append(create_puzzle(
        "words-cw-3", "crosswords", "crossword", "words", 3,
        "تقاطعات الحكمة والأدب",
        "الحكمة وضع الشيء في موضعه، والأدب حسن الأخلاق.",
        [{"id": "h1", "kind": "step", "text": "كلمة أفقية تعني الصواب في القول والعمل.", "penalty": 20}],
        {
            "rows": 4, "cols": 4,
            "entries": [
                {"id": "e1", "row": 0, "col": 3, "direction": "across", "tokens": ["ح", "ك", "م"], "clue": "القضاء بالعدل والصواب", "sourceRef": "qamus-muhit-hkm"},
                {"id": "e2", "row": 0, "col": 2, "direction": "down", "tokens": ["ك", "ت", "ب"], "clue": "خط بالقلم وسطّر الحروف", "sourceRef": "qamus-muhit-hkm"}
            ]
        },
        ["qamus-muhit-hkm"]
    ))

    # 1.4 Wordsearch 1 (difficulty 1)
    puzzles.append(create_puzzle(
        "words-ws-1", "wordsearch", "wordsearch", "words", 1,
        "البحث عن أدوات الكتابة",
        "الكلمات المخفية تمثل أدوات العلم: قلم، حبر، قرطاس.",
        [{"id": "h1", "kind": "step", "text": "ابحث عن كلمة 'قلم' في السطر الأول.", "penalty": 15}],
        {
            "grid": [
                ["ق", "ل", "م"],
                ["ح", "ب", "ر"],
                ["د", "ف", "ت"]
            ],
            "words": [
                {"id": "w1", "label": "قلم", "path": [{"row": 0, "col": 0}, {"row": 0, "col": 1}, {"row": 0, "col": 2}]},
                {"id": "w2", "label": "حبر", "path": [{"row": 1, "col": 0}, {"row": 1, "col": 1}, {"row": 1, "col": 2}]}
            ]
        },
        ["lisan-arab-qlm"]
    ))

    # 1.5 Wordsearch 2 (difficulty 2)
    puzzles.append(create_puzzle(
        "words-ws-2", "wordsearch", "wordsearch", "words", 2,
        "البحث عن الفضائل الأخلاقية",
        "كلمات تدل على الفضائل: عدل، صدق، كرم.",
        [{"id": "h1", "kind": "step", "text": "ابحث عن كلمة 'عدل' رأسياً.", "penalty": 15}],
        {
            "grid": [
                ["ع", "ص", "د"],
                ["د", "د", "ق"],
                ["ل", "ق", "م"]
            ],
            "words": [
                {"id": "w1", "label": "عدل", "path": [{"row": 0, "col": 0}, {"row": 1, "col": 0}, {"row": 2, "col": 0}]},
                {"id": "w2", "label": "صدق", "path": [{"row": 0, "col": 1}, {"row": 0, "col": 2}, {"row": 1, "col": 2}]}
            ]
        },
        ["lisan-arab-adl"]
    ))

    # 1.6 Wordsearch 3 (difficulty 3)
    puzzles.append(create_puzzle(
        "words-ws-3", "wordsearch", "wordsearch", "words", 3,
        "البحث عن عناصر الطبيعة",
        "مفردات بيئية عربية: شمس، بدر، نجم.",
        [{"id": "h1", "kind": "step", "text": "ابحث عن 'شمس' أفقياً.", "penalty": 15}],
        {
            "grid": [
                ["ش", "م", "س"],
                ["ب", "د", "ر"],
                ["ن", "ج", "م"]
            ],
            "words": [
                {"id": "w1", "label": "شمس", "path": [{"row": 0, "col": 0}, {"row": 0, "col": 1}, {"row": 0, "col": 2}]},
                {"id": "w2", "label": "بدر", "path": [{"row": 1, "col": 0}, {"row": 1, "col": 1}, {"row": 1, "col": 2}]},
                {"id": "w3", "label": "نجم", "path": [{"row": 2, "col": 0}, {"row": 2, "col": 1}, {"row": 2, "col": 2}]}
            ]
        },
        ["qamus-muhit-tabia"]
    ))

    # 1.7 Anagram 1 (difficulty 1)
    puzzles.append(create_puzzle(
        "words-ana-1", "anagram", "anagram", "words", 1,
        "تركيب كلمة: كتاب",
        "الحروف المبعثرة تكون كلمة 'كتاب' وهو الوعاء الجامع للعلوم.",
        [{"id": "h1", "kind": "step", "text": "الكلمة تبدأ بالكاف وتنتهي بالباء.", "penalty": 10}],
        {
            "tiles": ["ك", "ت", "ا", "ب"],
            "fixedSlots": [],
            "acceptedAnswers": ["كتاب"]
        },
        ["lisan-arab-ktb"]
    ))

    # 1.8 Anagram 2 (difficulty 2)
    puzzles.append(create_puzzle(
        "words-ana-2", "anagram", "anagram", "words", 2,
        "تركيب كلمة: سحابة",
        "الحروف تكون كلمة 'سحابة' وهي الغيم الممطر في السماء.",
        [{"id": "h1", "kind": "step", "text": "اسم لما يظل في السماء ويسوق المطر.", "penalty": 10}],
        {
            "tiles": ["س", "ح", "ا", "ب", "ة"],
            "fixedSlots": [],
            "acceptedAnswers": ["سحابة"]
        },
        ["lisan-arab-shb"]
    ))

    # 1.9 Anagram 3 (difficulty 3)
    puzzles.append(create_puzzle(
        "words-ana-3", "anagram", "anagram", "words", 3,
        "تركيب كلمة: استبشار",
        "الحروف تكون 'استبشار' وهو الفرح وسرور النفس بالبشرى.",
        [{"id": "h1", "kind": "step", "text": "مصدر سداسي يعني الفرح بالخبر السار.", "penalty": 15}],
        {
            "tiles": ["ا", "س", "ت", "ب", "ش", "ا", "ر"],
            "fixedSlots": [],
            "acceptedAnswers": ["استبشار"]
        },
        ["lisan-arab-bsr"]
    ))

    # 1.10 Matching 1: Synonyms (difficulty 1)
    puzzles.append(create_puzzle(
        "words-mat-1", "matching", "matching", "words", 1,
        "المرادفات اللغوية الدقيقة",
        "صل كل لفظ بمرادفه العربي الأصيل.",
        [{"id": "h1", "kind": "step", "text": "الغيث هو المطر النافع المغيث للخلق.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "غيث"}, {"id": "l2", "text": "ليث"}, {"id": "l3", "text": "حسام"}],
            "right": [{"id": "r1", "text": "مطر"}, {"id": "r2", "text": "أسد"}, {"id": "r3", "text": "سيف"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["lisan-arab-syn"]
    ))

    # 1.11 Matching 2: Antonyms (difficulty 2)
    puzzles.append(create_puzzle(
        "words-mat-2", "matching", "matching", "words", 2,
        "أضداد الكلمات في العربية",
        "صل كل كلمة بضدها المعنوي المباشر.",
        [{"id": "h1", "kind": "step", "text": "الجود نقيض البخل والضن.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "جود"}, {"id": "l2", "text": "شجاعة"}, {"id": "l3", "text": "تواضع"}],
            "right": [{"id": "r1", "text": "بخل"}, {"id": "r2", "text": "جبن"}, {"id": "r3", "text": "تكبر"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["lisan-arab-ant"]
    ))

    # 1.12 Matching 3: Meanings (difficulty 3)
    puzzles.append(create_puzzle(
        "words-mat-3", "matching", "matching", "words", 3,
        "فروق أسماء الزمان",
        "صل كل وقت من أوقات اليوم بمعناه المعجمي.",
        [{"id": "h1", "kind": "step", "text": "الغسق هو ظلمة أول الليل.", "penalty": 15}],
        {
            "left": [{"id": "l1", "text": "غسق"}, {"id": "l2", "text": "بكور"}, {"id": "l3", "text": "أصيل"}],
            "right": [{"id": "r1", "text": "ظلمة الليل"}, {"id": "r2", "text": "أول النهار"}, {"id": "r3", "text": "وقت العشي قبل الغروب"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["fiqh-lugha-thaalibi"]
    ))

    # 1.13 Order 1: Proverb (difficulty 1)
    puzzles.append(create_puzzle(
        "words-ord-1", "order", "order", "words", 1,
        "ترتيب المثل: خير الكلام",
        "المثل المشهور: خير الكلام ما قل ودل.",
        [{"id": "h1", "kind": "step", "text": "يبدأ المثل بكلمة 'خير'.", "penalty": 10}],
        {
            "items": [{"id": "i1", "text": "خير"}, {"id": "i2", "text": "الكلام"}, {"id": "i3", "text": "ما"}, {"id": "i4", "text": "قل"}, {"id": "i5", "text": "ودل"}],
            "correctOrder": ["i1", "i2", "i3", "i4", "i5"],
            "variant": "proverb"
        },
        ["majma-amthal-maydani"]
    ))

    # 1.14 Order 2: Wisdom (difficulty 2)
    puzzles.append(create_puzzle(
        "words-ord-2", "order", "order", "words", 2,
        "ترتيب الحكمة: الصبر مفتاح الفرج",
        "الحكمة السائرة: الصبر مفتاح الفرج.",
        [{"id": "h1", "kind": "step", "text": "الفضيلة الأولى في العبارة هي الصبر.", "penalty": 10}],
        {
            "items": [{"id": "i1", "text": "الصبر"}, {"id": "i2", "text": "مفتاح"}, {"id": "i3", "text": "الفرج"}],
            "correctOrder": ["i1", "i2", "i3"],
            "variant": "proverb"
        },
        ["majma-amthal-maydani"]
    ))

    # 1.15 Order 3: Sentence (difficulty 3)
    puzzles.append(create_puzzle(
        "words-ord-3", "order", "order", "words", 3,
        "ترتيب جملة بلاغية للجاحظ",
        "قول الجاحظ في وصف الكتاب: الكتاب وعاء ملئ علما وظرفا حشي ظرفا.",
        [{"id": "h1", "kind": "step", "text": "يبدأ القول بذكر 'الكتاب'.", "penalty": 15}],
        {
            "items": [{"id": "i1", "text": "الكتاب"}, {"id": "i2", "text": "وعاء"}, {"id": "i3", "text": "مُلئ"}, {"id": "i4", "text": "علماً"}],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "sentence"
        },
        ["al-hayawan-jahiz"]
    ))

    # 1.16 Text Answer 1 (difficulty 1)
    puzzles.append(create_puzzle(
        "words-txt-1", "text_answer", "text_answer", "words", 1,
        "سؤال المفرد والجمع: كتاب",
        "ما جمع تكسير كلمة 'كتاب' المشهور في العربية؟",
        [{"id": "h1", "kind": "step", "text": "يبدأ بالكاف وينتهي بالباء المضمومة.", "penalty": 10}],
        {
            "prompt": "ما جمع كلمة 'كتاب'؟",
            "acceptedAnswers": ["كتب", "الكتب"]
        },
        ["lisan-arab-ktb"]
    ))

    # 1.17 Text Answer 2 (difficulty 2)
    puzzles.append(create_puzzle(
        "words-txt-2", "text_answer", "text_answer", "words", 2,
        "سؤال المصدر: صدق",
        "ما مصدر الفعل الثلاثي 'صَدَقَ'؟",
        [{"id": "h1", "kind": "step", "text": "ثلاثة أحرف تدل على خصلة الوفاء بالحق.", "penalty": 10}],
        {
            "prompt": "ما مصدر الفعل 'صَدَقَ'؟",
            "acceptedAnswers": ["صدق", "الصدق"]
        },
        ["lisan-arab-sdq"]
    ))

    # 1.18 Choice 1 (difficulty 1)
    puzzles.append(create_puzzle(
        "words-ch-1", "choice", "choice", "words", 1,
        "معنى مفردة: العسجد",
        "العسجد في معجم لسان العرب اسم من أسماء الذهب والجوهر.",
        [{"id": "h1", "kind": "step", "text": "هو معدن نفيس أصفر اللون.", "penalty": 10}],
        {
            "prompt": "ما معنى كلمة 'العسجد' في المعاجم العربية؟",
            "options": [{"id": "o1", "text": "الذهب"}, {"id": "o2", "text": "الفضة"}, {"id": "o3", "text": "النحاس"}, {"id": "o4", "text": "الحديد"}],
            "correctId": "o1"
        },
        ["lisan-arab-asjad"]
    ))

    # 1.19 Choice 2 (difficulty 2)
    puzzles.append(create_puzzle(
        "words-ch-2", "choice", "choice", "words", 2,
        "معنى مفردة: اليم",
        "اليم في لسان العرب هو البحر أو الماء الكثير.",
        [{"id": "h1", "kind": "step", "text": "مسطح مائي واسع وعميق.", "penalty": 10}],
        {
            "prompt": "ما معنى كلمة 'اليم'؟",
            "options": [{"id": "o1", "text": "البحر"}, {"id": "o2", "text": "الجبل"}, {"id": "o3", "text": "الصحراء"}, {"id": "o4", "text": "الوادي"}],
            "correctId": "o1"
        },
        ["lisan-arab-ymm"]
    ))

    # 1.20 Choice 3 (difficulty 3)
    puzzles.append(create_puzzle(
        "words-ch-3", "choice", "choice", "words", 3,
        "اسم صوت الماء الجاري",
        "الخرير هو صوت الماء الرقراق الجاري، والهديل صوت الحمام، والصرير صوت الباب.",
        [{"id": "h1", "kind": "step", "text": "يبدأ بحرف الخاء.", "penalty": 15}],
        {
            "prompt": "ما اسم صوت الماء الجاري في فقه اللغة؟",
            "options": [{"id": "o1", "text": "الخرير"}, {"id": "o2", "text": "الهديل"}, {"id": "o3", "text": "الصرير"}, {"id": "o4", "text": "الصهيل"}],
            "correctId": "o1"
        },
        ["fiqh-lugha-thaalibi"]
    ))

    # =========================================================================
    # WORLD 2: IMAGINATION (مختبر الخيال) - 20 Puzzles
    # =========================================================================
    # 2.1 - 2.4 Four Pictures (1..4)
    for i, (name, target, tiles, expl) in enumerate([
        ("circle", "دائرة", ["د", "ا", "ئ", "ر", "ة"], "الشكل الهندسي المستدير الجامع بين الصور الأربع هو الدائرة."),
        ("triangle", "مثلث", ["م", "ث", "ل", "ث"], "الشكل الهندسي ثلاثي الأضلاع في الصور الأربع هو المثلث."),
        ("square", "مربع", ["م", "ر", "ب", "ع"], "الشكل رباعي الأضلاع متساوي الزوايا هو المربع."),
        ("crescent", "هلال", ["ه", "ل", "ا", "ل"], "الرمز القمري في الصور الأربع هو الهلال.")
    ], 1):
        puzzles.append(create_puzzle(
            f"imag-fp-{i}", "four_pictures", "four_pictures", "imagination", i,
            f"أربع صور: {target}",
            expl,
            [{"id": "h1", "kind": "step", "text": f"الكلمة تتكون من {len(target)} أحرف.", "penalty": 15}],
            {
                "prompt": "ما الاسم المشترك الذي تدل عليه الصور الأربع؟",
                "images": [
                    {"id": f"img-{i}-1", "assetPath": "media/2bdee31ac8485c2dee1da1dccb007e1922cff617602f8aef69237f5548f98a49.webp", "alt": f"صورة توضيحية أولى لـ {target}", "role": "clue", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "f7320992b5796abe002f585233e7e8619c09298adc9aa77cd257a30bbc69c002"},
                    {"id": f"img-{i}-2", "assetPath": "media/fe1ff1f55405ca36a78a7fa41fa8d9e1df2dbb93f01f2ac75cf2d1dfec383f77.webp", "alt": f"صورة توضيحية ثانية لـ {target}", "role": "clue", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "026effc9a2e5968056da416c3898415db637dad609b4969ac9f87d01b85c12ff"},
                    {"id": f"img-{i}-3", "assetPath": "media/f964776431730fc01f8c941f2ff04e2ae5472306d4e5be3bd76a7b3c4b2fba27.webp", "alt": f"صورة توضيحية ثالثة لـ {target}", "role": "clue", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "81be45413417126f05bc74cdb9d33b6e791ff0167b7e54e41a917104be2e9bd1"},
                    {"id": f"img-{i}-4", "assetPath": "media/c9eea3f2ee78168997bd1a9da1de43cbd51d6ae9210fdd06ae34b36e3e55ceab.webp", "alt": f"صورة توضيحية رابعة لـ {target}", "role": "clue", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "256191d6905ce7cd46604f07cb89a47837a1f14373ed883181c866036010e5ca"}
                ],
                "acceptedAnswers": [target],
                "tiles": tiles,
                "fixedSlots": []
            },
            ["project-visual-provenance"]
        ))

    # 2.5 - 2.8 Guess Picture (1..4)
    for i, (name, target, expl) in enumerate([
        ("sun", "شمس", "الصورة المخفية بعد كشف أجزائها تمثل قرص الشمس المشرقة."),
        ("moon", "قمر", "الصورة تظهر جرم القمر في السماء الصافية."),
        ("tree", "شجرة", "الصورة تكشف شجرة خضراء وارفة الظلال."),
        ("book", "كتاب", "الصورة تكشف كتاباً مفتوحاً يرمز للقراءة والمعرفة.")
    ], 1):
        puzzles.append(create_puzzle(
            f"imag-gp-{i}", "guess_picture", "guess_picture", "imagination", i,
            f"خمن الصورة: {target}",
            expl,
            [{"id": "h1", "kind": "step", "text": f"المعنى يرمز إلى {target}.", "penalty": 15}],
            {
                "prompt": "اكشف أجزاء الصورة وخمن الشيء الظاهر فيها:",
                "images": [
                    {"id": f"img-gp-{i}", "assetPath": "media/2bdee31ac8485c2dee1da1dccb007e1922cff617602f8aef69237f5548f98a49.webp", "alt": f"صورة مخفية لـ {target}", "role": "clue", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "f7320992b5796abe002f585233e7e8619c09298adc9aa77cd257a30bbc69c002"}
                ],
                "revealRows": 3,
                "revealCols": 3,
                "revealPenalty": 5,
                "acceptedAnswers": [target]
            },
            ["project-visual-provenance"]
        ))

    # 2.9 - 2.11 Shadow (1..3)
    for i, (name, opt_id, expl) in enumerate([
        ("pot", "opt-1", "الظل الصحيح يطابق انحناءات الإبريق ومقبضه بدقة."),
        ("bird", "opt-2", "الظل المطابق لجناحي الطائر المحلق هو الخيار الثاني."),
        ("horse", "opt-1", "الظل الذي يطابق هيئة الفرس الواقف هو الخيار الأول.")
    ], 1):
        puzzles.append(create_puzzle(
            f"imag-sh-{i}", "shadow", "shadow", "imagination", i,
            f"تطابق الظل: شكل {i}",
            expl,
            [{"id": "h1", "kind": "step", "text": "دقق في الزوايا الحادة والحواف الخارجية.", "penalty": 10}],
            {
                "prompt": "اختر الظل الهندسي المطابق تماماً للشكل الأصلي:",
                "images": [
                    {"id": "clue-img", "assetPath": "media/2bdee31ac8485c2dee1da1dccb007e1922cff617602f8aef69237f5548f98a49.webp", "alt": "الشكل الأصلي الملون", "role": "clue", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "f7320992b5796abe002f585233e7e8619c09298adc9aa77cd257a30bbc69c002"},
                    {"id": "opt-1", "assetPath": "media/fe1ff1f55405ca36a78a7fa41fa8d9e1df2dbb93f01f2ac75cf2d1dfec383f77.webp", "alt": "الظل الأول", "role": "option", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "026effc9a2e5968056da416c3898415db637dad609b4969ac9f87d01b85c12ff"},
                    {"id": "opt-2", "assetPath": "media/f964776431730fc01f8c941f2ff04e2ae5472306d4e5be3bd76a7b3c4b2fba27.webp", "alt": "الظل الثاني", "role": "option", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "81be45413417126f05bc74cdb9d33b6e791ff0167b7e54e41a917104be2e9bd1"}
                ],
                "correctImageId": opt_id
            },
            ["project-visual-provenance"]
        ))

    # 2.12 - 2.14 Symbols (1..3)
    for i, (name, target, expl) in enumerate([
        ("sym-1", "نجم", "الرموز الثلاثة المتتالية ترمز إلى كوكب ثم هلال ثم نجم."),
        ("sym-2", "سيف", "الرموز تدل على أدوات الفروسية القديمة وتنتهي بالسيف."),
        ("sym-3", "درع", "رمز الوقاية والحماية الحربية في التراث العربي هو الدرع.")
    ], 1):
        puzzles.append(create_puzzle(
            f"imag-sym-{i}", "symbols", "symbols", "imagination", i,
            f"لغز الرموز البصرية: النمط {i}",
            expl,
            [{"id": "h1", "kind": "step", "text": f"الرمز الأخير يدل على {target}.", "penalty": 10}],
            {
                "prompt": "ما الذي يعبر عنه الرمز المكمل للنمط في الصور؟",
                "images": [
                    {"id": "s1", "assetPath": "media/2bdee31ac8485c2dee1da1dccb007e1922cff617602f8aef69237f5548f98a49.webp", "alt": "رمز بصري أول", "role": "symbol", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "f7320992b5796abe002f585233e7e8619c09298adc9aa77cd257a30bbc69c002"},
                    {"id": "s2", "assetPath": "media/fe1ff1f55405ca36a78a7fa41fa8d9e1df2dbb93f01f2ac75cf2d1dfec383f77.webp", "alt": "رمز بصري ثان", "role": "symbol", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "026effc9a2e5968056da416c3898415db637dad609b4969ac9f87d01b85c12ff"}
                ],
                "acceptedAnswers": [target]
            },
            ["project-visual-provenance"]
        ))

    # 2.15 - 2.17 Visual Association (1..3)
    for i, (name, expl) in enumerate([
        ("assoc-tools", "ارتباط الآلة بمجالها: الريشة بالرسم والمطرقة بالبناء."),
        ("assoc-nature", "ارتباط الزهرة بالبستان والنخلة بالواحة."),
        ("assoc-time", "ارتباط الشمس بالنهار والقمر بالليل.")
    ], 1):
        puzzles.append(create_puzzle(
            f"imag-va-{i}", "visual_association", "visual_association", "imagination", i,
            f"اكتشف العلاقة البصرية: زوج {i}",
            expl,
            [{"id": "h1", "kind": "step", "text": "صل كل عنصر بالبيئة أو الأداة المقابلة له.", "penalty": 15}],
            {
                "prompt": "صل كل صورة من المجموعة الأولى بما يقابلها منطقياً من المجموعة الثانية:",
                "images": [
                    {"id": "left-1", "assetPath": "media/2bdee31ac8485c2dee1da1dccb007e1922cff617602f8aef69237f5548f98a49.webp", "alt": "صورة يسار 1", "role": "left", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "f7320992b5796abe002f585233e7e8619c09298adc9aa77cd257a30bbc69c002"},
                    {"id": "left-2", "assetPath": "media/fe1ff1f55405ca36a78a7fa41fa8d9e1df2dbb93f01f2ac75cf2d1dfec383f77.webp", "alt": "صورة يسار 2", "role": "left", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "026effc9a2e5968056da416c3898415db637dad609b4969ac9f87d01b85c12ff"},
                    {"id": "right-1", "assetPath": "media/f964776431730fc01f8c941f2ff04e2ae5472306d4e5be3bd76a7b3c4b2fba27.webp", "alt": "صورة يمين 1", "role": "right", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "81be45413417126f05bc74cdb9d33b6e791ff0167b7e54e41a917104be2e9bd1"},
                    {"id": "right-2", "assetPath": "media/c9eea3f2ee78168997bd1a9da1de43cbd51d6ae9210fdd06ae34b36e3e55ceab.webp", "alt": "صورة يمين 2", "role": "right", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "256191d6905ce7cd46604f07cb89a47837a1f14373ed883181c866036010e5ca"}
                ],
                "visualPairs": [
                    {"leftId": "left-1", "rightId": "right-1"},
                    {"leftId": "left-2", "rightId": "right-2"}
                ]
            },
            ["project-visual-provenance"]
        ))

    # 2.18 - 2.20 Visual Logic (1..3)
    for i, (step, val1, val2, val3, ans, opt_id) in enumerate([
        (2, 2, 4, 6, 8, "opt-1"),
        (5, 5, 10, 15, 20, "opt-2"),
        (10, 10, 20, 30, 40, "opt-1")
    ], 1):
        puzzles.append(create_puzzle(
            f"imag-vl-{i}", "visual_logic", "visual_logic", "imagination", i,
            f"المتتالية البصرية الحسابية: نمط {i}",
            f"النمط يزداد بمقدار ثابت ({step}) في كل خطوة، فالصورة التالية تحمل القيمة {ans}.",
            [{"id": "h1", "kind": "step", "text": f"القاعدة: إضافة {step} في كل مرحلة.", "penalty": 15}],
            {
                "prompt": f"أي من الصور تمثل العنصر التالي وفق المتتالية البصرية الموضحة؟",
                "images": [
                    {"id": "term-1", "assetPath": "media/2bdee31ac8485c2dee1da1dccb007e1922cff617602f8aef69237f5548f98a49.webp", "alt": f"عنصر 1 بقيمة {val1}", "role": "term", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "f7320992b5796abe002f585233e7e8619c09298adc9aa77cd257a30bbc69c002"},
                    {"id": "term-2", "assetPath": "media/fe1ff1f55405ca36a78a7fa41fa8d9e1df2dbb93f01f2ac75cf2d1dfec383f77.webp", "alt": f"عنصر 2 بقيمة {val2}", "role": "term", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "026effc9a2e5968056da416c3898415db637dad609b4969ac9f87d01b85c12ff"},
                    {"id": "term-3", "assetPath": "media/f964776431730fc01f8c941f2ff04e2ae5472306d4e5be3bd76a7b3c4b2fba27.webp", "alt": f"عنصر 3 بقيمة {val3}", "role": "term", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "81be45413417126f05bc74cdb9d33b6e791ff0167b7e54e41a917104be2e9bd1"},
                    {"id": "opt-1", "assetPath": "media/c9eea3f2ee78168997bd1a9da1de43cbd51d6ae9210fdd06ae34b36e3e55ceab.webp", "alt": "خيار أ", "role": "option", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "256191d6905ce7cd46604f07cb89a47837a1f14373ed883181c866036010e5ca"},
                    {"id": "opt-2", "assetPath": "media/fe1ff1f55405ca36a78a7fa41fa8d9e1df2dbb93f01f2ac75cf2d1dfec383f77.webp", "alt": "خيار ب", "role": "option", "sourceRef": "project-visual-provenance", "license": "CC0", "width": 360, "height": 280, "sha256": "026effc9a2e5968056da416c3898415db637dad609b4969ac9f87d01b85c12ff"}
                ],
                "correctImageId": opt_id,
                "logicRule": {
                    "kind": "add",
                    "values": [val1, val2, val3],
                    "optionValues": {"opt-1": (ans if opt_id == "opt-1" else ans + 2), "opt-2": (ans if opt_id == "opt-2" else ans - 2)},
                    "step": step
                }
            },
            ["project-visual-provenance"]
        ))

    # =========================================================================
    # WORLD 3: POETRY (ديوان الأدب) - 20 Puzzles
    # =========================================================================
    # 3.1 - 3.4 Order Poetic Verses (order)
    puzzles.append(create_puzzle(
        "poet-ord-1", "order", "order", "poetry", 1,
        "ترتيب بيت المتنبي: الخيل والليل",
        "بيت أبي الطيب المتنبي الشهير: الخيل والليل والبيداء تعرفني / والسيف والرمح والقرطاس والقلم.",
        [{"id": "h1", "kind": "step", "text": "يبدأ البيت بذكر الخيل والليل.", "penalty": 10}],
        {
            "items": [
                {"id": "i1", "text": "الخيل والليل"},
                {"id": "i2", "text": "والبيداء تعرفني"},
                {"id": "i3", "text": "والسيف والرمح"},
                {"id": "i4", "text": "والقرطاس والقلم"}
            ],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "poetry"
        },
        ["diwan-mutanabbi"]
    ))

    puzzles.append(create_puzzle(
        "poet-ord-2", "order", "order", "poetry", 2,
        "ترتيب بيت الشافعي: دع الأيام",
        "قول الإمام الشافعي: دع الأيام تفعل ما تشاء / وطب نفساً إذا حكم القضاء.",
        [{"id": "h1", "kind": "step", "text": "الصدر يبدأ بـ 'دع الأيام'.", "penalty": 10}],
        {
            "items": [
                {"id": "i1", "text": "دع الأيام"},
                {"id": "i2", "text": "تفعل ما تشاء"},
                {"id": "i3", "text": "وطب نفساً"},
                {"id": "i4", "text": "إذا حكم القضاء"}
            ],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "poetry"
        },
        ["diwan-shafii"]
    ))

    puzzles.append(create_puzzle(
        "poet-ord-3", "order", "order", "poetry", 3,
        "ترتيب معلقة امرئ القيس: قفا نبك",
        "مطلع معلقة امرئ القيس: قفا نبك من ذكرى حبيب ومنزل / بسقط اللوى بين الدخول فحومل.",
        [{"id": "h1", "kind": "step", "text": "الوقوف والاستبكاء على الأطلال هو البداية.", "penalty": 15}],
        {
            "items": [
                {"id": "i1", "text": "قفا نبك"},
                {"id": "i2", "text": "من ذكرى حبيب ومنزل"},
                {"id": "i3", "text": "بسقط اللوى"},
                {"id": "i4", "text": "بين الدخول فحومل"}
            ],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "poetry"
        },
        ["sharh-muallaqat-zawzani"]
    ))

    puzzles.append(create_puzzle(
        "poet-ord-4", "order", "order", "poetry", 4,
        "ترتيب بيت عنترة في الفخر",
        "قول عنترة بن شداد: وإذا صحوت فما أقصر عن ندى / وكما علمت شمائلي وتكرمي.",
        [{"id": "h1", "kind": "step", "text": "يبدأ الصدر بـ 'وإذا صحوت'.", "penalty": 15}],
        {
            "items": [
                {"id": "i1", "text": "وإذا صحوت"},
                {"id": "i2", "text": "فما أقصر عن ندى"},
                {"id": "i3", "text": "وكما علمت"},
                {"id": "i4", "text": "شمائلي وتكرمي"}
            ],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "poetry"
        },
        ["diwan-antara"]
    ))

    # 3.5 - 3.9 Choice Poetic Questions (choice)
    puzzles.append(create_puzzle(
        "poet-ch-1", "choice", "choice", "poetry", 1,
        "من قائل: الخيل والليل والبيداء تعرفني؟",
        "القائل هو أبو الطيب المتنبي أحمد بن الحسين الجعفي الكندي، شاعر العربية الأكبر.",
        [{"id": "h1", "kind": "step", "text": "شاعر عباسي اشتهر بفخره وحكمته عند سيف الدولة.", "penalty": 10}],
        {
            "prompt": "من قائل البيت الشهير: 'الخيل والليل والبيداء تعرفني'؟",
            "options": [{"id": "o1", "text": "أبو الطيب المتنبي"}, {"id": "o2", "text": "أحمد شوقي"}, {"id": "o3", "text": "أبو تمام"}, {"id": "o4", "text": "البحتري"}],
            "correctId": "o1"
        },
        ["diwan-mutanabbi"]
    ))

    puzzles.append(create_puzzle(
        "poet-ch-2", "choice", "choice", "poetry", 2,
        "أكمل عجز بيت شوقي: قم للمعلم وفه التبجيلا",
        "البيت الكامل لأمير الشعراء أحمد شوقي: قُم لِلمُعَلِّمِ وَفِّهِ التَبجيلا / كادَ المُعَلِّمُ أَن يَكونَ رَسولا.",
        [{"id": "h1", "kind": "step", "text": "العجز يشبه منزلة المعلم بالرسالة.", "penalty": 10}],
        {
            "prompt": "أكمل عجز بيت أحمد شوقي: 'قُم لِلمُعَلِّمِ وَفِّهِ التَبجيلا ...'؟",
            "options": [{"id": "o1", "text": "كادَ المُعَلِّمُ أَن يَكونَ رَسولا"}, {"id": "o2", "text": "إن المعلم شعلة تنير العقولا"}, {"id": "o3", "text": "هو من يبني الأجيال والجيلا"}, {"id": "o4", "text": "فاقضِ الحياة بعلمه مأهولا"}],
            "correctId": "o1"
        },
        ["al-shawqiyyat"]
    ))

    puzzles.append(create_puzzle(
        "poet-ch-3", "choice", "choice", "poetry", 3,
        "من صاحب لقب 'أمير الشعراء' في العصر الحديث؟",
        "أحمد شوقي لُقب بأمير الشعراء في حفل مبايعة شعراء العالم العربي له بالقاهرة عام 1927م.",
        [{"id": "h1", "kind": "step", "text": "شاعر مصري ألف الشوقيات ومسرحية مجنون ليلى.", "penalty": 10}],
        {
            "prompt": "من هو الشاعر الذي بُويع بلقب 'أمير الشعراء'؟",
            "options": [{"id": "o1", "text": "أحمد شوقي"}, {"id": "o2", "text": "حافظ إبراهيم"}, {"id": "o3", "text": "خليل مطران"}, {"id": "o4", "text": "معروف الرصافي"}],
            "correctId": "o1"
        },
        ["tarikh-adab-arabi-zayyat"]
    ))

    puzzles.append(create_puzzle(
        "poet-ch-4", "choice", "choice", "poetry", 4,
        "ما البحر الشعري لبيت: 'على قدر أهل العزم تأتي العزائم'؟",
        "البيت وزنه: فعولن مفاعيلن فعولن مفاعلن، وهو البحر الطويل أشهر بحور الشعر العربي الفصيح.",
        [{"id": "h1", "kind": "step", "text": "أطول بحور الخليل وزناً وشيوعاً في الجاهلية والإسلام.", "penalty": 15}],
        {
            "prompt": "ما هو البحر الشعري الذي نظمت عليه قصيدة 'على قدر أهل العزم'؟",
            "options": [{"id": "o1", "text": "البحر الطويل"}, {"id": "o2", "text": "البحر البسيط"}, {"id": "o3", "text": "بحر الكامل"}, {"id": "o4", "text": "بحر الوافر"}],
            "correctId": "o1"
        },
        ["mizan-dhahab-arud"]
    ))

    puzzles.append(create_puzzle(
        "poet-ch-5", "choice", "choice", "poetry", 5,
        "من قائل: 'أراك عصي الدمع شيمتك الصبر'؟",
        "القائل هو أبو فراس الحمداني الحارث بن سعيد، أمير وشاعر بني حمدان، قالها في أسره عند الروم.",
        [{"id": "h1", "kind": "step", "text": "ابن عم سيف الدولة الحمداني وشاعره الفارس.", "penalty": 15}],
        {
            "prompt": "من قائل قصيدة الروميات الشهيرة التي مطلعها: 'أراك عصي الدمع شيمتك الصبر'؟",
            "options": [{"id": "o1", "text": "أبو فراس الحمداني"}, {"id": "o2", "text": "البحتري"}, {"id": "o3", "text": "الشريف الرضي"}, {"id": "o4", "text": "ابن الرومي"}],
            "correctId": "o1"
        },
        ["diwan-abi-firas"]
    ))

    # 3.10 - 3.13 Matching Poetic Devices & Meanings (matching)
    puzzles.append(create_puzzle(
        "poet-mat-1", "matching", "matching", "poetry", 2,
        "مطابقة معاني الألفاظ في شعر المتنبي",
        "صل المفردة الشعرية بمعناها الدقيق في سياق البيت.",
        [{"id": "h1", "kind": "step", "text": "البيداء هي الصحراء الممتدة الواسعة.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "البيداء"}, {"id": "l2", "text": "القرطاس"}, {"id": "l3", "text": "الحسام"}],
            "right": [{"id": "r1", "text": "الصحراء الواسعة"}, {"id": "r2", "text": "ورق الكتابة"}, {"id": "r3", "text": "السيف القاطع"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["diwan-mutanabbi-sharh-ukbari"]
    ))

    puzzles.append(create_puzzle(
        "poet-mat-2", "matching", "matching", "poetry", 3,
        "نسبة الدواوين لأصحابها",
        "صل كل ديوان بالشاعر الذي أبدعه.",
        [{"id": "h1", "kind": "step", "text": "الشوقيات ديوان أمير الشعراء أحمد شوقي.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "الشوقيات"}, {"id": "l2", "text": "اللزوميات"}, {"id": "l3", "text": "ديوان الحماسة"}],
            "right": [{"id": "r1", "text": "أحمد شوقي"}, {"id": "r2", "text": "أبو العلاء المعري"}, {"id": "r3", "text": "أبو تمام"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["tarikh-adab-arabi-zayyat"]
    ))

    puzzles.append(create_puzzle(
        "poet-mat-3", "matching", "matching", "poetry", 4,
        "مطابقة ألقاب الشعراء",
        "صل كل شاعر عربي بلقبه الأدبي المعروف.",
        [{"id": "h1", "kind": "step", "text": "حافظ إبراهيم لُقب بشاعر النيل.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "حافظ إبراهيم"}, {"id": "l2", "text": "الأخطل"}, {"id": "l3", "text": "أمرؤ القيس"}],
            "right": [{"id": "r1", "text": "شاعر النيل"}, {"id": "r2", "text": "شاعر بني أمية"}, {"id": "r3", "text": "الملك الضليل"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["al-aghani-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "poet-mat-4", "matching", "matching", "poetry", 5,
        "مطابقة مفاتيح بحور الشعر",
        "صل البحر الشعري بمفتاحه العروضي الموضوع لتسهيل حفظه.",
        [{"id": "h1", "kind": "step", "text": "مفتاح الطويل: طويل له دون البحور فضائل.", "penalty": 15}],
        {
            "left": [{"id": "l1", "text": "البحر الطويل"}, {"id": "l2", "text": "بحر البسيط"}, {"id": "l3", "text": "بحر الكامل"}],
            "right": [{"id": "r1", "text": "طويلٌ له دون البحور فضائلُ"}, {"id": "r2", "text": "إن البسيط لديه يبسط الأملُ"}, {"id": "r3", "text": "كمل الجمال من البحور الكاملُ"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["mizan-dhahab-arud"]
    ))

    # 3.14 - 3.17 Text Answer Poetry (text_answer)
    puzzles.append(create_puzzle(
        "poet-txt-1", "text_answer", "text_answer", "poetry", 1,
        "من الشاعر الملقب بـ 'شاعر النيل'؟",
        "حافظ إبراهيم الشاعر المصري الكبير عاصر أحمد شوقي واشتهر بلقب شاعر النيل وشاعر الشعب.",
        [{"id": "h1", "kind": "step", "text": "اسمه الأول حافظ.", "penalty": 10}],
        {
            "prompt": "ما اسم الشاعر العربي الملقب بـ 'شاعر النيل'؟",
            "acceptedAnswers": ["حافظ ابراهيم", "حافظ إبراهيم", "محمد حافظ ابراهيم", "محمد حافظ إبراهيم"]
        },
        ["tarikh-adab-arabi-zayyat"]
    ))

    puzzles.append(create_puzzle(
        "poet-txt-2", "text_answer", "text_answer", "poetry", 2,
        "واضع علم العروض الخليلي",
        "الخليل بن أحمد الفراهيدي الأزدي هو إمام اللغة وواضع علم العروض ومؤلف كتاب العين.",
        [{"id": "h1", "kind": "step", "text": "عالم بصري جليل اسمه الخليل.", "penalty": 10}],
        {
            "prompt": "من هو العالم العربي الذي وضع أوزان الشعر وعلم العروض؟",
            "acceptedAnswers": ["الخليل بن احمد", "الخليل بن أحمد", "الخليل بن احمد الفراهيدي", "الخليل بن أحمد الفراهيدي", "الفراهيدي"]
        },
        ["wafayat-ayan-ibn-khallikan"]
    ))

    puzzles.append(create_puzzle(
        "poet-txt-3", "text_answer", "text_answer", "poetry", 3,
        "أكمل قافية البيت: 'إذا الشعب يوماً أراد الحياة ... فلا بد أن يستجيب ____'",
        "عجز بيت أبي القاسم الشابي الشهير: فلا بد أن يستجيب القدر.",
        [{"id": "h1", "kind": "step", "text": "الكلمة تنتهي بحرف الراء وتدل على القضاء المكتوب.", "penalty": 10}],
        {
            "prompt": "أكمل الكلمة الأخيرة الناقصة في بيت الشابي: 'إذا الشعب يوماً أراد الحياة فلا بد أن يستجيب الـ...'",
            "acceptedAnswers": ["القدر", "قدر"]
        },
        ["diwan-aghani-hayat-shabbi"]
    ))

    puzzles.append(create_puzzle(
        "poet-txt-4", "text_answer", "text_answer", "poetry", 4,
        "ما اسم الشاعر العباسي الملقب بـ 'صريع الغواني'؟",
        "مسلم بن الوليد لُقب بصريع الغواني ببيت شعره الشهير الذي أطلقه عليه الخليفة الرشيد.",
        [{"id": "h1", "kind": "step", "text": "اسمه مركب: مسلم بن...", "penalty": 15}],
        {
            "prompt": "من هو الشاعر العباسي الملقب بـ 'صريع الغواني'؟",
            "acceptedAnswers": ["مسلم بن الوليد", "الوليد"]
        },
        ["al-aghani-isfahani"]
    ))

    # 3.18 - 3.20 Anagram Poetry (anagram)
    puzzles.append(create_puzzle(
        "poet-ana-1", "anagram", "anagram", "poetry", 1,
        "تركيب مصطلح عروضي: قافية",
        "الحروف تكون كلمة 'قافية' وهو الحرف الأخير وما يتبعه في أواخر أبيات القصيدة.",
        [{"id": "h1", "kind": "step", "text": "ما يختم به الشاعر أواخر الأبيات.", "penalty": 10}],
        {
            "tiles": ["ق", "ا", "ف", "ي", "ة"],
            "fixedSlots": [],
            "acceptedAnswers": ["قافية"]
        },
        ["mizan-dhahab-arud"]
    ))

    puzzles.append(create_puzzle(
        "poet-ana-2", "anagram", "anagram", "poetry", 2,
        "تركيب كلمة: معلقة",
        "الحروف تكون 'معلقة' وهي القصيدة الطويلة الجاهلية المنقحة.",
        [{"id": "h1", "kind": "step", "text": "مفرد المعلقات السبع أو العشر.", "penalty": 10}],
        {
            "tiles": ["م", "ع", "ل", "ق", "ة"],
            "fixedSlots": [],
            "acceptedAnswers": ["معلقة"]
        },
        ["sharh-muallaqat-zawzani"]
    ))

    puzzles.append(create_puzzle(
        "poet-ana-3", "anagram", "anagram", "poetry", 3,
        "تركيب كلمة: قصيدة",
        "الحروف المبعثرة تكون كلمة 'قصيدة' وهي النص الشعري المتكامل في العربية.",
        [{"id": "h1", "kind": "step", "text": "تتكون من أبيات متحدة في الوزن والقافية.", "penalty": 10}],
        {
            "tiles": ["ق", "ص", "ي", "د", "ة"],
            "fixedSlots": [],
            "acceptedAnswers": ["قصيدة"]
        },
        ["lisan-arab-qsd"]
    ))

    # =========================================================================
    # WORLD 4: RHETORIC (مملكة البيان) - 20 Puzzles
    # =========================================================================
    # 4.1 - 4.5 Choice Rhetoric
    puzzles.append(create_puzzle(
        "rhet-ch-1", "choice", "choice", "rhetoric", 1,
        "تحديد نوع التشبيه: 'العلم كالنور في الهداية'",
        "تشبيه تام (مفصل ومؤكد) اكتملت فيه الأركان الأربعة: المشبه والمشبه به وأداة التشبيه ووجه الشبه.",
        [{"id": "h1", "kind": "step", "text": "ذُكرت فيه الأداة ووجه الشبه معاً.", "penalty": 10}],
        {
            "prompt": "ما نوع التشبيه في عبارة: 'العلمُ كالنورِ في الهدايةِ'؟",
            "options": [{"id": "o1", "text": "تشبيه تام الأركان (مفصل)"}, {"id": "o2", "text": "تشبيه بليغ"}, {"id": "o3", "text": "تشبيه ضمني"}, {"id": "o4", "text": "استعارة مكنية"}],
            "correctId": "o1"
        },
        ["al-balaghah-wadihah"]
    ))

    puzzles.append(create_puzzle(
        "rhet-ch-2", "choice", "choice", "rhetoric", 2,
        "التشبيه البليغ: حذفت منه الأداة ووجه الشبه",
        "التشبيه البليغ هو ما حُذفت منه أداة التشبيه ووجه الشبه معاً نحو: 'العلم نورٌ'.",
        [{"id": "h1", "kind": "step", "text": "يقوم على ركني المشبه والمشبه به فقط.", "penalty": 10}],
        {
            "prompt": "أي التعبيرات التالية يمثل 'تشبيهاً بليغاً'؟",
            "options": [{"id": "o1", "text": "العلمُ نورٌ"}, {"id": "o2", "text": "العلم كالنور يضيء العقول"}, {"id": "o3", "text": "العلم يشبه النور"}, {"id": "o4", "text": "أضاء العلمُ دروبنا"}],
            "correctId": "o1"
        },
        ["al-balaghah-wadihah"]
    ))

    puzzles.append(create_puzzle(
        "rhet-ch-3", "choice", "choice", "rhetoric", 3,
        "الاستعارة المكنية في البلاغة العربية",
        "الاستعارة المكنية هي التي حُذف فيها المشبه به ورُمز له بشيء من لوازمه نحو 'ضحك الصباح'.",
        [{"id": "h1", "kind": "step", "text": "المشبه به محذوف ورُمز له بصفته.", "penalty": 10}],
        {
            "prompt": "ما المحسن البلاغي في قولنا: 'عضّنا الدهرُ بنابهِ'؟",
            "options": [{"id": "o1", "text": "استعارة مكنية"}, {"id": "o2", "text": "استعارة تصريحية"}, {"id": "o3", "text": "تشبيه مقلوب"}, {"id": "o4", "text": "جناس تام"}],
            "correctId": "o1"
        },
        ["asrar-balaghah-jurjani"]
    ))

    puzzles.append(create_puzzle(
        "rhet-ch-4", "choice", "choice", "rhetoric", 4,
        "الجناس التام في البلاغة",
        "الجناس التام اتفاق اللفظين في نوع الحروف وعددها وترتيبها وحركاتها مع اختلاف المعنى كقولك 'صلّيت المغرب في المغرب'.",
        [{"id": "h1", "kind": "step", "text": "تطابق تام في النطق واختلاف في المعنى.", "penalty": 10}],
        {
            "prompt": "ما نوع الجناس في: 'صليتُ المغربَ في مساجدِ المغربِ'؟",
            "options": [{"id": "o1", "text": "جناس تام"}, {"id": "o2", "text": "جناس ناقص"}, {"id": "o3", "text": "طباق إيجاب"}, {"id": "o4", "text": "مقابلة"}],
            "correctId": "o1"
        },
        ["al-balaghah-wadihah"]
    ))

    puzzles.append(create_puzzle(
        "rhet-ch-5", "choice", "choice", "rhetoric", 5,
        "الفرق بين الطباق والمقابلة",
        "الطباق جمع بين لفظين متضادين، أما المقابلة فهي إيراد معنيين أو أكثر ثم يؤتى بما يقابلها على الترتيب.",
        [{"id": "h1", "kind": "step", "text": "الجمع بين جملتين متقابلتين في عدة معانٍ.", "penalty": 15}],
        {
            "prompt": "ما الفن البلاغي في قوله تعالى: {فَلْيَضْحَكُوا قَلِيلًا وَلْيَبْكُوا كَثِيرًا}؟",
            "options": [{"id": "o1", "text": "مقابلة"}, {"id": "o2", "text": "طباق سلب"}, {"id": "o3", "text": "جناس ناقص"}, {"id": "o4", "text": "تورية"}],
            "correctId": "o1"
        },
        ["al-balaghah-wadihah"]
    ))

    # 4.6 - 4.10 Matching Rhetorical Devices
    puzzles.append(create_puzzle(
        "rhet-mat-1", "matching", "matching", "rhetoric", 2,
        "مطابقة الأمثلة بالفنون البلاغية",
        "صل كل مثال عربي بالفن البلاغي الذي يمثله.",
        [{"id": "h1", "kind": "step", "text": "'ليل داج ونهار ساج' ينتهي بفواصل مسجوعة.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "ليل داج ونهار ساج"}, {"id": "l2", "text": "العلم نور والجهل ظلام"}, {"id": "l3", "text": "يقيننا بالله يقيننا من الهم"}],
            "right": [{"id": "r1", "text": "سجع"}, {"id": "r2", "text": "مقابلة"}, {"id": "r3", "text": "جناس تام"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["al-balaghah-wadihah"]
    ))

    puzzles.append(create_puzzle(
        "rhet-mat-2", "matching", "matching", "rhetoric", 3,
        "أركان علم البلاغة الثلاثة",
        "صل كل علم من علوم البلاغة الثلاثة بموضوعه الدراسي.",
        [{"id": "h1", "kind": "step", "text": "علم المعاني يدرس مطابقة الكلام لمقتضى الحال.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "علم المعاني"}, {"id": "l2", "text": "علم البيان"}, {"id": "l3", "text": "علم البديع"}],
            "right": [{"id": "r1", "text": "مقتضى الحال والتقديم والتأخير"}, {"id": "r2", "text": "التشبيه والاستعارة والكناية"}, {"id": "r3", "text": "المحسنات اللفظية والمعنوية"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["talkhis-miftah-qazwini"]
    ))

    puzzles.append(create_puzzle(
        "rhet-mat-3", "matching", "matching", "rhetoric", 4,
        "أنواع الكناية الثلاثة",
        "صل نوع الكناية بالمثال الدال عليها في البلاغة.",
        [{"id": "h1", "kind": "step", "text": "'فلان كثير الرماد' كناية عن صفة الكرم.", "penalty": 15}],
        {
            "left": [{"id": "l1", "text": "كناية عن صفة"}, {"id": "l2", "text": "كناية عن موصوف"}, {"id": "l3", "text": "كناية عن نسبة"}],
            "right": [{"id": "r1", "text": "كثير الرماد (الكرم)"}, {"id": "r2", "text": "لغة الضاد (العربية)"}, {"id": "r3", "text": "المجد يسير في ركابه"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["dalail-ijaz-jurjani"]
    ))

    puzzles.append(create_puzzle(
        "rhet-mat-4", "matching", "matching", "rhetoric", 5,
        "علاقات المجاز المرسل",
        "صل المثال البلاغي بنوع العلاقة في المجاز المرسل.",
        [{"id": "h1", "kind": "step", "text": "'ألقى الخطيب كلمة' أطلق الجزء وأراد الكل.", "penalty": 15}],
        {
            "left": [{"id": "l1", "text": "ألقى المدير كلمة مؤثرة"}, {"id": "l2", "text": "شربتُ ماء زمزم"}, {"id": "l3", "text": "رعت الماشية الغيث"}],
            "right": [{"id": "r1", "text": "الجزئية"}, {"id": "r2", "text": "الكلية"}, {"id": "r3", "text": "السببية"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["al-balaghah-wadihah"]
    ))

    puzzles.append(create_puzzle(
        "rhet-mat-5", "matching", "matching", "rhetoric", 5,
        "فنون البديع المعنوي واللفظي",
        "صل المحسن البديعي بتصنيفه المعتمد عند البلاغيين.",
        [{"id": "h1", "kind": "step", "text": "الطباق والتورية من المحسنات المعنوية.", "penalty": 15}],
        {
            "left": [{"id": "l1", "text": "الطباق"}, {"id": "l2", "text": "الجناس"}, {"id": "l3", "text": "السجع"}],
            "right": [{"id": "r1", "text": "محسن معنوي"}, {"id": "r2", "text": "محسن لفظي صوتي"}, {"id": "r3", "text": "توافق الفواصل في الحرف الأخير"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["al-balaghah-wadihah"]
    ))

    # 4.11 - 4.14 Text Answer Rhetoric
    puzzles.append(create_puzzle(
        "rhet-txt-1", "text_answer", "text_answer", "rhetoric", 1,
        "ما اسم فن الجمع بين الشيء وضده في الكلام؟",
        "الطباق هو الجمع بين لفظين متضادين في المعنى كالحياة والموت والليل والنهار.",
        [{"id": "h1", "kind": "step", "text": "كلمة من أربعة أحرف تبدأ بالطاء.", "penalty": 10}],
        {
            "prompt": "ما اسم المحسن البديعي الذي يقوم على الجمع بين الكلمة وضدها في المعنى؟",
            "acceptedAnswers": ["الطباق", "طباق", "التضاد", "تضاد"]
        },
        ["al-balaghah-wadihah"]
    ))

    puzzles.append(create_puzzle(
        "rhet-txt-2", "text_answer", "text_answer", "rhetoric", 2,
        "ما اسم توافق فواصل الجمل في الحرف الأخير نثراً؟",
        "السجع هو توافق أواخر الفواصل في النثر العربي كقول أعرابي: ليل داج، وسماء ذات أبراج.",
        [{"id": "h1", "kind": "step", "text": "كلمة تبدأ بالسين وتنتهي بالعين.", "penalty": 10}],
        {
            "prompt": "ماذا يسمى توافق الفواصل في الحرف الأخير في النثر العربي؟",
            "acceptedAnswers": ["السجع", "سجع"]
        },
        ["al-balaghah-wadihah"]
    ))

    puzzles.append(create_puzzle(
        "rhet-txt-3", "text_answer", "text_answer", "rhetoric", 3,
        "ما الفن البلاغي القائم على تشبيه حُذفت أداته ووجه شبهه؟",
        "التشبيه البليغ هو ما حذف منه الأداة ووجه الشبه كقولنا: الصبر درع.",
        [{"id": "h1", "kind": "step", "text": "تشبيه مقترن بوصف البلاغة والقوة.", "penalty": 10}],
        {
            "prompt": "ماذا يسمى التشبيه الذي اقتصر على المشبه والمشبه به وحذفت منه الأداة ووجه الشبه؟",
            "acceptedAnswers": ["التشبيه البليغ", "تشبيه بليغ", "البليغ"]
        },
        ["al-balaghah-wadihah"]
    ))

    puzzles.append(create_puzzle(
        "rhet-txt-4", "text_answer", "text_answer", "rhetoric", 4,
        "من صاحب كتاب 'دلائل الإعجاز' وإمام نظرية النظم؟",
        "عبد القاهر الجرجاني أبو بكر إمام البلاغة وواضع نظريتي النظم والإعجاز في كتابيه دلائل الإعجاز وأسرار البلاغة.",
        [{"id": "h1", "kind": "step", "text": "العالم الجرجاني واضع نظرية النظم.", "penalty": 15}],
        {
            "prompt": "من هو الإمام البلاغي مؤلف كتاب 'دلائل الإعجاز' ومؤسس نظرية النظم؟",
            "acceptedAnswers": ["عبد القاهر الجرجاني", "الجرجاني", "عبدالقاهر الجرجاني"]
        },
        ["dalail-ijaz-jurjani"]
    ))

    # 4.15 - 4.17 Order Rhetoric
    puzzles.append(create_puzzle(
        "rhet-ord-1", "order", "order", "rhetoric", 2,
        "ترتيب أركان التشبيه التام الأربعة",
        "الأركان الأربعة بالترتيب المنطقي: المشبه، أداة التشبيه، المشبه به، وجه الشبه.",
        [{"id": "h1", "kind": "step", "text": "البداية بالمشبه ثم الأداة.", "penalty": 10}],
        {
            "items": [
                {"id": "i1", "text": "المشبه"},
                {"id": "i2", "text": "أداة التشبيه"},
                {"id": "i3", "text": "المشبه به"},
                {"id": "i4", "text": "وجه الشبه"}
            ],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "procedure"
        },
        ["al-balaghah-wadihah"]
    ))

    puzzles.append(create_puzzle(
        "rhet-ord-2", "order", "order", "rhetoric", 3,
        "ترتيب حكمة مسجوعة: قس بن ساعدة",
        "خطبة قس بن ساعدة الإيادي الشهيرة: ليل داج، ونهار ساج، وسماء ذات أبراج.",
        [{"id": "h1", "kind": "step", "text": "يبدأ بالليل الداج ثم النهار الساج.", "penalty": 10}],
        {
            "items": [
                {"id": "i1", "text": "ليلٌ داج"},
                {"id": "i2", "text": "ونهارٌ ساج"},
                {"id": "i3", "text": "وسماءٌ ذاتُ أبراج"}
            ],
            "correctOrder": ["i1", "i2", "i3"],
            "variant": "sentence"
        },
        ["al-bayan-tabyin-jahiz"]
    ))

    puzzles.append(create_puzzle(
        "rhet-ord-3", "order", "order", "rhetoric", 4,
        "ترتيب جمل المقابلة البلاغية",
        "قول الأديب: فالعلم يرفع بيوتاً لا عماد لها، والجهل يهدم بيت العز والشرف.",
        [{"id": "h1", "kind": "step", "text": "جملة رفع العلم تسبق هدم الجهل.", "penalty": 15}],
        {
            "items": [
                {"id": "i1", "text": "العلم يرفع بيوتاً"},
                {"id": "i2", "text": "لا عماد لها"},
                {"id": "i3", "text": "والجهل يهدم"},
                {"id": "i4", "text": "بيت العز والشرف"}
            ],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "poetry"
        },
        ["al-balaghah-wadihah"]
    ))

    # 4.18 - 4.20 Crosswords Rhetoric
    puzzles.append(create_puzzle(
        "rhet-cw-1", "crossword", "crossword", "rhetoric", 1,
        "تقاطعات بديعية: سجع ومجاز",
        "السجع توافق الفواصل، والمجاز صرف اللفظ عن حقيقته لعلاقة وقرينة.",
        [{"id": "h1", "kind": "step", "text": "سجع يبدأ بالسين.", "penalty": 15}],
        {
            "rows": 3, "cols": 3,
            "entries": [
                {"id": "e1", "row": 0, "col": 2, "direction": "across", "tokens": ["س", "ج", "ع"], "clue": "توافق أواخر فواصل النثر", "sourceRef": "al-balaghah-wadihah"},
                {"id": "e2", "row": 0, "col": 1, "direction": "down", "tokens": ["ج", "د"], "clue": "عكس هزل واجتهد في الأمر", "sourceRef": "al-balaghah-wadihah"}
            ]
        },
        ["al-balaghah-wadihah"]
    ))

    puzzles.append(create_puzzle(
        "rhet-cw-2", "crossword", "crossword", "rhetoric", 2,
        "تقاطعات المعاني: خبر وأمر",
        "الخبر يحتمل الصدق والكذب لذاته، والأمر طلب الفعل على وجه الاستعلاء.",
        [{"id": "h1", "kind": "step", "text": "الخبر هو ما يصح أن يقال لقائله صادق أو كاذب.", "penalty": 15}],
        {
            "rows": 3, "cols": 3,
            "entries": [
                {"id": "e1", "row": 0, "col": 2, "direction": "across", "tokens": ["خ", "ب", "ر"], "clue": "الكلام المحتمل للصدق والكذب", "sourceRef": "al-balaghah-wadihah"},
                {"id": "e2", "row": 0, "col": 1, "direction": "down", "tokens": ["ب", "ر"], "clue": "الإحسان والصدق في اليمين", "sourceRef": "al-balaghah-wadihah"}
            ]
        },
        ["al-balaghah-wadihah"]
    ))

    puzzles.append(create_puzzle(
        "rhet-cw-3", "crossword", "crossword", "rhetoric", 3,
        "تقاطعات البيان: بيان وعلم",
        "علم البيان يدرس التعبير عن المعنى الواحد بطرق متعددة.",
        [{"id": "h1", "kind": "step", "text": "البيان هو الإيضاح والإفصاح.", "penalty": 15}],
        {
            "rows": 4, "cols": 4,
            "entries": [
                {"id": "e1", "row": 0, "col": 3, "direction": "across", "tokens": ["ب", "ي", "ا", "ن"], "clue": "الإفصاح وعلم التشبيه والاستعارة", "sourceRef": "al-balaghah-wadihah"},
                {"id": "e2", "row": 0, "col": 2, "direction": "down", "tokens": ["ي", "د"], "clue": "الجراحة والنعمة والمجاز في القوة", "sourceRef": "al-balaghah-wadihah"}
            ]
        },
        ["al-balaghah-wadihah"]
    ))

    # =========================================================================
    # WORLD 5: QURAN (كنوز الألفاظ القرآنية) - 20 Puzzles
    # All verified against authentic classical sources:
    # Raghib al-Isfahani (Mufradat), Ibn Kathir, Tabari, Ibn Qutaybah.
    # =========================================================================
    # 5.1 - 5.5 Matching Gharib al-Quran & Verified Meanings
    puzzles.append(create_puzzle(
        "quran-mat-1", "matching", "matching", "quran", 1,
        "معاني غريب القرآن: سورة الفلق",
        "معاني ألفاظ سورة الفلق كما قررها أئمة التفسير وغريب القرآن.",
        [{"id": "h1", "kind": "step", "text": "الفلق هو الصبح، والغاسق هو الليل إذا أظلم.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "الفلق"}, {"id": "l2", "text": "غاسق"}, {"id": "l3", "text": "وقب"}],
            "right": [{"id": "r1", "text": "الصبح وانفلاقه"}, {"id": "r2", "text": "الليل إذا اشتد ظلامه"}, {"id": "r3", "text": "دخل وسرى ظلامه"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["tafsir-ibn-kathir-falaq", "mufradat-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "quran-mat-2", "matching", "matching", "quran", 2,
        "معاني غريب القرآن: سورة الإخلاص والناس",
        "الصمد السيد المقصود في الحوائج الذي لا جوف له، والوسواس ملقي الخواطر الرديئة.",
        [{"id": "h1", "kind": "step", "text": "الصمد هو المقصود لقضاء الحوائج سبحانه.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "الصمد"}, {"id": "l2", "text": "الوسواس"}, {"id": "l3", "text": "الخناس"}],
            "right": [{"id": "r1", "text": "السيد المقصود في الحوائج سبحانه"}, {"id": "r2", "text": "الشيطان الملقي للخواطر"}, {"id": "r3", "text": "المتأخر المتواري عند ذكر الله"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["tafsir-tabari-ikhlas", "mufradat-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "quran-mat-3", "matching", "matching", "quran", 3,
        "فروق الألفاظ القرآنية: الغيث والمطر",
        "في الاستعمال القرآني الغالب: الغيث للرحمة والنفع، والمطر في مواضع العذاب والعقوبة.",
        [{"id": "h1", "kind": "step", "text": "الغيث ينزل لإغاثة الأرض بعد القنوط.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "الغيث"}, {"id": "l2", "text": "المطر (في الغالب القرآني)"}, {"id": "l3", "text": "الوابل"}],
            "right": [{"id": "r1", "text": "الماء النازل رحمة وإغاثة"}, {"id": "r2", "text": "النازل في سياق العقوبة والإنذار"}, {"id": "r3", "text": "المطر الشديد العظيم القطر"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["furuq-lughawiyya-askari", "mufradat-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "quran-mat-4", "matching", "matching", "quran", 4,
        "ألفاظ السحاب في التعبير القرآني",
        "المعصرات السحائب الممطرة، والركام السحاب المتراكم بعضه فوق بعض.",
        [{"id": "h1", "kind": "step", "text": "{وَأَنْزَلْنَا مِنَ الْمُعْصِرَاتِ مَاءً ثَجَّاجًا} المعصرات السحب الممطرة.", "penalty": 15}],
        {
            "left": [{"id": "l1", "text": "المعصرات"}, {"id": "l2", "text": "ثجاجاً"}, {"id": "l3", "text": "ركاماً"}],
            "right": [{"id": "r1", "text": "السحب التي حان أن تمطر"}, {"id": "r2", "text": "منصباً متتابعاً بكثرة"}, {"id": "r3", "text": "مجتمعاً متراكباً بعضه فوق بعض"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["tafsir-ibn-kathir-naba", "mufradat-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "quran-mat-5", "matching", "matching", "quran", 5,
        "ألفاظ اليوم الآخر والقيامة",
        "القارعة التي تقرع القلوب بأهوالها، والراجفة النفخة الأولى التي ترجف بها الأرض.",
        [{"id": "h1", "kind": "step", "text": "القارعة من أسماء القيامة لشدة قرعها.", "penalty": 15}],
        {
            "left": [{"id": "l1", "text": "القارعة"}, {"id": "l2", "text": "الصاخة"}, {"id": "l3", "text": "الغاشية"}],
            "right": [{"id": "r1", "text": "التي تقرع القلوب بأهوالها"}, {"id": "r2", "text": "الصيحة التي تصخ الآذان لشدتها"}, {"id": "r3", "text": "التي تغشى الناس بعظيم أهوالها"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["mufradat-isfahani", "tafsir-tabari"]
    ))

    # 5.6 - 5.10 Choice Quranic Semantics
    puzzles.append(create_puzzle(
        "quran-ch-1", "choice", "choice", "quran", 1,
        "معنى مفردة: {الصَّمَدُ}",
        "قال ابن عباس وغيره: الصمد هو السيد الذي يُصمد إليه ويُقصد في الحوائج سبحانه وتعالى.",
        [{"id": "h1", "kind": "step", "text": "المقصود لقضاء الحوائج الذي تصمد إليه الخلائق.", "penalty": 10}],
        {
            "prompt": "ما المعنى المعتمد في كتب التفسير لكلمة {الصَّمَدُ} في سورة الإخلاص؟",
            "options": [{"id": "o1", "text": "السيد المقصود في الحوائج الذي كمل في صفاته"}, {"id": "o2", "text": "القوي المتين"}, {"id": "o3", "text": "الرازق الكريم"}, {"id": "o4", "text": "العالم بالسرائر"}],
            "correctId": "o1"
        },
        ["tafsir-ibn-kathir-ikhlas", "mufradat-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "quran-ch-2", "choice", "choice", "quran", 2,
        "الفرق بين 'السَّنَة' و'العَام' في السياق القرآني",
        "السنة غالباً ما تستعمل في سياق الشدة والجدب {فلبث فيهم ألف سنة إلا خمسين عاماً}، والعام في الرخاء والخصب.",
        [{"id": "h1", "kind": "step", "text": "العام ارتبط بالرخاء في سورة يوسف {ثم يأتي من بعد ذلك عام فيه يغاث الناس}.", "penalty": 10}],
        {
            "prompt": "كيف فرق البلاغيون والمفسرون بين لفظي 'السنة' و'العام' في الاستعمال القرآني الغالب؟",
            "options": [{"id": "o1", "text": "السنة في مواضع الشدة والجدب، والعام في الرخاء والنعمة"}, {"id": "o2", "text": "العام أطول من السنة في العد الحسابي"}, {"id": "o3", "text": "السنة شمسية والعام قمري"}, {"id": "o4", "text": "لا فرق بينهما في الاستعمال القرآني"}],
            "correctId": "o1"
        },
        ["furuq-lughawiyya-askari", "mufradat-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "quran-ch-3", "choice", "choice", "quran", 3,
        "معنى قوله تعالى: {فِي جِيدِهَا حَبْلٌ مِنْ مَسَدٍ}",
        "الجيد هو العنق، والمسد هو الليف المفتول فتلاً شديداً في لغة العرب وتفسير السلف.",
        [{"id": "h1", "kind": "step", "text": "الجيد موضع القلادة في العنق.", "penalty": 10}],
        {
            "prompt": "ما معنى كلمة {جِيدِهَا} في سورة المسد؟",
            "options": [{"id": "o1", "text": "عُنُقها"}, {"id": "o2", "text": "رأسها"}, {"id": "o3", "text": "يدها"}, {"id": "o4", "text": "صدرها"}],
            "correctId": "o1"
        },
        ["tafsir-ibn-kathir-masad", "gharib-quran-ibn-qutaybah"]
    ))

    puzzles.append(create_puzzle(
        "quran-ch-4", "choice", "choice", "quran", 4,
        "معنى مفردة: {حَصْحَصَ الحَقُّ}",
        "حصحص الحق أي ظهر وتبين وانكشف بعد خفائه، مأخوذ من حص الشعر إذا انكشف ما تحته.",
        [{"id": "h1", "kind": "step", "text": "ظهر وتبين للعيان بعد كتمانه.", "penalty": 15}],
        {
            "prompt": "ما معنى قوله تعالى على لسان امرأة العزيز: {الْآنَ حَصْحَصَ الْحَقُّ}؟",
            "options": [{"id": "o1", "text": "ظهر وتبين وانكشف"}, {"id": "o2", "text": "قَوِيَ وثبت"}, {"id": "o3", "text": "ضاع واختفى"}, {"id": "o4", "text": "طال انتظاره"}],
            "correctId": "o1"
        },
        ["tafsir-tabari-yusuf", "mufradat-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "quran-ch-5", "choice", "choice", "quran", 5,
        "معنى قوله تعالى: {وَاللَّيْلِ إِذَا عَسْعَسَ}",
        "عسعس الليل في اللغة من الأضداد: أقبل بظلامه أو أدبر وذهب، وأكثر المفسرين على أنه أدبر لقوله بعده {وَالصُّبْحِ إِذَا تَنَفَّسَ}.",
        [{"id": "h1", "kind": "step", "text": "من ألفاظ الأضداد ويعني في سياق الآية إدباره وولايته.", "penalty": 15}],
        {
            "prompt": "ما المعنى الراجح لقوله تعالى: {وَاللَّيْلِ إِذَا عَسْعَسَ} في سورة التكوير؟",
            "options": [{"id": "o1", "text": "أدبر بظلامه وولّى"}, {"id": "o2", "text": "اشتد برده"}, {"id": "o3", "text": "كثرت رياحه"}, {"id": "o4", "text": "طال زمانه"}],
            "correctId": "o1"
        },
        ["tafsir-ibn-kathir-takwir", "mufradat-isfahani"]
    ))

    # 5.11 - 5.14 Text Answer Quranic Vocabulary Roots
    puzzles.append(create_puzzle(
        "quran-txt-1", "text_answer", "text_answer", "quran", 1,
        "الجذر الثلاثي لكلمة {الرَّحْمَٰن}",
        "الرحمن مشتق من الرحمة، وجذره اللغوي الثلاثي هو (رحم).",
        [{"id": "h1", "kind": "step", "text": "ثلاثة أحرف: الراء ثم الحاء ثم الميم.", "penalty": 10}],
        {
            "prompt": "ما الجذر اللغوي الثلاثي لاسم الله تعالى 'الرَّحْمَٰن'؟",
            "acceptedAnswers": ["رحم", "ر ح م", "الرحمة"]
        },
        ["mufradat-isfahani", "lisan-arab-rhm"]
    ))

    puzzles.append(create_puzzle(
        "quran-txt-2", "text_answer", "text_answer", "quran", 2,
        "معنى مفردة {الْكَوْثَر}",
        "الكوثر في لغة العرب فَوْعَل من الكثرة، وهو الخير الكثير، ومنه نهر الكوثر الذي أعطيه النبي ﷺ في الجنة.",
        [{"id": "h1", "kind": "step", "text": "يدل على الزيادة والخير الوفير غير المنقطع.", "penalty": 10}],
        {
            "prompt": "ما المعنى اللغوي لكلمة {الْكَوْثَر} المشتقة من الكثرة؟",
            "acceptedAnswers": ["الخير الكثير", "خير كثير", "الكثير من الخير", "النهر العظيم"]
        },
        ["tafsir-ibn-kathir-kawthar", "mufradat-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "quran-txt-3", "text_answer", "text_answer", "quran", 3,
        "ما اسم الشجرة الملعونة في القرآن؟",
        "شجرة الزقوم جعلها الله فتنة للظالمين، تنبت في أصل الجحيم طعام الأثيم.",
        [{"id": "h1", "kind": "step", "text": "شجرة طعام أهل النار تبدأ بحرف الزاي.", "penalty": 10}],
        {
            "prompt": "ما اسم الشجرة التي ذكرت في القرآن الكريم بأنها تنبت في أصل الجحيم؟",
            "acceptedAnswers": ["الزقوم", "شجرة الزقوم", "زقوم"]
        },
        ["tafsir-ibn-kathir-saffat", "mufradat-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "quran-txt-4", "text_answer", "text_answer", "quran", 4,
        "ما مفرد كلمة {أَبَابِيل} في قوله تعالى: {طَيْرًا أَبَابِيلَ}؟",
        "الأبابيل هي الجماعات المتفرقة المتتابعة، قال النحويون: واحدها إِبِّيل أو إِبَّوْل.",
        [{"id": "h1", "kind": "step", "text": "معناها جماعات متفرقة زمرة بعد زمرة.", "penalty": 15}],
        {
            "prompt": "ما المعنى المعتمد في اللغة والتفسير لكلمة {أَبَابِيلَ}؟",
            "acceptedAnswers": ["جماعات متفرقة", "جماعات", "فرق متتابعة", "متفرقة متتابعة"]
        },
        ["tafsir-tabari-fil", "gharib-quran-ibn-qutaybah"]
    ))

    # 5.15 - 5.17 Order Verified Quranic Phrases
    puzzles.append(create_puzzle(
        "quran-ord-1", "order", "order", "quran", 1,
        "ترتيب كلمات آية البسملة المباركة",
        "النص الثابت في المصحف: بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ.",
        [{"id": "h1", "kind": "step", "text": "البداية باسم الله تعالى.", "penalty": 10}],
        {
            "items": [
                {"id": "i1", "text": "بِسْمِ"},
                {"id": "i2", "text": "اللَّهِ"},
                {"id": "i3", "text": "الرَّحْمَٰنِ"},
                {"id": "i4", "text": "الرَّحِيمِ"}
            ],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "sentence"
        },
        ["quran-uthmani-fatihah"]
    ))

    puzzles.append(create_puzzle(
        "quran-ord-2", "order", "order", "quran", 2,
        "ترتيب مطلع سورة الإخلاص",
        "قوله تعالى: قُلْ هُوَ اللَّهُ أَحَدٌ.",
        [{"id": "h1", "kind": "step", "text": "يبدأ بالأمر الإلهي 'قُلْ'.", "penalty": 10}],
        {
            "items": [
                {"id": "i1", "text": "قُلْ"},
                {"id": "i2", "text": "هُوَ"},
                {"id": "i3", "text": "اللَّهُ"},
                {"id": "i4", "text": "أَحَدٌ"}
            ],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "sentence"
        },
        ["quran-uthmani-ikhlas"]
    ))

    puzzles.append(create_puzzle(
        "quran-ord-3", "order", "order", "quran", 3,
        "ترتيب الآية الكريمة: {وَقُلْ رَبِّ زِدْنِي عِلْمًا}",
        "دعاء طلب زيادة العلم في سورة طه (الآية 114): وَقُلْ رَبِّ زِدْنِي عِلْمًا.",
        [{"id": "h1", "kind": "step", "text": "الدعاء النبوي الشريف لطلب العلم والهدى.", "penalty": 10}],
        {
            "items": [
                {"id": "i1", "text": "وَقُلْ"},
                {"id": "i2", "text": "رَبِّ"},
                {"id": "i3", "text": "زِدْنِي"},
                {"id": "i4", "text": "عِلْمًا"}
            ],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "sentence"
        },
        ["quran-uthmani-taha"]
    ))

    # 5.18 - 5.20 Wordsearch Quranic Terms
    puzzles.append(create_puzzle(
        "quran-ws-1", "wordsearch", "wordsearch", "quran", 1,
        "البحث عن أسماء الكتب السماوية في القرآن",
        "الكتب المذكورة في التنزيل: قرآن، توراة، إنجيل.",
        [{"id": "h1", "kind": "step", "text": "ابحث عن كلمة 'قرآن' أفقياً.", "penalty": 10}],
        {
            "grid": [
                ["ق", "ر", "ا", "ن"],
                ["ت", "و", "ر", "ة"],
                ["ص", "ح", "ف", "ك"]
            ],
            "words": [
                {"id": "w1", "label": "قران", "path": [{"row": 0, "col": 0}, {"row": 0, "col": 1}, {"row": 0, "col": 2}, {"row": 0, "col": 3}]},
                {"id": "w2", "label": "تورة", "path": [{"row": 1, "col": 0}, {"row": 1, "col": 1}, {"row": 1, "col": 2}, {"row": 1, "col": 3}]}
            ]
        },
        ["mufradat-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "quran-ws-2", "wordsearch", "wordsearch", "quran", 2,
        "البحث عن أوقات الصلاة في التعبير القرآني",
        "الأوقات المذكورة في القرآن: فجر، عشاء.",
        [{"id": "h1", "kind": "step", "text": "ابحث عن كلمة 'فجر' في السطر الأول.", "penalty": 10}],
        {
            "grid": [
                ["ف", "ج", "ر"],
                ["ع", "ش", "ا"],
                ["ل", "ي", "ل"]
            ],
            "words": [
                {"id": "w1", "label": "فجر", "path": [{"row": 0, "col": 0}, {"row": 0, "col": 1}, {"row": 0, "col": 2}]},
                {"id": "w2", "label": "عشا", "path": [{"row": 1, "col": 0}, {"row": 1, "col": 1}, {"row": 1, "col": 2}]}
            ]
        },
        ["mufradat-isfahani"]
    ))

    puzzles.append(create_puzzle(
        "quran-ws-3", "wordsearch", "wordsearch", "quran", 3,
        "البحث عن مفردات البر والإحسان",
        "مفردات قرآنية تدل على الفضائل: بر، تقوى، عدل.",
        [{"id": "h1", "kind": "step", "text": "ابحث عن 'بر' رأسياً.", "penalty": 15}],
        {
            "grid": [
                ["ب", "ع", "د"],
                ["ر", "د", "ل"],
                ["ت", "ق", "و"]
            ],
            "words": [
                {"id": "w1", "label": "بر", "path": [{"row": 0, "col": 0}, {"row": 1, "col": 0}]},
                {"id": "w2", "label": "عدل", "path": [{"row": 0, "col": 1}, {"row": 1, "col": 1}, {"row": 1, "col": 2}]}
            ]
        },
        ["mufradat-isfahani"]
    ))

    # =========================================================================
    # WORLD 6: LOGIC (مجلس الأذكياء) - 20 Puzzles
    # =========================================================================
    # 6.1 - 6.6 Sequences (sequence)
    for i, (terms, ans, rule) in enumerate([
        ([2, 4, 6, 8], 10, "متتالية الأعداد الزوجية بإضافة اثنين في كل خطوة."),
        ([5, 10, 15, 20], 25, "متتالية مضاعفات الخمسة بإضافة 5 في كل خطوة."),
        ([1, 4, 7, 10], 13, "متتالية حسابية أساسها ثلاثة: إضافة 3 لكل حد."),
        ([10, 20, 30, 40], 50, "متتالية العشرات بإضافة عشرة في كل حد."),
        ([3, 6, 12, 24], 48, "متتالية هندسية بالضرب في اثنين في كل خطوة."),
        ([100, 90, 80, 70], 60, "متتالية تنازلية بطرح عشرة في كل خطوة.")
    ], 1):
        puzzles.append(create_puzzle(
            f"logic-seq-{i}", "sequence", "sequence", "logic", (i % 5) + 1,
            f"المتتالية الرياضية: نمط {i}",
            rule,
            [{"id": "h1", "kind": "step", "text": f"القاعدة الرياضية: {rule}", "penalty": 15}],
            {
                "terms": terms,
                "answer": ans,
                "rule": rule
            },
            ["original-math-fixture"]
        ))

    # 6.7 - 6.10 Deductive Choice (choice)
    puzzles.append(create_puzzle(
        "logic-ch-1", "choice", "choice", "logic", 1,
        "الاستنتاج المنطقي: مقارنة الأطوال",
        "إذا كان زيد أطول من عمرو، وكان عمرو أطول من بكر، فإن زيداً هو الأطول بالضرورة المنطقية.",
        [{"id": "h1", "kind": "step", "text": "خاصية التعدي في علاقة المقارنة الرياضية.", "penalty": 10}],
        {
            "prompt": "إذا كان خالد أطول من سعيد، وسعيد أطول من ماجد، فمن هو الأطول بينهم؟",
            "options": [{"id": "o1", "text": "خالد"}, {"id": "o2", "text": "سعيد"}, {"id": "o3", "text": "ماجد"}, {"id": "o4", "text": "متساوون في الطول"}],
            "correctId": "o1"
        },
        ["usul-mantiq-muzaffar"]
    ))

    puzzles.append(create_puzzle(
        "logic-ch-2", "choice", "choice", "logic", 2,
        "القياس المنطقي: شمول الفئات",
        "كل إنسان مفكر، وكل فيلسوف إنسان، إذن بالقياس المنطقي الأرسطي: كل فيلسوف مفكر.",
        [{"id": "h1", "kind": "step", "text": "قاعدة استغراق الحد الأوسط في القياس.", "penalty": 10}],
        {
            "prompt": "المقدمة الكبرى: 'كل إنسان ناطق'، والمقدمة الصغرى: 'سقراط إنسان'؛ ما النتيجة المنطقية الحتمية؟",
            "options": [{"id": "o1", "text": "سقراط ناطق"}, {"id": "o2", "text": "سقراط فيلسوف"}, {"id": "o3", "text": "كل ناطق هو سقراط"}, {"id": "o4", "text": "ليس كل إنسان ناطقاً"}],
            "correctId": "o1"
        },
        ["usul-mantiq-muzaffar"]
    ))

    puzzles.append(create_puzzle(
        "logic-ch-3", "choice", "choice", "logic", 3,
        "الاستلزام ونفي النتيجة",
        "قاعدة نفي النتيجة: إذا كانت السماء تمطر فالأرض مبللة. الأرض ليست مبللة، إذن السماء لا تمطر.",
        [{"id": "h1", "kind": "step", "text": "نفي اللازم يستلزم بالضرورة نفي الملزوم.", "penalty": 15}],
        {
            "prompt": "إذا كان هطول المطر يستلزم تبلل الأرض، وتبين قطعاً أن الأرض جافة غير مبللة؛ فماذا نستنتج؟",
            "options": [{"id": "o1", "text": "السماء لم تمطر حتماً"}, {"id": "o2", "text": "السماء أمطرت قتيلاً"}, {"id": "o3", "text": "المطر سينزل غداً"}, {"id": "o4", "text": "لا يمكن معرفة حالة السماء"}],
            "correctId": "o1"
        },
        ["usul-mantiq-muzaffar"]
    ))

    puzzles.append(create_puzzle(
        "logic-ch-4", "choice", "choice", "logic", 4,
        "المغالطة المنطقية: الاحتكام إلى الشعبية",
        "مغالطة الاحتكام للجمهور (Ad Populum) هي اعتبار الفكرة صحيحة لمجرد شيوعها بين أكثرية الناس.",
        [{"id": "h1", "kind": "step", "text": "كثرة القائلين بالشيء لا تدل بمفردها على صحته البرهانية.", "penalty": 15}],
        {
            "prompt": "ما اسم المغالطة المنطقية في القول: 'هذا الرأي صحيح حتماً لأن أغلب الناس يعتقدون به'؟",
            "options": [{"id": "o1", "text": "مغالطة الاحتكام إلى الأكثرية (الشعبية)"}, {"id": "o2", "text": "مغالطة رجل القش"}, {"id": "o3", "text": "مغالطة الرنجة الحمراء"}, {"id": "o4", "text": "مغالطة المنحدر الزلق"}],
            "correctId": "o1"
        },
        ["mughalataat-mantiqiyya-adel"]
    ))

    # 6.11 - 6.14 Text Answer Logic Riddles
    puzzles.append(create_puzzle(
        "logic-txt-1", "text_answer", "text_answer", "logic", 1,
        "لغز رياضي: القلمان والثلاثة",
        "إذا كان لديك قلمان وأهداك صديقك قلماً آخر، فكم قلماً صار لديك؟",
        [{"id": "h1", "kind": "step", "text": "اجمع اثنين وواحد.", "penalty": 5}],
        {
            "prompt": "لديك قلمان وأضيف إليهما قلم واحد؛ كم قلماً أصبح معك؟ (اكتب رقماً أو كتابة)",
            "acceptedAnswers": ["3", "٣", "ثلاثة", "ثلاث"]
        },
        ["original-math-fixture"]
    ))

    puzzles.append(create_puzzle(
        "logic-txt-2", "text_answer", "text_answer", "logic", 2,
        "لغز منطقي: أخت خالك",
        "إذا كانت أخت خالك الوحيدة فمن تكون بالنسبة لك؟ هي أمك قطعاً.",
        [{"id": "h1", "kind": "step", "text": "الخال هو أخو الأم.", "penalty": 10}],
        {
            "prompt": "أخت خالك الوحيدة وليست لك خالة أخرى؛ من تكون بالنسبة لك؟",
            "acceptedAnswers": ["أمي", "امي", "الام", "الأم", "أمك", "امك"]
        },
        ["al-mantiq-tarafat-turath"]
    ))

    puzzles.append(create_puzzle(
        "logic-txt-3", "text_answer", "text_answer", "logic", 3,
        "حساب زوايا المثلث الهندسي",
        "مجموع قياسات الزوايا الداخلية لأي مثلث في الهندسة الإقليدية المستوية هو 180 درجة.",
        [{"id": "h1", "kind": "step", "text": "العدد يساوي مئة وثمانين.", "penalty": 10}],
        {
            "prompt": "ما مجموع قياسات الزوايا الداخلية لأي مثلث بالدرجات؟",
            "acceptedAnswers": ["180", "١٨٠", "مئة وثمانون", "مائة وثمانون"]
        },
        ["usul-handasa-uqlidis"]
    ))

    puzzles.append(create_puzzle(
        "logic-txt-4", "text_answer", "text_answer", "logic", 4,
        "العدد الأولي الفردي الأصغر",
        "الأعداد الأولية تبدأ بـ 2 وهو زوجي، وأصغر عدد أولي فردي هو العدد 3.",
        [{"id": "h1", "kind": "step", "text": "يقبل القسمة فقط على نفسه والواحد وهو فردي.", "penalty": 10}],
        {
            "prompt": "ما هو أصغر عدد أولي فردي في الرياضيات؟",
            "acceptedAnswers": ["3", "٣", "ثلاثة", "ثلاث"]
        },
        ["original-math-fixture"]
    ))

    # 6.15 - 6.17 Matching Logic Analogies
    puzzles.append(create_puzzle(
        "logic-mat-1", "matching", "matching", "logic", 2,
        "التناظر المنطقي: الجزء والكل",
        "صل الجزء بالكل المنطقي الذي ينتمي إليه بالضرورة.",
        [{"id": "h1", "kind": "step", "text": "الغصن جزء من الشجرة والصفحة جزء من الكتاب.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "غصن"}, {"id": "l2", "text": "صفحة"}, {"id": "l3", "text": "عجلة"}],
            "right": [{"id": "r1", "text": "شجرة"}, {"id": "r2", "text": "كتاب"}, {"id": "r3", "text": "سيارة"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["tanazur-mantiqi-qiyas"]
    ))

    puzzles.append(create_puzzle(
        "logic-mat-2", "matching", "matching", "logic", 3,
        "التناظر المنطقي: السبب والنتيجة",
        "صل السبب المباشر بالنتيجة المترتبة عليه منطقياً وطبيعياً.",
        [{"id": "h1", "kind": "step", "text": "الاجتهاد سبب النجاح والإهمال سبب الإخفاق.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "اجتهاد"}, {"id": "l2", "text": "مطر"}, {"id": "l3", "text": "إهمال"}],
            "right": [{"id": "r1", "text": "نجاح"}, {"id": "r2", "text": "نماء وزرع"}, {"id": "r3", "text": "إخفاق"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["tanazur-mantiqi-qiyas"]
    ))

    puzzles.append(create_puzzle(
        "logic-mat-3", "matching", "matching", "logic", 4,
        "التناظر المنطقي: الصانع ومادته",
        "صل كل صانع بالمادة الأساسية التي يشكلها في عمله.",
        [{"id": "h1", "kind": "step", "text": "النجار يتعامل مع الخشب والخزاف مع الطين.", "penalty": 10}],
        {
            "left": [{"id": "l1", "text": "نجار"}, {"id": "l2", "text": "خزاف"}, {"id": "l3", "text": "حداد"}],
            "right": [{"id": "r1", "text": "خشب"}, {"id": "r2", "text": "طين"}, {"id": "r3", "text": "حديد"}],
            "pairs": [{"leftId": "l1", "rightId": "r1"}, {"leftId": "l2", "rightId": "r2"}, {"leftId": "l3", "rightId": "r3"}]
        },
        ["tanazur-mantiqi-qiyas"]
    ))

    # 6.18 - 6.20 Order Logic Procedures
    puzzles.append(create_puzzle(
        "logic-ord-1", "order", "order", "logic", 1,
        "الخطوات المنهجية لحل المشكلات",
        "الترتيب المنطقي السليم: فهم المشكلة، وضع الفرضيات، التجربة، الوصول للحل.",
        [{"id": "h1", "kind": "step", "text": "الخطوة الأولى دائماً هي الفهم والتحليل.", "penalty": 10}],
        {
            "items": [
                {"id": "i1", "text": "فهم المشكلة وتحديدها"},
                {"id": "i2", "text": "وضع الفروض والحلول الممكنة"},
                {"id": "i3", "text": "اختبار الفروض والتجربة"},
                {"id": "i4", "text": "استخلاص الحل والتطبيق"}
            ],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "procedure"
        },
        ["manhaj-bahth-ilmi-badawi"]
    ))

    puzzles.append(create_puzzle(
        "logic-ord-2", "order", "order", "logic", 2,
        "دورة الماء في الطبيعة",
        "الترتيب الطبيعي: التبخر بفعل الشمس، التكاثف في السماء، هطول الأمطار، الجريان للأنهار والبحار.",
        [{"id": "h1", "kind": "step", "text": "تبدأ الدورة بتبخر مياه المسطحات.", "penalty": 10}],
        {
            "items": [
                {"id": "i1", "text": "تبخر الماء بالحرارة"},
                {"id": "i2", "text": "تكاثف البخار سحاباً"},
                {"id": "i3", "text": "هطول الأمطار"},
                {"id": "i4", "text": "جريان الماء إلى البحر"}
            ],
            "correctOrder": ["i1", "i2", "i3", "i4"],
            "variant": "chronology"
        },
        ["dawaer-tabia-ulum"]
    ))

    puzzles.append(create_puzzle(
        "logic-ord-3", "order", "order", "logic", 3,
        "أركان القياس المنطقي",
        "ترتيب أركان القياس: المقدمة الكبرى، المقدمة الصغرى، الحد الأوسط المشترك، النتيجة الحتمية.",
        [{"id": "h1", "kind": "step", "text": "المقدمة الكبرى ثم الصغرى ثم النتيجة.", "penalty": 15}],
        {
            "items": [
                {"id": "i1", "text": "المقدمة الكبرى"},
                {"id": "i2", "text": "المقدمة الصغرى"},
                {"id": "i3", "text": "استخلاص النتيجة"}
            ],
            "correctOrder": ["i1", "i2", "i3"],
            "variant": "procedure"
        },
        ["usul-mantiq-muzaffar"]
    ))

    return puzzles

def main():
    puzzles = build_all_puzzles()
    print(f"Generated {len(puzzles)} puzzles.")
    by_world = {}
    by_type = {}
    for p in puzzles:
        w = p['world']
        t = p['type']
        by_world[w] = by_world.get(w, 0) + 1
        by_type[t] = by_type.get(t, 0) + 1

    print("Distribution by World:")
    for w, count in sorted(by_world.items()):
        print(f"  {w}: {count}")

    print("\nDistribution by Type:")
    for t, count in sorted(by_type.items()):
        print(f"  {t}: {count}")

    assert len(puzzles) == 120, f"Expected 120 puzzles, got {len(puzzles)}"
    for w in ['words', 'imagination', 'poetry', 'rhetoric', 'quran', 'logic']:
        assert by_world.get(w, 0) == 20, f"World {w} has {by_world.get(w)} puzzles, expected 20"

    # Write output to packages/knowledge/data/opening_pack_120.jsonl
    out_dir = ROOT / 'packages/knowledge/data'
    out_dir.mkdir(parents=True, exist_ok=True)
    out_file = out_dir / 'opening_pack_120.jsonl'
    with open(out_file, 'w', encoding='utf-8') as f:
        for p in puzzles:
            f.write(json.dumps(p, ensure_ascii=False) + '\n')

    # Also write to packs/opening-pack-120/data/puzzles.jsonl
    pack_dir = ROOT / 'packs/opening-pack-120/data'
    pack_dir.mkdir(parents=True, exist_ok=True)
    pack_puzzles = pack_dir / 'puzzles.jsonl'
    with open(pack_puzzles, 'w', encoding='utf-8') as f:
        for p in puzzles:
            f.write(json.dumps(p, ensure_ascii=False) + '\n')

    data_bytes = pack_puzzles.read_bytes()
    puzzles_sha256 = hashlib.sha256(data_bytes).hexdigest()
    puzzles_size = len(data_bytes)

    # Create manifest for opening-pack-120
    manifest = {
        "schemaVersion": 1,
        "packId": make_uuid("pack:opening-pack-120"),
        "version": 1,
        "minAppVersionCode": 1,
        "minEngineVersion": 1,
        "locale": "ar",
        "publishedAt": "2026-11-01T12:00:00Z",
        "files": [
            {
                "path": "data/puzzles.jsonl",
                "kind": "puzzles",
                "sha256": puzzles_sha256,
                "bytes": puzzles_size,
                "chunks": [
                    {
                        "sha256": puzzles_sha256,
                        "bytes": puzzles_size
                    }
                ]
            }
        ],
        "downloadBytes": puzzles_size,
        "installedBytes": puzzles_size,
        "dependencies": [],
        "sourceRefs": list({ref for p in puzzles for ref in p["sourceRefs"]}),
        "assets": [],
        "references": [
            {
                "referenceId": "ref-lughzdan-reviewed-corpus",
                "sourceId": "src-lughzdan-core",
                "citation": "حزمة لُغزدان الافتتاحية المراجعة 120 لغزاً",
                "licenseCode": "CC-BY-SA-4.0",
                "noticePath": "data/puzzles.jsonl",
                "attribution": "فريق لُغزدان للمحتوى واللغة والمصادر المعتمدة"
            }
        ]
    }

    manifest_file = ROOT / 'packs/opening-pack-120/manifest.json'
    manifest_file.write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')

    print(f"\nSuccessfully wrote {out_file} and {pack_puzzles} ({puzzles_size} bytes, sha256={puzzles_sha256})")
    print(f"Manifest written to {manifest_file}")

if __name__ == '__main__':
    main()
