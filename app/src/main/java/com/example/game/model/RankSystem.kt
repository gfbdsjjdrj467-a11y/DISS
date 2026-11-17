package com.example.game.model

enum class RankTier(
    val displayName: String,
    val minRp: Int,
    val primaryHex: Long,
    val secondaryHex: Long,
    val matchedBotDifficulty: BotDifficulty
) {
    BRONZE("Бронза", 0, 0xFFCD7F32, 0xFF8B4513, BotDifficulty.EASY),
    SILVER("Серебро", 500, 0xFFD0D8E2, 0xFF6B7B8C, BotDifficulty.NORMAL),
    GOLD("Золото", 1000, 0xFFFFC107, 0xFFB87800, BotDifficulty.NORMAL),
    PLATINUM("Платина", 1600, 0xFF38BDF8, 0xFF0284C7, BotDifficulty.HARD),
    DIAMOND("Алмаз", 2300, 0xFF818CF8, 0xFF4F46E5, BotDifficulty.HARD),
    MASTER("Мастер", 3100, 0xFFE879F9, 0xFF9333EA, BotDifficulty.EXPERT),
    LEGEND("Легенда", 4000, 0xFFFF3D57, 0xFFFFD166, BotDifficulty.EXPERT)
}

data class RankProgress(
    val rp: Int,
    val tier: RankTier,
    val division: String,
    val nextTier: RankTier?,
    val progressInTier: Float,
    val rpToNextTier: Int
) {
    val fullTitle: String
        get() = if (division.isEmpty()) tier.displayName else "${tier.displayName} $division"
}

object RankSystem {

    fun tierForRp(rp: Int): RankTier {
        val safeRp = rp.coerceAtLeast(0)
        return RankTier.entries.lastOrNull { safeRp >= it.minRp } ?: RankTier.BRONZE
    }

    fun progressForRp(rp: Int): RankProgress {
        val safeRp = rp.coerceAtLeast(0)
        val tier = tierForRp(safeRp)
        val idx = RankTier.entries.indexOf(tier)
        val next = RankTier.entries.getOrNull(idx + 1)

        if (next == null) {
            return RankProgress(
                rp = safeRp,
                tier = tier,
                division = "",
                nextTier = null,
                progressInTier = 1f,
                rpToNextTier = 0
            )
        }

        val span = (next.minRp - tier.minRp).coerceAtLeast(1)
        val inside = (safeRp - tier.minRp).coerceIn(0, span)
        val ratio = inside.toFloat() / span.toFloat()
        val division = when {
            ratio < 0.34f -> "I"
            ratio < 0.67f -> "II"
            else -> "III"
        }

        return RankProgress(
            rp = safeRp,
            tier = tier,
            division = division,
            nextTier = next,
            progressInTier = ratio.coerceIn(0f, 1f),
            rpToNextTier = (next.minRp - safeRp).coerceAtLeast(0)
        )
    }

    fun calculateMatchDelta(
        victory: Boolean,
        kills: Int,
        deaths: Int,
        headshots: Int,
        scoreAlpha: Int,
        scoreBravo: Int,
        currentRp: Int
    ): Int {
        val tier = tierForRp(currentRp)
        val roundMargin = (scoreAlpha - scoreBravo).coerceIn(-5, 5)
        val combatPerf = (kills * 2 + headshots * 2 - deaths).coerceIn(-6, 18)

        return if (victory) {
            val base = 28 + (roundMargin.coerceAtLeast(1) * 3) + (combatPerf / 2)
            base.coerceIn(22, 55)
        } else {
            val tierPenaltyBase = when (tier) {
                RankTier.BRONZE -> 10
                RankTier.SILVER -> 14
                RankTier.GOLD -> 17
                RankTier.PLATINUM -> 20
                RankTier.DIAMOND -> 22
                RankTier.MASTER -> 25
                RankTier.LEGEND -> 26
            }
            val mitigated = tierPenaltyBase - (combatPerf / 3).coerceAtLeast(0)
            val rawDelta = -mitigated.coerceIn(5, 28)
            // Never drop below 0 RP
            if (currentRp + rawDelta < 0) -currentRp else rawDelta
        }
    }
}
