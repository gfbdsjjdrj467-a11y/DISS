package com.example.game.model

enum class WeaponSkin(
    val shaderMode: Int,
    val displayName: String,
    val subtitle: String,
    val unlockKills: Int,
    val unlockRank: RankTier,
    val primaryColor: Triple<Float, Float, Float>,
    val secondaryColor: Triple<Float, Float, Float>,
    val previewHex: Long,
    val previewAccentHex: Long
) {
    FACTORY_STOCK(
        shaderMode = 0,
        displayName = "Заводской стандарт",
        subtitle = "Матовая оружейная сталь",
        unlockKills = 0,
        unlockRank = RankTier.BRONZE,
        primaryColor = Triple(0.20f, 0.22f, 0.25f),
        secondaryColor = Triple(0.45f, 0.48f, 0.52f),
        previewHex = 0xFF333942,
        previewAccentHex = 0xFF7B8794
    ),
    DESERT_SNAKE(
        shaderMode = 1,
        displayName = "Пустынный питон",
        subtitle = "Песчано-оливковый камуфляж",
        unlockKills = 0,
        unlockRank = RankTier.BRONZE,
        primaryColor = Triple(0.68f, 0.55f, 0.34f),
        secondaryColor = Triple(0.32f, 0.27f, 0.18f),
        previewHex = 0xFFAD8C57,
        previewAccentHex = 0xFF52452E
    ),
    COBALT_DIGITAL(
        shaderMode = 2,
        displayName = "Кобальтовый шторм",
        subtitle = "Цифровой пиксель • Металл",
        unlockKills = 5,
        unlockRank = RankTier.SILVER,
        primaryColor = Triple(0.14f, 0.36f, 0.72f),
        secondaryColor = Triple(0.12f, 0.80f, 0.96f),
        previewHex = 0xFF245CB8,
        previewAccentHex = 0xFF1ECCF5
    ),
    CRIMSON_VIPER(
        shaderMode = 3,
        displayName = "Багровая гадюка",
        subtitle = "Карбон + алые полосы",
        unlockKills = 12,
        unlockRank = RankTier.GOLD,
        primaryColor = Triple(0.12f, 0.12f, 0.14f),
        secondaryColor = Triple(0.92f, 0.14f, 0.20f),
        previewHex = 0xFF1E1E24,
        previewAccentHex = 0xFFEB2433
    ),
    ARCTIC_SPECOPS(
        shaderMode = 4,
        displayName = "Арктический призрак",
        subtitle = "Белая керамика + ледяной хром",
        unlockKills = 20,
        unlockRank = RankTier.PLATINUM,
        primaryColor = Triple(0.84f, 0.90f, 0.96f),
        secondaryColor = Triple(0.24f, 0.72f, 0.95f),
        previewHex = 0xFFD6E6F5,
        previewAccentHex = 0xFF3DB8F2
    ),
    NEON_CYBER(
        shaderMode = 5,
        displayName = "Кибер-импульс",
        subtitle = "Анимированные неоновые жилы",
        unlockKills = 30,
        unlockRank = RankTier.DIAMOND,
        primaryColor = Triple(0.08f, 0.10f, 0.16f),
        secondaryColor = Triple(0.08f, 0.96f, 0.88f),
        previewHex = 0xFF141A29,
        previewAccentHex = 0xFF14F5E0
    ),
    GOLD_DYNASTY(
        shaderMode = 6,
        displayName = "Золотая династия",
        subtitle = "Полированное золото 24K + эбен",
        unlockKills = 45,
        unlockRank = RankTier.MASTER,
        primaryColor = Triple(0.96f, 0.76f, 0.24f),
        secondaryColor = Triple(0.16f, 0.11f, 0.08f),
        previewHex = 0xFFF5C23D,
        previewAccentHex = 0xFF291C14
    ),
    LEGEND_VOID(
        shaderMode = 7,
        displayName = "Пламя Легенды",
        subtitle = "Обсидиан + раскалённая лава",
        unlockKills = 60,
        unlockRank = RankTier.LEGEND,
        primaryColor = Triple(0.10f, 0.06f, 0.12f),
        secondaryColor = Triple(1.0f, 0.38f, 0.08f),
        previewHex = 0xFF1A0F1F,
        previewAccentHex = 0xFFFF6114
    );

    fun isUnlocked(totalKills: Int, currentRank: RankTier): Boolean {
        return totalKills >= unlockKills || currentRank.ordinal >= unlockRank.ordinal
    }
}
