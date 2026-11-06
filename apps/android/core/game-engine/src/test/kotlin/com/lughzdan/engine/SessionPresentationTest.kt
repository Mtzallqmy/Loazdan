package com.lughzdan.engine
import org.junit.Assert.*
import org.junit.Test
class SessionPresentationTest {
 private val options=(0..3).map {Option("$it","answer $it")}
 @Test fun choiceOrderSurvivesEncodingAndVariesAcrossSessions() {
  val positions=(0..99).map {n->
   val s=GameSession("session-$n","p",1,displaySeed=n)
   val order=SessionPresentation.shuffled(options,s,"options")
   assertEquals(order,SessionPresentation.shuffled(options,SessionCodec.restore(SessionCodec.encode(s)),"options"))
   assertEquals(options.toSet(),order.toSet())
   order.indexOf(options.first())
  }.toSet()
  assertEquals(setOf(0,1,2,3),positions)
 }
 @Test fun orderingNeverStartsSolvedEvenWithTwoItems() {
  for(size in 2..20) for(seed in 0..100) {
   val ids=(0 until size).toList()
   val order=SessionPresentation.unsolved(ids,ids,seed)
   assertNotEquals(ids,order);assertEquals(ids.toSet(),order.toSet())
  }
 }
 @Test fun legacySessionGetsStablePresentationWithoutRewritingProgress() {
  val s=GameSession("legacy","p",1,selectedId="2")
  assertEquals(SessionPresentation.shuffled(options,s,"options"),SessionPresentation.shuffled(options,SessionCodec.decode(SessionCodec.encode(s)),"options"))
  assertEquals("2",s.selectedId)
 }
}
