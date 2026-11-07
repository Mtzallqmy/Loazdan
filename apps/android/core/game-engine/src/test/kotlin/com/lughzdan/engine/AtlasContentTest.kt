package com.lughzdan.engine

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AtlasContentTest {
    private fun puzzles(): List<PuzzleDefinition> {
        val root = generateSequence(File(".").canonicalFile) { it.parentFile }.first { File(it, "packs/atlas-600/new-puzzles.jsonl").exists() }
        return File(root, "packs/atlas-600/new-puzzles.jsonl").readLines().filter { it.isNotBlank() }.map { Json.decodeFromString<PuzzleDefinition>(it) }
    }

    @Test fun everyChallengeSolvesAndEveryJourneyHasFiveDistinctConcepts() {
        val all = puzzles()
        assertEquals(432, all.size)
        assertEquals(432, all.map { it.puzzleId }.distinct().size)
        val registry = GameRegistry()
        for (p in all) {
            val plugin = registry.plugin(p.type)
            assertEquals(p.puzzleId, emptyList<String>(), plugin.validatePuzzle(p).errors)
            var s = plugin.createSession(p, p.puzzleId)
            assertTrue(p.puzzleId, plugin.validateSession(p, s).valid)
            var index = 0
            fun send(action: PlayerAction) { s = plugin.evaluate(p, s, action) }
            fun id() = "a-${index++}"
            when (p.type) {
                "choice" -> send(PlayerAction.Select(id(), p.payload.correctId))
                "anagram" -> {
                    val used = mutableSetOf<Int>()
                    p.payload.acceptedAnswers.first().forEach { token ->
                        val tile = p.payload.tiles.indices.first { it !in used && p.payload.tiles[it] == token.toString() }
                        used.add(tile)
                        send(PlayerAction.AddTile(id(), tile))
                    }
                }
                "text_answer" -> if (WordBridge.supports(p)) {
                    val path = WordBridge.route(p, WordBridge.start(p))!!
                    for (word in path.drop(1)) {
                        send(PlayerAction.InputAnswer(id(), word))
                        send(PlayerAction.Submit(id()))
                        assertTrue(p.puzzleId, plugin.validateSession(p, s).valid)
                    }
                } else send(PlayerAction.InputAnswer(id(), p.payload.acceptedAnswers.first()))
                "sequence" -> send(PlayerAction.InputAnswer(id(), p.payload.answer.toString()))
                "order" -> send(PlayerAction.Reorder(id(), p.payload.correctOrder))
            }
            send(PlayerAction.Submit(id()))
            assertEquals(p.puzzleId, SessionStatus.WON, s.status)
            assertTrue(p.puzzleId, plugin.validateSession(p, s).valid)
            assertEquals(s, plugin.evaluate(p, s, PlayerAction.Submit(id())))
        }
        val journeys = AtlasCatalog.journeys(all)
        assertEquals(30, journeys.size)
        for (journey in journeys) {
            assertEquals(5, journey.puzzles.size)
            assertEquals(5, journey.puzzles.map { it.tags.first { tag -> tag.startsWith("concept:") } }.distinct().size)
            assertTrue(journey.puzzles.map { it.type }.distinct().size >= 2)
        }
        for (world in AtlasCatalog.worlds) {
            val duel = AtlasCatalog.duelPool(all, world.id, 5).map { id -> all.first { it.puzzleId == id } }
            assertTrue(duel.size >= 2)
            duel.chunked(2).forEach { pair -> assertEquals(pair[0].type, pair[1].type); assertEquals(pair[0].payload.variant,pair[1].payload.variant); assertEquals(pair[0].difficulty, pair[1].difficulty) }
        }
    }

    @Test fun bridgeRejectsTwoLetterChangesAndRestoresItsPath() {
        val p = puzzles().first { WordBridge.supports(it) }
        val plugin = TextAnswerPlugin()
        var s = plugin.createSession(p, "bridge")
        s = plugin.evaluate(p, s, PlayerAction.InputAnswer("invalid", "سسس"))
        s = plugin.evaluate(p, s, PlayerAction.Submit("submit"))
        assertEquals(1, s.wrongSubmissions)
        assertEquals(listOf(WordBridge.start(p)), s.answerHistory)
        val next = WordBridge.route(p, WordBridge.start(p))!![1]
        s = plugin.evaluate(p, s, PlayerAction.InputAnswer("valid", next))
        s = plugin.evaluate(p, s, PlayerAction.Submit("step"))
        val restored = SessionCodec.restore(SessionCodec.encode(s))
        assertEquals(listOf(WordBridge.start(p), next), restored.answerHistory)
        assertTrue(plugin.validateSession(p, restored).valid)
    }
}
