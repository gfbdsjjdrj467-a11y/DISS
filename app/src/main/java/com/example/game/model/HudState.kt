package com.example.game.model

data class KillFeedEntry(
    val id: Long,
    val killerName: String,
    val victimName: String,
    val weaponName: String,
    val isHeadshot: Boolean,
    val killerIsPlayer: Boolean,
    val victimIsPlayer: Boolean,
    val timestampMs: Long
)

data class MinimapBlip(
    val relX: Float, // relative to player in world units
    val relZ: Float,
    val isEnemy: Boolean,
    val isPickup: Boolean = false
)

data class MinimapWall(
    val minX: Float,
    val minZ: Float,
    val maxX: Float,
    val maxZ: Float
)

data class MatchResultSummary(
    val victory: Boolean,
    val title: String,
    val subtitle: String,
    val mode: GameModeType,
    val mapId: MapId,
    val scoreAlpha: Int,
    val scoreBravo: Int,
    val playerKills: Int,
    val playerDeaths: Int,
    val headshots: Int,
    val shotsFired: Int,
    val shotsHit: Int,
    val wavesCleared: Int,
    val durationSec: Int,
    val rpBefore: Int = 0,
    val rpAfter: Int = 0,
    val rankDelta: Int = 0
) {
    val accuracyPercent: Float
        get() = if (shotsFired > 0) (shotsHit.toFloat() / shotsFired.toFloat()) * 100f else 0f
}

data class HudState(
    val isActiveMatch: Boolean = false,
    val isPaused: Boolean = false,
    val health: Int = 100,
    val maxHealth: Int = 100,
    val armor: Int = 50,
    val isDead: Boolean = false,
    val respawnSecondsLeft: Float = 0f,
    val activeWeaponId: WeaponId = WeaponId.AR_M4A1,
    val activeWeaponSkin: WeaponSkin = WeaponSkin.FACTORY_STOCK,
    val secondaryWeaponId: WeaponId = WeaponId.PISTOL_P9,
    val ammoInMag: Int = 30,
    val ammoReserve: Int = 150,
    val isReloading: Boolean = false,
    val reloadProgress: Float = 0f,
    val isAds: Boolean = false,
    val isSprinting: Boolean = false,
    val isCrouching: Boolean = false,
    val crosshairSpreadPx: Float = 14f,
    val hitmarkerAlpha: Float = 0f,
    val hitmarkerHeadshot: Boolean = false,
    val damageFlashAlpha: Float = 0f,
    val damageDirectionDeg: Float = 0f,
    val nearDoorPrompt: String? = null,
    val pickupNotification: String? = null,
    val playerX: Float = 0f,
    val playerZ: Float = 0f,
    val playerYawDeg: Float = 0f,
    val mapMode: GameModeType = GameModeType.TEAM_DEATHMATCH,
    val mapId: MapId = MapId.DESERT_BASE,
    val scoreAlpha: Int = 0,
    val scoreBravo: Int = 0,
    val playerKills: Int = 0,
    val playerDeaths: Int = 0,
    val roundNumber: Int = 1,
    val totalRounds: Int = 5,
    val waveNumber: Int = 1,
    val aliveAlpha: Int = 0,
    val aliveBravo: Int = 0,
    val matchTimeRemainingSec: Int = 180,
    val killFeed: List<KillFeedEntry> = emptyList(),
    val minimapBlips: List<MinimapBlip> = emptyList(),
    val minimapWalls: List<MinimapWall> = emptyList(),
    val targetedBotName: String? = null,
    val targetedBotHpRatio: Float = 0f,
    val fpsEstimate: Int = 60,
    val matchResult: MatchResultSummary? = null
)
