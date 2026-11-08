package com.lughzdan.engine

import kotlin.random.Random

/** Seed is saved with new sessions. Legacy sessions use their immutable ID, never composition time. */
object SessionPresentation {
 fun <T> shuffled(items:List<T>,session:GameSession,lane:String):List<T> =
  items.shuffled(Random((session.displaySeed ?: session.id.hashCode()) xor lane.hashCode()))
 fun <T> unsolved(items:List<T>,solution:List<T>,seed:Int):List<T> {
  val shuffled=items.shuffled(Random(seed))
  return if(shuffled.size>1 && shuffled==solution) shuffled.drop(1)+shuffled.first() else shuffled
 }
 fun matchingRight(p:PuzzleDefinition,s:GameSession):List<Option> {
  val left=shuffled(p.payload.left,s,"left")
  val right=shuffled(p.payload.right,s,"right")
  val solution=left.map {l->p.payload.pairs.first {it.leftId==l.id}.rightId}
  return if(right.size>1 && right.map {it.id}==solution) right.drop(1)+right.first() else right
 }
}
