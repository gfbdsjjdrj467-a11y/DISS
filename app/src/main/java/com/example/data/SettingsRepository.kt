package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.game.model.BotDifficulty
import com.example.game.model.GameModeType
import com.example.game.model.GameSettings
import com.example.game.model.GraphicsQuality
import com.example.game.model.HudControlOffsets
import com.example.game.model.MapId
import com.example.game.model.WeaponId
import com.example.game.model.WeaponSkin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "strike_arena_prefs")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val GRAPHICS_QUALITY = stringPreferencesKey("graphics_quality")
        val TEXTURE_QUALITY = intPreferencesKey("texture_quality")
        val SHADOW_QUALITY = intPreferencesKey("shadow_quality")
        val EFFECTS_QUALITY = intPreferencesKey("effects_quality")
        val FPS_LIMIT = intPreferencesKey("fps_limit")

        val CAMERA_SENS = floatPreferencesKey("camera_sens")
        val ADS_SENS = floatPreferencesKey("ads_sens")
        val BUTTON_SCALE = floatPreferencesKey("button_scale")
        val HUD_OPACITY = floatPreferencesKey("hud_opacity")
        val JOYSTICK_FLOATING = booleanPreferencesKey("joystick_floating")

        val JOYSTICK_X = floatPreferencesKey("hud_joy_x")
        val JOYSTICK_Y = floatPreferencesKey("hud_joy_y")
        val FIRE_X = floatPreferencesKey("hud_fire_x")
        val FIRE_Y = floatPreferencesKey("hud_fire_y")
        val ADS_X = floatPreferencesKey("hud_ads_x")
        val ADS_Y = floatPreferencesKey("hud_ads_y")
        val RELOAD_X = floatPreferencesKey("hud_reload_x")
        val RELOAD_Y = floatPreferencesKey("hud_reload_y")
        val JUMP_X = floatPreferencesKey("hud_jump_x")
        val JUMP_Y = floatPreferencesKey("hud_jump_y")
        val CROUCH_X = floatPreferencesKey("hud_crouch_x")
        val CROUCH_Y = floatPreferencesKey("hud_crouch_y")
        val INTERACT_X = floatPreferencesKey("hud_interact_x")
        val INTERACT_Y = floatPreferencesKey("hud_interact_y")
        val SWAP_X = floatPreferencesKey("hud_swap_x")
        val SWAP_Y = floatPreferencesKey("hud_swap_y")

        val AUTO_SPRINT = booleanPreferencesKey("auto_sprint")
        val AUTO_RELOAD = booleanPreferencesKey("auto_reload")
        val AUTO_SWITCH = booleanPreferencesKey("auto_switch")
        val AIM_ASSIST = booleanPreferencesKey("aim_assist")
        val AIM_ASSIST_STRENGTH = floatPreferencesKey("aim_assist_strength")
        val AUTO_PICKUP = booleanPreferencesKey("auto_pickup")
        val AUTO_DOORS = booleanPreferencesKey("auto_doors")
        val AUTO_ADS_FIRE = booleanPreferencesKey("auto_ads_fire")

        val MASTER_VOL = floatPreferencesKey("master_vol")
        val MUSIC_VOL = floatPreferencesKey("music_vol")
        val SFX_VOL = floatPreferencesKey("sfx_vol")
        val UI_VOL = floatPreferencesKey("ui_vol")

        val BOT_DIFFICULTY = stringPreferencesKey("bot_difficulty")
        val BOT_COUNT = intPreferencesKey("bot_count")
        val SELECTED_MAP = stringPreferencesKey("selected_map")
        val SELECTED_MODE = stringPreferencesKey("selected_mode")
        val PRIMARY_WEAPON = stringPreferencesKey("primary_weapon")
        val SECONDARY_WEAPON = stringPreferencesKey("secondary_weapon")

        fun skinKey(weaponId: WeaponId) = stringPreferencesKey("skin_${weaponId.name}")
    }

    val settingsFlow: Flow<GameSettings> = context.settingsDataStore.data
        .catch { e ->
            if (e is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw e
        }
        .map { prefs ->
            GameSettings(
                graphicsQuality = enumValueOrDefault(prefs[Keys.GRAPHICS_QUALITY], GraphicsQuality.MEDIUM),
                textureQuality = prefs[Keys.TEXTURE_QUALITY] ?: 2,
                shadowQuality = prefs[Keys.SHADOW_QUALITY] ?: 1,
                effectsQuality = prefs[Keys.EFFECTS_QUALITY] ?: 2,
                fpsLimit = prefs[Keys.FPS_LIMIT] ?: 60,
                cameraSensitivity = prefs[Keys.CAMERA_SENS] ?: 1.0f,
                adsSensitivity = prefs[Keys.ADS_SENS] ?: 0.65f,
                buttonScale = prefs[Keys.BUTTON_SCALE] ?: 1.0f,
                hudOpacity = prefs[Keys.HUD_OPACITY] ?: 0.85f,
                joystickFloating = prefs[Keys.JOYSTICK_FLOATING] ?: false,
                hudOffsets = HudControlOffsets(
                    joystickOffsetX = prefs[Keys.JOYSTICK_X] ?: 0f,
                    joystickOffsetY = prefs[Keys.JOYSTICK_Y] ?: 0f,
                    fireOffsetX = prefs[Keys.FIRE_X] ?: 0f,
                    fireOffsetY = prefs[Keys.FIRE_Y] ?: 0f,
                    adsOffsetX = prefs[Keys.ADS_X] ?: 0f,
                    adsOffsetY = prefs[Keys.ADS_Y] ?: 0f,
                    reloadOffsetX = prefs[Keys.RELOAD_X] ?: 0f,
                    reloadOffsetY = prefs[Keys.RELOAD_Y] ?: 0f,
                    jumpOffsetX = prefs[Keys.JUMP_X] ?: 0f,
                    jumpOffsetY = prefs[Keys.JUMP_Y] ?: 0f,
                    crouchOffsetX = prefs[Keys.CROUCH_X] ?: 0f,
                    crouchOffsetY = prefs[Keys.CROUCH_Y] ?: 0f,
                    interactOffsetX = prefs[Keys.INTERACT_X] ?: 0f,
                    interactOffsetY = prefs[Keys.INTERACT_Y] ?: 0f,
                    swapOffsetX = prefs[Keys.SWAP_X] ?: 0f,
                    swapOffsetY = prefs[Keys.SWAP_Y] ?: 0f
                ),
                autoSprint = prefs[Keys.AUTO_SPRINT] ?: true,
                autoReload = prefs[Keys.AUTO_RELOAD] ?: true,
                autoSwitchWeapon = prefs[Keys.AUTO_SWITCH] ?: true,
                aimAssistEnabled = prefs[Keys.AIM_ASSIST] ?: true,
                aimAssistStrength = prefs[Keys.AIM_ASSIST_STRENGTH] ?: 0.55f,
                autoPickup = prefs[Keys.AUTO_PICKUP] ?: true,
                autoOpenDoors = prefs[Keys.AUTO_DOORS] ?: false,
                autoAdsOnFire = prefs[Keys.AUTO_ADS_FIRE] ?: false,
                masterVolume = prefs[Keys.MASTER_VOL] ?: 0.85f,
                musicVolume = prefs[Keys.MUSIC_VOL] ?: 0.45f,
                sfxVolume = prefs[Keys.SFX_VOL] ?: 0.90f,
                uiVolume = prefs[Keys.UI_VOL] ?: 0.75f,
                botDifficulty = enumValueOrDefault(prefs[Keys.BOT_DIFFICULTY], BotDifficulty.NORMAL),
                botCount = (prefs[Keys.BOT_COUNT] ?: 6).coerceIn(2, 12),
                selectedMap = enumValueOrDefault(prefs[Keys.SELECTED_MAP], MapId.DESERT_BASE),
                selectedMode = enumValueOrDefault(prefs[Keys.SELECTED_MODE], GameModeType.TEAM_DEATHMATCH),
                primaryWeapon = enumValueOrDefault(prefs[Keys.PRIMARY_WEAPON], WeaponId.AR_M4A1),
                secondaryWeapon = enumValueOrDefault(prefs[Keys.SECONDARY_WEAPON], WeaponId.PISTOL_P9),
                weaponSkins = WeaponId.entries.associateWith { wId ->
                    enumValueOrDefault(prefs[Keys.skinKey(wId)], WeaponSkin.FACTORY_STOCK)
                }
            )
        }

    suspend fun updateSettings(transform: (GameSettings) -> GameSettings) {
        context.settingsDataStore.edit { prefs ->
            val current = GameSettings(
                graphicsQuality = enumValueOrDefault(prefs[Keys.GRAPHICS_QUALITY], GraphicsQuality.MEDIUM),
                textureQuality = prefs[Keys.TEXTURE_QUALITY] ?: 2,
                shadowQuality = prefs[Keys.SHADOW_QUALITY] ?: 1,
                effectsQuality = prefs[Keys.EFFECTS_QUALITY] ?: 2,
                fpsLimit = prefs[Keys.FPS_LIMIT] ?: 60,
                cameraSensitivity = prefs[Keys.CAMERA_SENS] ?: 1.0f,
                adsSensitivity = prefs[Keys.ADS_SENS] ?: 0.65f,
                buttonScale = prefs[Keys.BUTTON_SCALE] ?: 1.0f,
                hudOpacity = prefs[Keys.HUD_OPACITY] ?: 0.85f,
                joystickFloating = prefs[Keys.JOYSTICK_FLOATING] ?: false,
                hudOffsets = HudControlOffsets(
                    joystickOffsetX = prefs[Keys.JOYSTICK_X] ?: 0f,
                    joystickOffsetY = prefs[Keys.JOYSTICK_Y] ?: 0f,
                    fireOffsetX = prefs[Keys.FIRE_X] ?: 0f,
                    fireOffsetY = prefs[Keys.FIRE_Y] ?: 0f,
                    adsOffsetX = prefs[Keys.ADS_X] ?: 0f,
                    adsOffsetY = prefs[Keys.ADS_Y] ?: 0f,
                    reloadOffsetX = prefs[Keys.RELOAD_X] ?: 0f,
                    reloadOffsetY = prefs[Keys.RELOAD_Y] ?: 0f,
                    jumpOffsetX = prefs[Keys.JUMP_X] ?: 0f,
                    jumpOffsetY = prefs[Keys.JUMP_Y] ?: 0f,
                    crouchOffsetX = prefs[Keys.CROUCH_X] ?: 0f,
                    crouchOffsetY = prefs[Keys.CROUCH_Y] ?: 0f,
                    interactOffsetX = prefs[Keys.INTERACT_X] ?: 0f,
                    interactOffsetY = prefs[Keys.INTERACT_Y] ?: 0f,
                    swapOffsetX = prefs[Keys.SWAP_X] ?: 0f,
                    swapOffsetY = prefs[Keys.SWAP_Y] ?: 0f
                ),
                autoSprint = prefs[Keys.AUTO_SPRINT] ?: true,
                autoReload = prefs[Keys.AUTO_RELOAD] ?: true,
                autoSwitchWeapon = prefs[Keys.AUTO_SWITCH] ?: true,
                aimAssistEnabled = prefs[Keys.AIM_ASSIST] ?: true,
                aimAssistStrength = prefs[Keys.AIM_ASSIST_STRENGTH] ?: 0.55f,
                autoPickup = prefs[Keys.AUTO_PICKUP] ?: true,
                autoOpenDoors = prefs[Keys.AUTO_DOORS] ?: false,
                autoAdsOnFire = prefs[Keys.AUTO_ADS_FIRE] ?: false,
                masterVolume = prefs[Keys.MASTER_VOL] ?: 0.85f,
                musicVolume = prefs[Keys.MUSIC_VOL] ?: 0.45f,
                sfxVolume = prefs[Keys.SFX_VOL] ?: 0.90f,
                uiVolume = prefs[Keys.UI_VOL] ?: 0.75f,
                botDifficulty = enumValueOrDefault(prefs[Keys.BOT_DIFFICULTY], BotDifficulty.NORMAL),
                botCount = (prefs[Keys.BOT_COUNT] ?: 6).coerceIn(2, 12),
                selectedMap = enumValueOrDefault(prefs[Keys.SELECTED_MAP], MapId.DESERT_BASE),
                selectedMode = enumValueOrDefault(prefs[Keys.SELECTED_MODE], GameModeType.TEAM_DEATHMATCH),
                primaryWeapon = enumValueOrDefault(prefs[Keys.PRIMARY_WEAPON], WeaponId.AR_M4A1),
                secondaryWeapon = enumValueOrDefault(prefs[Keys.SECONDARY_WEAPON], WeaponId.PISTOL_P9),
                weaponSkins = WeaponId.entries.associateWith { wId ->
                    enumValueOrDefault(prefs[Keys.skinKey(wId)], WeaponSkin.FACTORY_STOCK)
                }
            )
            val next = transform(current)
            prefs[Keys.GRAPHICS_QUALITY] = next.graphicsQuality.name
            prefs[Keys.TEXTURE_QUALITY] = next.textureQuality
            prefs[Keys.SHADOW_QUALITY] = next.shadowQuality
            prefs[Keys.EFFECTS_QUALITY] = next.effectsQuality
            prefs[Keys.FPS_LIMIT] = next.fpsLimit
            prefs[Keys.CAMERA_SENS] = next.cameraSensitivity
            prefs[Keys.ADS_SENS] = next.adsSensitivity
            prefs[Keys.BUTTON_SCALE] = next.buttonScale
            prefs[Keys.HUD_OPACITY] = next.hudOpacity
            prefs[Keys.JOYSTICK_FLOATING] = next.joystickFloating
            prefs[Keys.JOYSTICK_X] = next.hudOffsets.joystickOffsetX
            prefs[Keys.JOYSTICK_Y] = next.hudOffsets.joystickOffsetY
            prefs[Keys.FIRE_X] = next.hudOffsets.fireOffsetX
            prefs[Keys.FIRE_Y] = next.hudOffsets.fireOffsetY
            prefs[Keys.ADS_X] = next.hudOffsets.adsOffsetX
            prefs[Keys.ADS_Y] = next.hudOffsets.adsOffsetY
            prefs[Keys.RELOAD_X] = next.hudOffsets.reloadOffsetX
            prefs[Keys.RELOAD_Y] = next.hudOffsets.reloadOffsetY
            prefs[Keys.JUMP_X] = next.hudOffsets.jumpOffsetX
            prefs[Keys.JUMP_Y] = next.hudOffsets.jumpOffsetY
            prefs[Keys.CROUCH_X] = next.hudOffsets.crouchOffsetX
            prefs[Keys.CROUCH_Y] = next.hudOffsets.crouchOffsetY
            prefs[Keys.INTERACT_X] = next.hudOffsets.interactOffsetX
            prefs[Keys.INTERACT_Y] = next.hudOffsets.interactOffsetY
            prefs[Keys.SWAP_X] = next.hudOffsets.swapOffsetX
            prefs[Keys.SWAP_Y] = next.hudOffsets.swapOffsetY
            prefs[Keys.AUTO_SPRINT] = next.autoSprint
            prefs[Keys.AUTO_RELOAD] = next.autoReload
            prefs[Keys.AUTO_SWITCH] = next.autoSwitchWeapon
            prefs[Keys.AIM_ASSIST] = next.aimAssistEnabled
            prefs[Keys.AIM_ASSIST_STRENGTH] = next.aimAssistStrength
            prefs[Keys.AUTO_PICKUP] = next.autoPickup
            prefs[Keys.AUTO_DOORS] = next.autoOpenDoors
            prefs[Keys.AUTO_ADS_FIRE] = next.autoAdsOnFire
            prefs[Keys.MASTER_VOL] = next.masterVolume
            prefs[Keys.MUSIC_VOL] = next.musicVolume
            prefs[Keys.SFX_VOL] = next.sfxVolume
            prefs[Keys.UI_VOL] = next.uiVolume
            prefs[Keys.BOT_DIFFICULTY] = next.botDifficulty.name
            prefs[Keys.BOT_COUNT] = next.botCount
            prefs[Keys.SELECTED_MAP] = next.selectedMap.name
            prefs[Keys.SELECTED_MODE] = next.selectedMode.name
            prefs[Keys.PRIMARY_WEAPON] = next.primaryWeapon.name
            prefs[Keys.SECONDARY_WEAPON] = next.secondaryWeapon.name
            for (wId in WeaponId.entries) {
                prefs[Keys.skinKey(wId)] = (next.weaponSkins[wId] ?: WeaponSkin.FACTORY_STOCK).name
            }
        }
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(raw: String?, default: T): T {
        if (raw == null) return default
        return runCatching { enumValueOf<T>(raw) }.getOrDefault(default)
    }
}
