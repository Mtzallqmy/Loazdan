package com.lughzdan.engine
/** Practice progression uses best-score XP, so replay alone cannot unlock levels. */
object ProgressionRules {
 fun unlockedDifficulty(bestScoreXp:Int):Int { require(bestScoreXp>=0);return (1+bestScoreXp/300).coerceAtMost(5) }
 fun playerLevel(bestScoreXp:Int):Int { require(bestScoreXp>=0);return 1+bestScoreXp/250 }
}
