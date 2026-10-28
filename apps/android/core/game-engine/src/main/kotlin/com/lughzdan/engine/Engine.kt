package com.lughzdan.engine

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.Normalizer

@Serializable data class Option(val id: String, val text: String)
@Serializable data class Hint(val id: String, val kind: String, val text: String, val penalty: Int)
@Serializable data class Cell(val row: Int, val col: Int)
@Serializable data class CrosswordEntry(val id: String, val row: Int, val col: Int, val direction: String, val tokens: List<String>, val clue: String, val sourceRef: String)
@Serializable data class SearchWord(val id: String, val label: String, val path: List<Cell>)
@Serializable data class FixedSlot(val index: Int, val token: String)
@Serializable data class MatchPair(val leftId: String, val rightId: String)
@Serializable data class ChoicePayload(
 val prompt: String = "", val options: List<Option> = emptyList(), val correctId: String = "",
 val rows: Int = 0, val cols: Int = 0, val entries: List<CrosswordEntry> = emptyList(),
 val grid: List<List<String>> = emptyList(), val words: List<SearchWord> = emptyList(),
 val tiles: List<String> = emptyList(), val fixedSlots: List<FixedSlot> = emptyList(), val acceptedAnswers: List<String> = emptyList(),
 val left: List<Option> = emptyList(), val right: List<Option> = emptyList(), val pairs: List<MatchPair> = emptyList(),
 val items: List<Option> = emptyList(), val correctOrder: List<String> = emptyList(), val variant: String = "",
)
@Serializable data class PuzzleDefinition(
    val schemaVersion: Int, val puzzleId: String, val revision: Int, val gameId: String,
    val type: String, val world: String, val difficulty: Int, val rulesVersion: Int,
    val normalization: String, val title: String, val status: String, val fixtureOnly: Boolean,
    val sourceRefs: List<String>, val explanation: String, val hints: List<Hint>, val payload: ChoicePayload,
)
@Serializable enum class SessionStatus { ACTIVE, PAUSED, WON, LOST }
@Serializable data class GameSession(
    val id: String, val puzzleId: String, val revision: Int, val packVersion: Int = 1,
    val packId: String = "", val stateVersion: Int = 1, val status: SessionStatus = SessionStatus.ACTIVE,
    val wrongSubmissions: Int = 0, val score: Int = 0, val selectedId: String? = null,
    val usedHintIds: Set<String> = emptySet(), val processedActions: Set<String> = emptySet(),
    val submittedIds: List<String> = emptyList(),
    val cells: Map<String, String> = emptyMap(), val selectedPath: List<Cell> = emptyList(),
    val foundWordIds: Set<String> = emptySet(), val tileOrder: List<Int> = emptyList(),
    val selectedTileIndices: List<Int> = emptyList(), val matchedPairs: List<MatchPair> = emptyList(),
    val orderedIds: List<String> = emptyList(), val answerHistory: List<String> = emptyList(),
    val revealedHints: Map<String, String> = emptyMap(),
)
sealed interface PlayerAction {
    val id: String
    data class Select(override val id: String, val optionId: String) : PlayerAction
    data class Submit(override val id: String) : PlayerAction
    data class UseHint(override val id: String, val hintId: String) : PlayerAction
    data class Pause(override val id: String) : PlayerAction
    data class Resume(override val id: String) : PlayerAction
    data class InputCell(override val id: String, val row: Int, val col: Int, val token: String) : PlayerAction
    data class SelectPath(override val id: String, val path: List<Cell>) : PlayerAction
    data class AddTile(override val id: String, val tileIndex: Int) : PlayerAction
    data class RemoveTile(override val id: String, val slotIndex: Int) : PlayerAction
    data class Undo(override val id: String) : PlayerAction
    data class Shuffle(override val id: String, val seed: Int) : PlayerAction
    data class Match(override val id: String, val leftId: String, val rightId: String) : PlayerAction
    data class Reorder(override val id: String, val itemIds: List<String>) : PlayerAction
}
data class ValidationResult(val errors: List<String>) { val valid get() = errors.isEmpty() }
interface GamePlugin {
    val type: String
    fun validatePuzzle(puzzle: PuzzleDefinition): ValidationResult
    fun validateSession(puzzle: PuzzleDefinition, session: GameSession): ValidationResult = validateGameSession(puzzle, session)
    fun createSession(puzzle: PuzzleDefinition, sessionId: String): GameSession
    fun evaluate(puzzle: PuzzleDefinition, session: GameSession, action: PlayerAction): GameSession
}
object ScoringEngine {
    fun score(p: PuzzleDefinition, wrong: Int, hints: Set<String>) =
        (50 + 50 * p.difficulty - 10 * wrong - p.hints.filter { it.id in hints }.sumOf { it.penalty }).coerceAtLeast(0)
}
class ChoicePlugin : GamePlugin {
    override val type = "choice"
    override fun validatePuzzle(puzzle: PuzzleDefinition) = ValidationResult(buildList {
        if (puzzle.type != type || puzzle.schemaVersion != 1 || puzzle.rulesVersion != 1) add("unsupported_version")
        if (puzzle.revision < 1 || puzzle.difficulty !in 1..5) add("invalid_revision_or_difficulty")
        if (puzzle.payload.options.size !in 2..6 || puzzle.payload.options.map { it.id }.distinct().size != puzzle.payload.options.size) add("two_to_six_unique_options_required")
        if (puzzle.payload.options.none { it.id == puzzle.payload.correctId }) add("missing_correct_option")
        if (puzzle.payload.options.any { it.id.isBlank() || it.text.isBlank() } || puzzle.payload.prompt.isBlank()) add("blank_content")
        if (puzzle.sourceRefs.isEmpty() || puzzle.explanation.isBlank()) add("missing_provenance")
        if (puzzle.hints.size > 10 || puzzle.hints.any { it.id.isBlank() || it.penalty !in 0..100 } || puzzle.hints.map { it.id }.distinct().size != puzzle.hints.size) add("invalid_hints")
        if (puzzle.normalization !in setOf("exact_v1", "arabic_basic_v1")) add("unsupported_normalization")
        if (puzzle.fixtureOnly && puzzle.status in setOf("approved", "published")) add("unapproved_fixture")
    })
    override fun createSession(puzzle: PuzzleDefinition, sessionId: String): GameSession {
        require(validatePuzzle(puzzle).valid)
        return GameSession(sessionId, puzzle.puzzleId, puzzle.revision)
    }
    override fun evaluate(puzzle: PuzzleDefinition, session: GameSession, action: PlayerAction): GameSession {
        require(session.puzzleId == puzzle.puzzleId && session.revision == puzzle.revision && session.stateVersion in 1..2)
        if (session.status in setOf(SessionStatus.WON, SessionStatus.LOST) || action.id in session.processedActions) return session
        if (session.status == SessionStatus.PAUSED && action !is PlayerAction.Resume) return session
        val next = when (action) {
            is PlayerAction.Resume -> session.copy(status = SessionStatus.ACTIVE)
            is PlayerAction.Pause -> session.copy(status = SessionStatus.PAUSED)
            is PlayerAction.Select -> if (puzzle.payload.options.any { it.id == action.optionId }) session.copy(selectedId = action.optionId) else session
            is PlayerAction.UseHint -> if (puzzle.hints.any { it.id == action.hintId }) session.copy(usedHintIds = session.usedHintIds + action.hintId) else session
            is PlayerAction.Submit -> {
                val selected = session.selectedId ?: return session
                if (selected == puzzle.payload.correctId) session.copy(status = SessionStatus.WON,
                    score = ScoringEngine.score(puzzle, session.wrongSubmissions, session.usedHintIds), submittedIds = session.submittedIds + selected)
                else session.copy(wrongSubmissions = session.wrongSubmissions + 1, score = 0, selectedId = null,
                    status = if (session.wrongSubmissions == 2) SessionStatus.LOST else SessionStatus.ACTIVE,
                    submittedIds = session.submittedIds + selected)
            }
            else -> return session
        }
        return next.copy(processedActions = next.processedActions + action.id)
    }
}
class GameRegistry(plugins: List<GamePlugin> = listOf(ChoicePlugin(), CrosswordPlugin(), WordsearchPlugin(), AnagramPlugin(), MatchingPlugin(), OrderPlugin())) {
    private val entries = plugins.associateBy { it.type }
    init { require(entries.size == plugins.size) }
    val types: Set<String> get() = entries.keys
    fun plugin(type: String): GamePlugin = entries[type] ?: error("Unsupported game type: $type")
}
object SessionCodec {
    val json = Json { encodeDefaults = true }
    fun encode(session: GameSession) = json.encodeToString(session)
    fun decode(value: String): GameSession = json.decodeFromString<GameSession>(value).also {
        require(it.stateVersion in 1..2) { "Unsupported session state version" }
        require(it.revision > 0 && it.packVersion > 0 && it.wrongSubmissions in 0..3 && it.score >= 0)
        require(it.status == SessionStatus.WON || it.score == 0)
        require((it.status == SessionStatus.LOST) == (it.wrongSubmissions == 3))
    }
    fun restore(value: String) = decode(value).let { if (it.status == SessionStatus.ACTIVE) it.copy(status = SessionStatus.PAUSED) else it }
}
data class NormalizationPolicy(val removeMarks: Boolean = false, val foldAlif: Boolean = false, val foldYa: Boolean = false)
object ArabicText {
    fun comparison(original: String, policy: NormalizationPolicy = NormalizationPolicy()): String {
        var result = Normalizer.normalize(original, Normalizer.Form.NFC).trim()
        require(result.none { it in '\u202A'..'\u202E' || it in '\u2066'..'\u2069' }) { "Bidi controls are not answer text" }
        result = result.replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
        if (policy.removeMarks) result = result.replace(Regex("[\\u0640\\u064B-\\u065F\\u0670]"), "").replace(Regex("\\s+"), " ")
        if (policy.foldAlif) result = result.replace(Regex("[أإآ]"), "ا")
        if (policy.foldYa) result = result.replace('ى', 'ي')
        return result
    }
}
