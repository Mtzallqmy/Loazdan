package com.lughzdan.engine

class TextAnswerPlugin : GamePlugin {
    override val type = "text_answer"

    override fun validatePuzzle(puzzle: PuzzleDefinition) = ValidationResult(buildList {
        addAll(LiteraryWords.errors(puzzle))
        if (puzzle.type != type || puzzle.schemaVersion != 1 || puzzle.rulesVersion != 1) add("unsupported_version")
        if (puzzle.revision < 1 || puzzle.difficulty !in 1..5) add("invalid_revision_or_difficulty")
        if (puzzle.payload.prompt.isBlank() || puzzle.payload.prompt.length > 2000) add("invalid_prompt")
        if (puzzle.payload.acceptedAnswers.isEmpty() || puzzle.payload.acceptedAnswers.size > 20) add("invalid_accepted_answers")
        if (puzzle.payload.acceptedAnswers.any { it.isBlank() || it.length > 2000 }) add("blank_accepted_answers")
        if (puzzle.sourceRefs.isEmpty() || puzzle.explanation.isBlank()) add("missing_provenance")
        if (puzzle.hints.size > 10 || puzzle.hints.any { it.id.isBlank() || it.penalty !in 0..100 }) add("invalid_hints")
        if (puzzle.normalization !in setOf("exact_v1", "arabic_basic_v1")) add("unsupported_normalization")
        if (puzzle.fixtureOnly && puzzle.status in setOf("approved", "published")) add("unapproved_fixture")
    })

    override fun createSession(puzzle: PuzzleDefinition, sessionId: String): GameSession {
        require(validatePuzzle(puzzle).valid)
        val s=GameSession(sessionId, puzzle.puzzleId, puzzle.revision, stateVersion = 2,displaySeed=sessionId.hashCode())
        return if(LiteraryWords.supports(puzzle))s.copy(answerInput=LiteraryWords.input(puzzle,s,"")) else s
    }

    override fun evaluate(puzzle: PuzzleDefinition, session: GameSession, action: PlayerAction): GameSession {
        require(session.puzzleId == puzzle.puzzleId && session.revision == puzzle.revision && session.stateVersion in 1..2)
        if (session.status in setOf(SessionStatus.WON, SessionStatus.LOST) || action.id in session.processedActions) return session
        if (session.status == SessionStatus.PAUSED && action !is PlayerAction.Resume) return session
        val next = when (action) {
            is PlayerAction.Pause -> session.copy(status = SessionStatus.PAUSED)
            is PlayerAction.Resume -> session.copy(status = SessionStatus.ACTIVE)
            is PlayerAction.UseHint -> {
                val h = puzzle.hints.firstOrNull { it.id == action.hintId }
                if (h == null || h.id in session.usedHintIds) session
                else session.copy(
                    usedHintIds = session.usedHintIds + h.id,
                    revealedHints = session.revealedHints + (h.id to HintProvider.reveal(puzzle, session, h))
                ).let {if(LiteraryWords.supports(puzzle))it.copy(answerInput=LiteraryWords.input(puzzle,it,it.answerInput)) else it}
            }
            is PlayerAction.InputAnswer -> if (answerTextValid(action.answer)) session.copy(answerInput = if(LiteraryWords.supports(puzzle))LiteraryWords.input(puzzle,session,action.answer) else action.answer) else session
            is PlayerAction.Submit -> {
                if (session.answerInput.isBlank() || (LiteraryWords.supports(puzzle) && !LiteraryWords.complete(puzzle,session))) session
                else {
                    val candidate = normalized(puzzle, session.answerInput)
                    val correct = puzzle.payload.acceptedAnswers.any { normalized(puzzle, it) == candidate }
                    if (correct) {
                        session.copy(
                            status = SessionStatus.WON,
                            score = ScoringEngine.score(puzzle, session.wrongSubmissions, session.usedHintIds)
                        )
                    } else {
                        val wrong = session.wrongSubmissions + 1
                        session.copy(
                            wrongSubmissions = wrong,
                            status = if (wrong >= 3) SessionStatus.LOST else SessionStatus.ACTIVE
                        )
                    }
                }
            }
            else -> session
        }
        return if (next != session || action is PlayerAction.Resume || action is PlayerAction.Pause) {
            next.copy(processedActions = next.processedActions + action.id)
        } else session
    }
}

