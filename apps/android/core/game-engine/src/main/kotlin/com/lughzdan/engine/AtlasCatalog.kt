package com.lughzdan.engine

data class AtlasWorld(val id: String, val title: String, val description: String, val topics: List<String>)
data class AtlasJourney(val id: String, val title: String, val world: AtlasWorld, val puzzles: List<PuzzleDefinition>)

object AtlasCatalog {
    val worlds = listOf(
        AtlasWorld("words", "مدينة اللغة", "معانٍ ومفردات واكتشاف الصياغة الصحيحة", listOf("المفردات", "الأضداد", "دقة التعبير", "المحقق اللغوي")),
        AtlasWorld("poetry", "ديوان الأدب", "فنون السرد والشعر وصناعة النص", listOf("فنون الكتابة", "داخل الحكاية", "لغة الشعر", "قراءة النص")),
        AtlasWorld("rhetoric", "مملكة البيان", "تأمل الصورة والمعنى وجمال التعبير", listOf("التشبيه والصورة", "المحسنات", "الأساليب", "دلالات التعبير")),
        AtlasWorld("heritage", "دروب اليمن", "مدن وآثار وطبيعة وتراث عربي", listOf("مدن ومعالم", "طبيعة ومكان", "تراث وحرف", "لغة المكان")),
        AtlasWorld("science", "مختبر الحياة", "اكتشف مفاهيم الطبيعة والعلم حولك", listOf("الماء والهواء", "الأرض والفضاء", "الحياة", "الطاقة والمادة")),
        AtlasWorld("logic", "مرصد التفكير", "أنماط وعلاقات وحلول خطوة بخطوة", listOf("أنماط", "علاقات", "ترتيب", "استنتاج")),
    )

    fun world(p: PuzzleDefinition): String = p.tags.firstOrNull { it.startsWith("atlas:") }?.substringAfter(':') ?: when (p.world) {
        "quran" -> "words"
        "imagination" -> "logic"
        else -> p.world
    }

    fun name(p: PuzzleDefinition): String = when (p.payload.variant) {
        "language_detective" -> "المحقق اللغوي"
        WordBridge.VARIANT -> "جسر الكلمات"
        "who_am_i" -> "من أنا؟"
        LiteraryWords.COMPLETE -> "أكمل الحروف"
        LiteraryWords.RIDDLE -> "خمن الكلمة"
        else -> when(p.type) { "choice" -> "اختر الإجابة"; "anagram" -> "ابنِ الكلمة"; "order" -> "رتّب الفكرة"; "sequence" -> "أكمل النمط"; else -> "تحدي المعرفة" }
    }

    fun journeys(all: List<PuzzleDefinition>): List<AtlasJourney> = worlds.flatMap { world ->
        (0..4).mapNotNull { chapter ->
            val candidates = all.filter { "atlas:${world.id}" in it.tags && (chapter == 4 || "topic:$chapter" in it.tags) }
            val concepts = candidates.groupBy { it.tags.firstOrNull { tag -> tag.startsWith("concept:") } ?: it.puzzleId }.toSortedMap()
            var previous = ""
            val stages = concepts.values.take(5).map { options ->
                val selected = options.sortedBy { it.puzzleId }.firstOrNull { it.type != previous } ?: options.first()
                previous = selected.type
                selected
            }
            if (stages.size != 5) null else AtlasJourney("${world.id}-$chapter", if(chapter==4) "حصاد ${world.title}" else world.topics[chapter], world, stages)
        }
    }

    /** Two distinct questions per game and difficulty give both players comparable turns. */
    fun duelPool(all: List<PuzzleDefinition>, world: String, maxDifficulty: Int): List<String> {
        val groups = all.filter { AtlasCatalog.world(it) == world && it.difficulty <= maxDifficulty && it.type in setOf("choice", "text_answer", "sequence") }
            .groupBy { it.type to it.difficulty }.values
        return groups.filter { it.size >= 2 }.sortedBy { it.first().difficulty }.take(3).flatMap { group -> group.shuffled().take(2).map { it.puzzleId } }
    }
}
