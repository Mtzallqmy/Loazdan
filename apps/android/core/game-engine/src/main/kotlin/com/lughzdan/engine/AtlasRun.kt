package com.lughzdan.engine

import kotlinx.serialization.Serializable

@Serializable
data class AtlasRun(
    val id: String,
    val kind: String,
    val title: String,
    val puzzleIds: List<String>,
    val playerNames: List<String> = emptyList(),
    val scores: List<Int> = listOf(0, 0),
    val step: Int = 0,
    val activeSessionId: String? = null,
    val updatedAt: Long = 0,
) {
    val finished: Boolean get() = step == puzzleIds.size
    val playerIndex: Int get() = step % 2
}