class SequencePlugin : GamePlugin {
    override val type = "sequence"

    override fun validatePuzzle(puzzle: PuzzleDefinition) = ValidationResult(buildList {
        if (puzzle.type != type || puzzle.schemaVersion != 1 || puzzle.rulesVersion != 1) add("unsupported_version")
        if (puzzle.revision < 1 || puzzle.difficulty !in 1..5) add("invalid_revision_or_difficulty")
        if (puzzle.payload.terms.size !in 3..20 || puzzle.payload.terms.any { it !in -1000000..1000000 }) add("invalid_terms")
        if (puzzle.payload.answer !in -1000000..1000000) add("invalid_answer")
        if (puzzle.payload.rule.isBlank() || puzzle.payload.rule.length > 2000) add("invalid_rule")
        if (puzzle.sourceRefs.isEmpty() || puzzle.explanation.isBlank()) add("missing_provenance")
        if (puzzle.hints.size > 10 || puzzle.hints.any { it.id.isBlank() || it.penalty !in 0..100 }) add("invalid_hints")
        if (puzzle.normalization !in setOf("exact_v1", "arabic_basic_v1")) add("unsupported_normalization")
        if (puzzle.fixtureOnly && puzzle.status in setOf("approved", "published")) add("unapproved_fixture")
    })

    override fun createSession(puzzle: PuzzleDefinition, sessionId: String): GameSession {
        require(validatePuzzle(puzzle).valid)
        return GameSession(sessionId, puzzle.puzzleId, puzzle.revision, stateVersion = 2)
    }

    private fun parseArabicNumber(input: String): Int? {
        val converted = input.trim().map { ch ->
            when (ch) {
                in '٠'..'٩' -> (ch - '٠' + '0'.code).toChar()
                else -> ch
            }
        }.joinToString("")
        return converted.toIntOrNull()
    }

    override fun evaluate(puzzle: PuzzleDefinition, session: GameSession, action: PlayerAction): GameSession {
        require(session.puzzleId == puzzle.puzzleId && session.revision == puzzle.revision && session.stateVersion in 1..2)
        if (session.status in setOf(SessionStatus.WON, SessionStatus.LOST) || action.id in session.processedActions) return session
        if (session.status == SessionStatus.PAUSED && action !is PlayerAction.Resume) return session
        val next = when (action) {
            is PlayerAction.Pause -> session.copy(status = SessionStatus.PAUSED)
            is PlayerAction.Resume -> session.copy(status = SessionStatus.ACTIVE)
            is PlayerAction.UseHint -> {
                val h = puzzle.hints.firstOrNull { it.id == action.hintId }
                if (h == null || h.id in session.usedHintIds) session
                else session.copy(
                    usedHintIds = session.usedHintIds + h.id,
                    revealedHints = session.revealedHints + (h.id to HintProvider.reveal(puzzle, session, h))
                )
            }
            is PlayerAction.InputAnswer -> if (answerTextValid(action.answer)) session.copy(answerInput = action.answer) else session
            is PlayerAction.Submit -> {
                val parsed = parseArabicNumber(session.answerInput)
                if (session.answerInput.isBlank() || parsed == null) session
                else {
                    val correct = (parsed == puzzle.payload.answer)
                    if (correct) {
                        session.copy(
                            status = SessionStatus.WON,
                            score = ScoringEngine.score(puzzle, session.wrongSubmissions, session.usedHintIds)
                        )
                    } else {
                        val wrong = session.wrongSubmissions + 1
                        session.copy(
                            wrongSubmissions = wrong,
                            status = if (wrong >= 3) SessionStatus.LOST else SessionStatus.ACTIVE
                        )
                    }
                }
            }
            else -> session
        }
        return if (next != session || action is PlayerAction.Resume || action is PlayerAction.Pause) {
            next.copy(processedActions = next.processedActions + action.id)
        } else session
    }
}

class ImageGuessPlugin : GamePlugin {
    override val type = "image_guess"

