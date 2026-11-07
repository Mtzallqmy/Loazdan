package com.lughzdan.engine

/** Two presentation modes share durable text-answer sessions and normal scoring. */
object LiteraryWords {
 const val COMPLETE="literary_complete"
 const val RIDDLE="literary_riddle"
 val categories=linkedMapOf("language" to "مفردات ومعانٍ","literature" to "فنون الأدب","rhetoric" to "بلاغة وبيان","classical" to "شعر تراثي","modern" to "شعر حديث","riddles" to "ألغاز وصفية","heritage" to "اليمن والتراث","science" to "العلوم والحياة","logic" to "المنطق")
 fun supports(p:PuzzleDefinition)=p.type=="text_answer" && p.payload.variant in setOf(COMPLETE,RIDDLE)
 fun answer(p:PuzzleDefinition)=normalized(p,p.payload.acceptedAnswers.firstOrNull() ?: "")
 fun revealed(p:PuzzleDefinition,s:GameSession):Map<Int,String> {
  val result=p.payload.fixedSlots.associate {it.index to it.token}.toMutableMap()
  s.usedHintIds.forEach {id->id.removePrefix("letter-").toIntOrNull()?.takeIf {id.startsWith("letter-") && it in answer(p).indices}?.let {result[it]=answer(p)[it].toString()}}
  return result
 }
 fun input(p:PuzzleDefinition,s:GameSession,value:String):String {
  if(p.payload.variant!=COMPLETE)return value
  val clean=normalized(p,value.replace(' ','□')).replace('□',' ').take(answer(p).length).padEnd(answer(p).length,' ').toCharArray()
  revealed(p,s).forEach {(i,t)->clean[i]=t.single()}
  return clean.concatToString()
 }
 fun complete(p:PuzzleDefinition,s:GameSession)=if(p.payload.variant==COMPLETE) s.answerInput.length==answer(p).length && s.answerInput.none {it.isWhitespace()} else s.answerInput.isNotBlank()
 fun errors(p:PuzzleDefinition):List<String> = buildList {
  if(!supports(p))return@buildList
  if(p.payload.mode !in categories || p.payload.sourceLabel.isBlank())add("missing_literary_metadata")
  if(p.payload.sourceUrl.isNotEmpty() && !p.payload.sourceUrl.startsWith("https://"))add("unsafe_source_url")
  if(p.payload.acceptedAnswers.any {normalized(p,it).length !in 2..12 || normalized(p,it).any {c->!validToken(c.toString())}})add("single_arabic_word_required")
  val answer=answer(p)
  if(p.payload.fixedSlots.map {it.index}.distinct().size!=p.payload.fixedSlots.size || p.payload.fixedSlots.any {it.index !in answer.indices || it.token!=answer.getOrNull(it.index)?.toString()})add("invalid_literary_fixed_slots")
  if(p.payload.fixedSlots.size>=answer.length)add("already_solved_word")
  if(p.payload.variant==COMPLETE && p.payload.acceptedAnswers.size!=1)add("completion_requires_one_target")
  if(p.hints.any {it.id.startsWith("letter-") && (it.id.removePrefix("letter-").toIntOrNull()?.let {i->i !in answer.indices} ?: true)})add("invalid_letter_hint")
 }
}
