package app.lughzdan.engine

// Stage 01 boundary contract, Kotlin/JVM standard library only.
// DTO serialization, Room implementations and Compose renderers belong outside this file.
data class PuzzleKey(val id: String, val revision: Int)
data class PackKey(val id: String, val version: Int)
data class Cell(val row: Int, val col: Int)
data class Hint(val id: String, val penalty: Int, val text: String)
data class Difficulty(val level: Int) {
    init { require(level in 1..5) }
    val baseScore: Int get() = 50 + 50 * level
}
enum class Phase { CREATED, ACTIVE, PAUSED, WON, LOST, ABANDONED }
enum class NormalizationPolicy { EXACT_V1, ARABIC_BASIC_V1 }
interface PuzzleDefinition {
    val key: PuzzleKey
    val type: String
    val schemaVersion: Int
    val rulesVersion: Int
    val difficulty: Difficulty
    val hints: List<Hint>
}
interface GameSessionState {
    val sessionId: String
    val puzzle: PuzzleKey
    val pack: PackKey
    val stateVersion: Int
    val phase: Phase
    val wrongSubmissions: Int
    val usedHintIds: Set<String>
    val appliedActionIds: Set<String>
}
sealed interface GameAction {
    val actionId: String
    data class EnterCell(override val actionId: String, val cell: Cell, val token: String) : GameAction
    data class EnterText(override val actionId: String, val value: String) : GameAction
    data class MoveTile(override val actionId: String, val tileId: String, val targetIndex: Int) : GameAction
    data class SelectPath(override val actionId: String, val cells: List<Cell>) : GameAction
    data class SelectChoice(override val actionId: String, val choiceId: String) : GameAction
    data class MatchPair(override val actionId: String, val leftId: String, val rightId: String) : GameAction
    data class Reveal(override val actionId: String, val step: Int) : GameAction
    data class RequestHint(override val actionId: String, val hintId: String) : GameAction
    data class Submit(override val actionId: String) : GameAction
    data class Pause(override val actionId: String) : GameAction
    data class Resume(override val actionId: String) : GameAction
    data class Abandon(override val actionId: String) : GameAction
}
sealed interface Effect {
    data class Feedback(val correct: Boolean) : Effect
    data class Completed(val score: Int) : Effect
}
data class Transition<S>(val state: S, val effects: List<Effect>)
data class ValidationIssue(val path: String, val code: String)
data class Evaluation(val correct: Boolean, val remainingUnits: Int)
data class SessionSeed(val sessionId: String, val randomSeed: Long, val pack: PackKey)

interface PuzzleValidator<D : PuzzleDefinition> { fun validate(definition: D): List<ValidationIssue> }
interface AnswerEvaluator<D : PuzzleDefinition, S : GameSessionState> { fun evaluate(d: D, s: S): Evaluation }
interface HintEngine<D : PuzzleDefinition, S : GameSessionState> { fun available(d: D, s: S): List<Hint> }
interface StateCodec<S : GameSessionState> {
    val version: Int
    fun encode(state: S): ByteArray
    fun decode(bytes: ByteArray, storedVersion: Int): S // throws typed UnsupportedStateVersion externally
}
interface GamePlugin<D : PuzzleDefinition, S : GameSessionState> {
    val type: String
    val schemaVersion: Int
    val validator: PuzzleValidator<D>
    val evaluator: AnswerEvaluator<D, S>
    val hints: HintEngine<D, S>
    val codec: StateCodec<S>
    fun initial(definition: D, seed: SessionSeed): S
    fun reduce(definition: D, state: S, action: GameAction): Transition<S>
}
class GameTypeRegistry(plugins: List<GamePlugin<*, *>>) {
    private val entries = plugins.associateBy { it.type to it.schemaVersion }
    init { require(entries.size == plugins.size) { "Duplicate type/version" } }
    fun find(type: String, version: Int): GamePlugin<*, *>? = entries[type to version]
    // A typed DTO adapter at registration performs checked runtime type matching,
    // never blind casting an arbitrary network definition to a plugin's generic D.
}
object ScoringEngine {
    fun score(difficulty: Difficulty, won: Boolean, wrong: Int, used: Set<String>, hints: List<Hint>): Int {
        require(wrong >= 0)
        require(hints.map { it.id }.distinct().size == hints.size)
        require(hints.all { it.penalty in 0..100 })
        require(used.all { id -> hints.any { it.id == id } })
        return if (!won) 0 else (difficulty.baseScore - 10 * wrong - hints.filter { it.id in used }.sumOf { it.penalty }).coerceAtLeast(0)
    }
}
object ArabicAnswerNormalizer {
    fun normalize(raw: String, policy: NormalizationPolicy): String {
        val nfc = java.text.Normalizer.normalize(raw, java.text.Normalizer.Form.NFC).trim()
        if (policy == NormalizationPolicy.EXACT_V1) return nfc
        return nfc.replace(Regex("[\u0640\u064B-\u065F\u0670]"), "").replace(Regex("\\s+"), " ")
    }
}
data class Completion(val sessionId: String, val puzzle: PuzzleKey, val won: Boolean, val score: Int)
data class SyncOperation(val operationId: String, val deviceId: String, val sequence: Int, val canonicalJson: String)
interface ProgressPersistence {
    // Implementation commits state, projection and outbox atomically in Room.
    suspend fun save(state: GameSessionState, completion: Completion?, operation: SyncOperation?)
    suspend fun restore(sessionId: String): GameSessionState?
}
interface AchievementEngine {
    fun evaluate(completedPuzzleIds: Set<String>, awardedCodes: Set<String>): Set<String>
}
class BasicAchievements : AchievementEngine {
    override fun evaluate(completedPuzzleIds: Set<String>, awardedCodes: Set<String>): Set<String> = buildSet {
        if (completedPuzzleIds.isNotEmpty()) add("first_win")
        if (completedPuzzleIds.size >= 10) add("ten_unique_puzzles")
    } - awardedCodes
}
