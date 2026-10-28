package com.lughzdan.engine
import org.junit.Assert.*
import org.junit.Test
class ProgressionTest {
 @Test fun difficultyUnlocksAtDocumentedXpBoundaries(){assertEquals(1,ProgressionRules.unlockedDifficulty(299));assertEquals(2,ProgressionRules.unlockedDifficulty(300));assertEquals(5,ProgressionRules.unlockedDifficulty(5000))}
 @Test(expected=IllegalArgumentException::class) fun invalidXpRejected(){ProgressionRules.playerLevel(-1)}
}
