package com.lughzdan.engine

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class OpeningPackTest {
    @Test fun releasePackIsAcceptedByEveryRuntimePlugin() {
        val root = generateSequence(File(".").canonicalFile) { it.parentFile }
            .first { File(it, "packs/opening-pack-120/data/puzzles.jsonl").exists() }
        val registry = GameRegistry()
        val puzzles = File(root, "packs/opening-pack-120/data/puzzles.jsonl").readLines()
            .filter { it.isNotBlank() }.map { Json.decodeFromString<PuzzleDefinition>(it) }
        val errors = puzzles.flatMap { p ->
            registry.plugin(p.type).validatePuzzle(p).errors.map { "${p.puzzleId} ${p.type}: $it" }
        }
        assertEquals(120, puzzles.size)
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
        puzzles.forEach { p ->
            val plugin = registry.plugin(p.type)
            var session = plugin.createSession(p, "release-${p.puzzleId}")
            assertTrue(p.puzzleId, plugin.validateSession(p, session).valid)
            var action = 0
            fun id() = "action-${action++}"
            fun send(a: PlayerAction) { session = plugin.evaluate(p, session, a) }
            when (p.type) {
                "choice" -> send(PlayerAction.Select(id(), p.payload.correctId))
                "crossword" -> p.payload.entries.forEach { e -> entryCells(e).zip(e.tokens).forEach { (c,t) -> send(PlayerAction.InputCell(id(),c.row,c.col,t)) } }
                "wordsearch" -> p.payload.words.forEach { w -> send(PlayerAction.SelectPath(id(),w.path));send(PlayerAction.Submit(id())) }
                "anagram", "four_pictures" -> {
                    val used = mutableSetOf<Int>()
                    tokens(p.payload.acceptedAnswers.first()).forEach { token ->
                        val i = p.payload.tiles.indices.first { it !in used && p.payload.tiles[it] == token }
                        used.add(i);send(PlayerAction.AddTile(id(),i))
                    }
                }
                "matching" -> if(p.payload.options.isNotEmpty()) send(PlayerAction.Select(id(),p.payload.correctId)) else p.payload.pairs.forEach { send(PlayerAction.Match(id(),it.leftId,it.rightId)) }
                "order" -> send(PlayerAction.Reorder(id(),p.payload.correctOrder))
                "text_answer", "guess_picture", "symbols" -> send(PlayerAction.InputAnswer(id(),p.payload.acceptedAnswers.first()))
                "sequence" -> send(PlayerAction.InputAnswer(id(),p.payload.answer.toString()))
                "shadow", "visual_logic" -> send(PlayerAction.SelectImage(id(),p.payload.correctImageId))
                "visual_association" -> p.payload.visualPairs.forEach { send(PlayerAction.MatchImages(id(),it.leftId,it.rightId)) }
                else -> error("Missing release game: ${p.type}")
            }
            if(session.status == SessionStatus.ACTIVE) send(PlayerAction.Submit(id()))
            assertEquals(p.puzzleId,SessionStatus.WON,session.status)
            assertTrue("${p.puzzleId}: ${plugin.validateSession(p,session).errors}",plugin.validateSession(p,session).valid)
            assertEquals(session,SessionCodec.restore(SessionCodec.encode(session)))
        }
    }
}
