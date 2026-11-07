package com.lughzdan.engine

import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class AtlasCorrectionsTest {
    @Test fun correctedBoardsSolveAndArchiveOnlyTheirOriginalCatalogEntries() {
        val root=generateSequence(File(".").canonicalFile){it.parentFile}.first {File(it,"packs/atlas-600/corrected-puzzles.jsonl").exists()}
        fun load(path:String)=File(root,path).readLines().filter {it.isNotBlank()}.map {Json.decodeFromString<PuzzleDefinition>(it)}
        val corrected=load("packs/atlas-600/corrected-puzzles.jsonl")
        val all=load("packs/opening-pack-120/data/puzzles.jsonl")+load("packs/atlas-600/puzzles.jsonl")
        assertEquals(14,corrected.size)
        val active=AtlasCatalog.active(all)
        assertEquals(600,active.size)
        assertTrue(corrected.all {it in active})
        assertTrue(corrected.all {p->active.none {it.puzzleId==p.tags.single().substringAfter(':')}})
        for(p in corrected) {
            val engine=GameRegistry().plugin(p.type)
            assertEquals(p.title,emptyList<String>(),engine.validatePuzzle(p).errors)
            var s=engine.createSession(p,p.puzzleId)
            var i=0
            fun id()="action-${i++}"
            fun send(a:PlayerAction){s=engine.evaluate(p,s,a)}
            when(p.type) {
                "crossword"->p.payload.entries.forEach {e->entryCells(e).zip(e.tokens).forEach {(c,t)->send(PlayerAction.InputCell(id(),c.row,c.col,t))}}
                "wordsearch"->p.payload.words.forEach {w->send(PlayerAction.SelectPath(id(),w.path));send(PlayerAction.Submit(id()))}
                "anagram"->{val used=mutableSetOf<Int>();p.payload.acceptedAnswers.first().forEach {token->val index=p.payload.tiles.indices.first {it !in used&&p.payload.tiles[it]==token.toString()};used.add(index);send(PlayerAction.AddTile(id(),index))}}
                "text_answer"->send(PlayerAction.InputAnswer(id(),p.payload.acceptedAnswers.first()))
                "choice"->send(PlayerAction.Select(id(),p.payload.correctId))
            }
            send(PlayerAction.Submit(id()))
            assertEquals(p.title,SessionStatus.WON,s.status)
            assertTrue(p.title,engine.validateSession(p,s).valid)
        }
    }
}
