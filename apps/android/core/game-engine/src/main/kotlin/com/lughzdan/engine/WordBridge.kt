package com.lughzdan.engine

object WordBridge {
    const val VARIANT = "word_bridge"
    fun supports(p: PuzzleDefinition) = p.type == "text_answer" && p.payload.variant == VARIANT
    fun adjacent(a: String, b: String) = a.length == b.length && a.indices.count { a[it] != b[it] } == 1
    fun start(p: PuzzleDefinition) = p.payload.items.firstOrNull()?.text.orEmpty()
    fun goal(p: PuzzleDefinition) = p.payload.acceptedAnswers.firstOrNull().orEmpty()

    fun route(p: PuzzleDefinition, from: String, blocked: Set<String> = emptySet()): List<String>? {
        val pending = ArrayDeque<List<String>>()
        pending.add(listOf(from))
        val seen = blocked.toMutableSet().also { it.add(from) }
        while (pending.isNotEmpty()) {
            val path = pending.removeFirst()
            if (path.last() == goal(p)) return path
            for (word in p.payload.items.map { it.text }) {
                if (word !in seen && adjacent(path.last(), word)) {
                    seen.add(word)
                    pending.add(path + word)
                }
            }
        }
        return null
    }

    fun errors(p: PuzzleDefinition): List<String> = buildList {
        if (!supports(p)) return@buildList
        val words = p.payload.items.map { it.text }
        if (words.size !in 3..64 || words.distinct().size != words.size || words.any { it.length != start(p).length || it.any { c -> !validToken(c.toString()) } }) add("invalid_bridge_dictionary")
        if (start(p) == goal(p) || route(p, start(p)) == null) add("unreachable_bridge")
    }

    fun submit(p: PuzzleDefinition, s: GameSession): GameSession {
        val word = normalized(p, s.answerInput)
        if (word.isBlank()) return s
        if (word in s.answerHistory || p.payload.items.none { it.text == word } || !adjacent(s.answerHistory.last(), word)) {
            val wrong = s.wrongSubmissions + 1
            return s.copy(wrongSubmissions = wrong, status = if (wrong == 3) SessionStatus.LOST else SessionStatus.ACTIVE)
        }
        val next = s.copy(answerHistory = s.answerHistory + word, answerInput = "")
        return if (word == goal(p)) next.copy(answerInput = word, status = SessionStatus.WON, score = ScoringEngine.score(p, s.wrongSubmissions, s.usedHintIds)) else next
    }

    fun validPath(p: PuzzleDefinition, s: GameSession): Boolean {
        val path = s.answerHistory
        return path.firstOrNull() == start(p) && path.distinct().size == path.size && path.all { word -> p.payload.items.any { it.text == word } } && path.zipWithNext().all { (a, b) -> adjacent(a, b) }
    }
}
