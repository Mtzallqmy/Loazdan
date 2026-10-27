package com.lughzdan.engine
import org.junit.Assert.*
import org.junit.Test
class EngineTest {
    private val p = PuzzleDefinition(1,"p",1,"g","choice","words",2,1,"arabic_basic_v1","t","draft",true,listOf("fixture"),"explain", listOf(Hint("h","step","hint",20)),ChoicePayload("q",listOf(Option("a","a"),Option("b","b"),Option("c","c"),Option("d","d")),"a"))
    private val e = ChoicePlugin()
    @Test fun correctAnswerScores() { val s=e.evaluate(p,e.createSession(p,"s"),PlayerAction.Select("1","a")); assertEquals(150,e.evaluate(p,s,PlayerAction.Submit("2")).score) }
    @Test fun threeWrongSubmissionsLose() { var s=e.createSession(p,"s"); repeat(3) { s=e.evaluate(p,s,PlayerAction.Select("s$it","b"));s=e.evaluate(p,s,PlayerAction.Submit("a$it")) }; assertEquals(SessionStatus.LOST,s.status); assertEquals(0,s.score) }
    @Test fun duplicateActionNoop() { val s=e.evaluate(p,e.createSession(p,"s"),PlayerAction.Select("1","b"));val n=e.evaluate(p,s,PlayerAction.Submit("2"));assertEquals(n,e.evaluate(p,n,PlayerAction.Submit("2"))) }
    @Test fun hintChargedOnce() { var s=e.createSession(p,"s");repeat(2){ s=e.evaluate(p,s,PlayerAction.UseHint("h$it","h")) };s=e.evaluate(p,s,PlayerAction.Select("3","a"));assertEquals(130,e.evaluate(p,s,PlayerAction.Submit("4")).score) }
    @Test fun terminalImmutable() { var s=e.evaluate(p,e.createSession(p,"s"),PlayerAction.Select("1","a"));s=e.evaluate(p,s,PlayerAction.Submit("2"));assertEquals(s,e.evaluate(p,s,PlayerAction.UseHint("3","h"))) }
    @Test fun emptySubmitNoPenalty() { val s=e.createSession(p,"s");assertEquals(s,e.evaluate(p,s,PlayerAction.Submit("1"))) }
    @Test fun restorePausesAndPins() { val s=e.createSession(p,"s");val restored=SessionCodec.restore(SessionCodec.encode(s));assertEquals(SessionStatus.PAUSED,restored.status);assertEquals(s,e.evaluate(p,restored,PlayerAction.Resume("resume")).copy(processedActions=emptySet())) }
    @Test fun invalidOptionsRejected() { assertFalse(e.validatePuzzle(p.copy(payload=p.payload.copy(options=p.payload.options.take(1)))).valid) }
    @Test fun contractAllowsTwoToSixOptions() { assertTrue(e.validatePuzzle(p.copy(payload=p.payload.copy(options=p.payload.options.take(2)))).valid) }
    @Test fun pausedSessionRejectsPlayUntilResume() { val s=e.createSession(p,"s").copy(status=SessionStatus.PAUSED); assertEquals(s,e.evaluate(p,s,PlayerAction.Select("select","a"))) }
    @Test fun excessiveHintPenaltyRejected() { assertFalse(e.validatePuzzle(p.copy(hints=listOf(Hint("h","step","hint",101)))).valid) }
    @Test fun normalizationPreservesDistinctLetters() { val policy=NormalizationPolicy(removeMarks=true);assertEquals("علم",ArabicText.comparison("عِلْم",policy));assertNotEquals(ArabicText.comparison("رحمة",policy),ArabicText.comparison("رحمه",policy));assertNotEquals(ArabicText.comparison("على",policy),ArabicText.comparison("علي",policy));assertNotEquals(ArabicText.comparison("أمل",policy),ArabicText.comparison("امل",policy)) }
    @Test fun optionalFoldsAndOriginalUnchanged() { val original="إِلَى";assertEquals("الي",ArabicText.comparison(original,NormalizationPolicy(true,true,true)));assertEquals("إِلَى",original);assertEquals("سؤال",ArabicText.comparison("سُؤَال",NormalizationPolicy(true))) }
    @Test(expected=IllegalArgumentException::class) fun futureStateRejected() { SessionCodec.decode(SessionCodec.encode(e.createSession(p,"s").copy(stateVersion=2))) }
    @Test(expected=IllegalArgumentException::class) fun bidiRejected() { ArabicText.comparison("علم\u202E") }
}