    override fun validatePuzzle(puzzle: PuzzleDefinition) = ValidationResult(buildList {
        if (puzzle.type != type || puzzle.schemaVersion != 1 || puzzle.rulesVersion != 1) add("unsupported_version")
        if (puzzle.revision < 1 || puzzle.difficulty !in 1..5) add("invalid_revision_or_difficulty")
        if (puzzle.payload.mode !in setOf("four", "reveal", "shadow", "symbols")) add("invalid_mode")
        if (puzzle.payload.mode == "four" && puzzle.payload.assets.size != 4) add("four_mode_requires_four_assets")
        if (puzzle.payload.assets.isEmpty() || puzzle.payload.assets.size > 4) add("invalid_assets_count")
        if (puzzle.payload.assets.any { it.assetId.isBlank() || it.alt.isBlank() }) add("invalid_assets_content")
        if (puzzle.payload.acceptedAnswers.isEmpty() || puzzle.payload.acceptedAnswers.size > 10) add("invalid_accepted_answers")
        if (puzzle.payload.revealSteps !in 1..10) add("invalid_reveal_steps")
        if (puzzle.sourceRefs.isEmpty() || puzzle.explanation.isBlank()) add("missing_provenance")
        if (puzzle.hints.size > 10 || puzzle.hints.any { it.id.isBlank() || it.penalty !in 0..100 }) add("invalid_hints")
        if (puzzle.normalization !in setOf("exact_v1", "arabic_basic_v1")) add("unsupported_normalization")
        if (puzzle.fixtureOnly && puzzle.status in setOf("approved", "published")) add("unapproved_fixture")
    })

    override fun createSession(puzzle: PuzzleDefinition, sessionId: String): GameSession {
        require(validatePuzzle(puzzle).valid)
        return GameSession(sessionId, puzzle.puzzleId, puzzle.revision, stateVersion = 2)
    }

    override fun evaluate(puzzle: PuzzleDefinition, session: GameSession, action: PlayerAction): GameSession {
        require(session.puzzleId == puzzle.puzzleId && session.revision == puzzle.revision && session.stateVersion in 1..2)
        if (session.status in setOf(SessionStatus.WON, SessionStatus.LOST) || action.id in session.processedActions) return session
        if (session.status == SessionStatus.PAUSED && action !is PlayerAction.Resume) return session
        val next = when (action) {
            is PlayerAction.Pause -> session.copy(status = SessionStatus.PAUSED)
            is PlayerAction.Resume -> session.copy(status = SessionStatus.ACTIVE)
            is PlayerAction.UseHint -> {
                val h = puzzle.hints.firstOrNull { it.id == action.hintId }
                if (h == null || h.id in session.usedHintIds) session
                else session.copy(
                    usedHintIds = session.usedHintIds + h.id,
                    revealedHints = session.revealedHints + (h.id to HintProvider.reveal(puzzle, session, h))
                )
            }
            is PlayerAction.RevealRegion -> {
                val regionIndex = action.row * puzzle.payload.revealSteps + action.col
                if (regionIndex in 0 until (puzzle.payload.revealSteps * puzzle.payload.revealSteps)) {
                    session.copy(revealedRegions = session.revealedRegions + regionIndex)
                } else session
            }
            is PlayerAction.InputAnswer -> if (answerTextValid(action.answer)) session.copy(answerInput = action.answer) else session
            is PlayerAction.Submit -> {
                if (session.answerInput.isBlank()) session
                else {
                    val candidate = normalized(puzzle, session.answerInput)
                    val correct = puzzle.payload.acceptedAnswers.any { normalized(puzzle, it) == candidate }
                    if (correct) {
                        session.copy(
                            status = SessionStatus.WON,
                            score = ScoringEngine.score(puzzle, session.wrongSubmissions, session.usedHintIds, session.revealedRegions.size)
                        )
                    } else {
                        val wrong = session.wrongSubmissions + 1
                        session.copy(
                            wrongSubmissions = wrong,
                            status = if (wrong >= 3) SessionStatus.LOST else SessionStatus.ACTIVE
                        )
                    }
                }
            }
            else -> session
        }
        return if (next != session || action is PlayerAction.Resume || action is PlayerAction.Pause) {
            next.copy(processedActions = next.processedActions + action.id)
        } else session
    }
}
