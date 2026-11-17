package com.example.game.model

enum class MapId(
    val displayName: String,
    val subtitle: String,
    val description: String,
    val accentHex: Long
) {
    DESERT_BASE(
        displayName = "Desert Base",
        subtitle = "Пустынная база",
        description = "Военный аванпост в пустыне: ангары, контейнеры, смотровая вышка и линии мешков с песком.",
        accentHex = 0xFFE69F45
    ),
    URBAN_CITY(
        displayName = "Urban City",
        subtitle = "Городские улицы",
        description = "Многоэтажные кварталы, узкие переулки, магазины, укрытия из бетонных блоков и парковка.",
        accentHex = 0xFF4FC3F7
    ),
    MILITARY_FACILITY(
        displayName = "Military Facility",
        subtitle = "Военный объект",
        description = "Закрытый комплекс с казармами, складами, коридорами, учебным полигоном и гермодверями.",
        accentHex = 0xFF81C784
    ),
    SNOW_VILLAGE(
        displayName = "Snow Village",
        subtitle = "Заснеженная деревня",
        description = "Альпийское поселение с деревянными домами, замёрзшим озером, заснеженными дорогами и снегопадом.",
        accentHex = 0xFF80DEEA
    ),
    INDUSTRIAL_ZONE(
        displayName = "Industrial Zone",
        subtitle = "Промзона",
        description = "Заводская территория с цехами, металлическими силосами, контейнерами и стальными мостиками.",
        accentHex = 0xFFFF8A65
    )
}

enum class GameModeType(
    val displayName: String,
    val shortCode: String,
    val description: String
) {
    RANKED_2V2(
        displayName = "2x2 На звание",
        shortCode = "2x2",
        description = "Ранговые бои 2 на 2 до 5 побед в раундах с напарником. Повышайте звание от Бронзы до Легенды!"
    ),
    TEAM_DEATHMATCH(
        displayName = "Team Deathmatch",
        shortCode = "TDM",
        description = "Командный бой 2 отрядов (Alpha vs Bravo). Побеждает команда с лучшим счётом до конца таймера."
    ),
    FREE_FOR_ALL(
        displayName = "Free For All",
        shortCode = "FFA",
        description = "Каждый сам за себя. Лидируйте в общей таблице устранений против всех ботов."
    ),
    ELIMINATION(
        displayName = "Elimination",
        shortCode = "ELIM",
        description = "Тактические раунды без возрождения (до 3 побед). Выживет сильнейший отряд."
    ),
    SURVIVAL(
        displayName = "Survival",
        shortCode = "SURV",
        description = "Бесконечные волны наступающих ботов с нарастающей сложностью и тактикой."
    ),
    PRACTICE(
        displayName = "Practice Mode",
        shortCode = "PRAC",
        description = "Свободная тренировка стрельбы, передвижения и всего арсенала без давления."
    )
}

enum class BotDifficulty(
    val displayName: String,
    val reactionDelaySec: Float,
    val aimJitterDeg: Float,
    val fireBurstChance: Float,
    val coverThresholdHp: Float,
    val moveSpeedMult: Float
) {
    EASY("Easy", 0.72f, 9.5f, 0.45f, 0.15f, 0.78f),
    NORMAL("Normal", 0.42f, 5.2f, 0.68f, 0.35f, 0.92f),
    HARD("Hard", 0.24f, 2.8f, 0.85f, 0.50f, 1.02f),
    EXPERT("Expert", 0.14f, 1.4f, 0.96f, 0.62f, 1.12f)
}

enum class GraphicsQuality(
    val displayName: String,
    val maxParticles: Int,
    val drawDistance: Float,
    val dynamicLightsEnabled: Boolean,
    val surfaceDetailLevel: Int
) {
    LOW("Low", 48, 70f, false, 0),
    MEDIUM("Medium", 110, 105f, true, 1),
    HIGH("High", 200, 145f, true, 2)
}

enum class WeaponId {
    PISTOL_P9,
    AR_M4A1,
    SMG_MPX,
    SHOTGUN_M870,
    SNIPER_AWP,
    LMG_M249
}

enum class WeaponCategory(val displayName: String) {
    PISTOL("Пистолет"),
    ASSAULT_RIFLE("Штурмовая винтовка"),
    SMG("Пистолет-пулемёт"),
    SHOTGUN("Дробовик"),
    SNIPER("Снайперская винтовка"),
    LMG("Лёгкий пулемёт")
}

enum class BotAiState(val label: String) {
    IDLE("IDLE"),
    PATROL("PATROL"),
    INVESTIGATE("INVESTIGATE"),
    CHASE("CHASE"),
    ATTACK("ATTACK"),
    TAKE_COVER("TAKE COVER"),
    RELOAD("RELOAD"),
    SEARCH("SEARCH"),
    RETREAT("RETREAT"),
    DEAD("DEAD")
}

enum class Team(val displayName: String) {
    ALPHA("ALPHA"),
    BRAVO("BRAVO"),
    NONE("SOLO")
}

enum class SurfaceType {
    SAND,
    CONCRETE,
    METAL,
    SNOW,
    WOOD
}

enum class PickupType(val displayName: String) {
    HEALTH("Аптечка +35 HP"),
    AMMO("Боеприпасы"),
    ARMOR("Броня +35")
}
