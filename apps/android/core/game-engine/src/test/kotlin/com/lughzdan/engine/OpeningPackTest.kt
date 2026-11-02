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
            val session = plugin.createSession(p, "release-${p.puzzleId}")
            assertTrue(p.puzzleId, plugin.validateSession(p, session).valid)
        }
    }
}
