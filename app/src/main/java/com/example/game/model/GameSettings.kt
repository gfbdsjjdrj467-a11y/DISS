package com.example.game.model

data class HudControlOffsets(
    val joystickOffsetX: Float = 0f,
    val joystickOffsetY: Float = 0f,
    val fireOffsetX: Float = 0f,
    val fireOffsetY: Float = 0f,
    val adsOffsetX: Float = 0f,
    val adsOffsetY: Float = 0f,
    val reloadOffsetX: Float = 0f,
    val reloadOffsetY: Float = 0f,
    val jumpOffsetX: Float = 0f,
    val jumpOffsetY: Float = 0f,
    val crouchOffsetX: Float = 0f,
    val crouchOffsetY: Float = 0f,
    val interactOffsetX: Float = 0f,
    val interactOffsetY: Float = 0f,
    val swapOffsetX: Float = 0f,
    val swapOffsetY: Float = 0f
)

data class GameSettings(
    // Graphics
    val graphicsQuality: GraphicsQuality = GraphicsQuality.MEDIUM,
    val textureQuality: Int = 2, // 0 Low, 1 Med, 2 High
    val shadowQuality: Int = 1,  // 0 Off, 1 Soft, 2 Crisp
    val effectsQuality: Int = 2, // 0 Low, 1 Med, 2 High
    val fpsLimit: Int = 60,      // 30, 60, 120

    // Controls
    val cameraSensitivity: Float = 1.0f,
    val adsSensitivity: Float = 0.65f,
    val buttonScale: Float = 1.0f,
    val hudOpacity: Float = 0.85f,
    val joystickFloating: Boolean = false,
    val hudOffsets: HudControlOffsets = HudControlOffsets(),

    // Automation
    val autoSprint: Boolean = true,
    val autoReload: Boolean = true,
    val autoSwitchWeapon: Boolean = true,
    val aimAssistEnabled: Boolean = true,
    val aimAssistStrength: Float = 0.55f,
    val autoPickup: Boolean = true,
    val autoOpenDoors: Boolean = false,
    val autoAdsOnFire: Boolean = false,

    // Audio
    val masterVolume: Float = 0.85f,
    val musicVolume: Float = 0.45f,
    val sfxVolume: Float = 0.90f,
    val uiVolume: Float = 0.75f,

    // Gameplay & Loadout
    val botDifficulty: BotDifficulty = BotDifficulty.NORMAL,
    val botCount: Int = 6,
    val selectedMap: MapId = MapId.DESERT_BASE,
    val selectedMode: GameModeType = GameModeType.TEAM_DEATHMATCH,
    val primaryWeapon: WeaponId = WeaponId.AR_M4A1,
    val secondaryWeapon: WeaponId = WeaponId.PISTOL_P9
)
