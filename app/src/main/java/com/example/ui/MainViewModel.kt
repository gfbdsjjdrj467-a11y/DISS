package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StrikeArenaApp
import com.example.data.CareerStatsEntity
import com.example.data.MatchRecordEntity
import com.example.game.engine.GameSimulation
import com.example.game.model.BotDifficulty
import com.example.game.model.GameModeType
import com.example.game.model.GameSettings
import com.example.game.model.GraphicsQuality
import com.example.game.model.HudControlOffsets
import com.example.game.model.HudState
import com.example.game.model.MapId
import com.example.game.model.MatchResultSummary
import com.example.game.model.WeaponId
import com.example.game.model.WeaponSkin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppRoute {
    MAIN_MENU,
    MAP_MODE_SELECT,
    ARSENAL,
    SETTINGS,
    CONTROLS_EDITOR,
    STATS,
    IN_GAME
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as StrikeArenaApp
    val soundEngine = app.soundEngine
    private val settingsRepo = app.settingsRepository
    private val statsRepo = app.statsRepository

    val settings: StateFlow<GameSettings> = settingsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, GameSettings())

    val careerStats: StateFlow<CareerStatsEntity> = statsRepo.careerStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CareerStatsEntity())

    val matchHistory: StateFlow<List<MatchRecordEntity>> = statsRepo.matchHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _route = MutableStateFlow(AppRoute.MAIN_MENU)
    val route: StateFlow<AppRoute> = _route.asStateFlow()

    private val _hudState = MutableStateFlow(HudState())
    val hudState: StateFlow<HudState> = _hudState.asStateFlow()

    private val recordedResultIds = HashSet<Int>()

    val simulation: GameSimulation = GameSimulation(
        soundEngine = soundEngine,
        onMatchFinished = { summary ->
            viewModelScope.launch {
                val sig = summary.hashCode()
                if (recordedResultIds.add(sig)) {
                    statsRepo.recordMatchResult(summary)
                }
            }
        }
    )

    init {
        soundEngine.startMenuMusic()
        viewModelScope.launch {
            settings.collect { s ->
                soundEngine.applySettings(s)
                simulation.settings = s
                if (_route.value == AppRoute.MAIN_MENU && simulation.currentMap.id != s.selectedMap) {
                    simulation.startShowcase(s)
                }
            }
        }
        viewModelScope.launch {
            careerStats.collect { stats ->
                simulation.currentPlayerRp = stats.rankPoints
            }
        }
    }

    fun onHudSnapshot(snapshot: HudState) {
        _hudState.value = snapshot
    }

    fun navigateTo(target: AppRoute) {
        soundEngine.playUiClick()
        _route.value = target
        val inMatch = target == AppRoute.IN_GAME
        soundEngine.setMenuMusicPaused(inMatch)
    }

    fun startMatch() {
        soundEngine.playUiClick()
        recordedResultIds.clear()
        simulation.currentPlayerRp = careerStats.value.rankPoints
        simulation.startMatch(settings.value)
        _hudState.value = simulation.snapshotHud(60)
        _route.value = AppRoute.IN_GAME
        soundEngine.setMenuMusicPaused(true)
    }

    fun startRanked2v2Match() {
        soundEngine.playUiClick()
        val rankedSettings = settings.value.copy(selectedMode = GameModeType.RANKED_2V2)
        updateSettings { it.copy(selectedMode = GameModeType.RANKED_2V2) }
        recordedResultIds.clear()
        simulation.currentPlayerRp = careerStats.value.rankPoints
        simulation.startMatch(rankedSettings)
        _hudState.value = simulation.snapshotHud(60)
        _route.value = AppRoute.IN_GAME
        soundEngine.setMenuMusicPaused(true)
    }

    fun restartMatch() {
        soundEngine.playUiClick()
        recordedResultIds.clear()
        simulation.currentPlayerRp = careerStats.value.rankPoints
        simulation.startMatch(settings.value)
        _hudState.value = simulation.snapshotHud(60)
    }

    fun togglePause(paused: Boolean) {
        soundEngine.playUiClick()
        simulation.isPaused = paused
        _hudState.value = simulation.snapshotHud(60)
    }

    fun exitMatchToMenu() {
        soundEngine.playUiClick()
        simulation.startShowcase(settings.value)
        _route.value = AppRoute.MAIN_MENU
        soundEngine.setMenuMusicPaused(false)
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        viewModelScope.launch {
            settingsRepo.updateSettings(transform)
        }
    }

    fun selectMap(mapId: MapId) = updateSettings { it.copy(selectedMap = mapId) }
    fun selectMode(mode: GameModeType) = updateSettings { it.copy(selectedMode = mode) }
    fun selectDifficulty(diff: BotDifficulty) = updateSettings { it.copy(botDifficulty = diff) }
    fun setBotCount(count: Int) = updateSettings { it.copy(botCount = count.coerceIn(2, 12)) }
    fun equipPrimaryWeapon(id: WeaponId) = updateSettings { it.copy(primaryWeapon = id) }
    fun equipSecondaryWeapon(id: WeaponId) = updateSettings { it.copy(secondaryWeapon = id) }
    fun equipWeaponSkin(id: WeaponId, skin: WeaponSkin) = updateSettings { s ->
        s.copy(weaponSkins = s.weaponSkins + (id to skin))
    }

    fun applyGraphicsPreset(q: GraphicsQuality) = updateSettings {
        when (q) {
            GraphicsQuality.LOW -> it.copy(graphicsQuality = q, textureQuality = 0, shadowQuality = 0, effectsQuality = 0, fpsLimit = 30)
            GraphicsQuality.MEDIUM -> it.copy(graphicsQuality = q, textureQuality = 1, shadowQuality = 1, effectsQuality = 1, fpsLimit = 60)
            GraphicsQuality.HIGH -> it.copy(graphicsQuality = q, textureQuality = 2, shadowQuality = 2, effectsQuality = 2, fpsLimit = 60)
        }
    }

    fun saveHudOffsets(offsets: HudControlOffsets) = updateSettings { it.copy(hudOffsets = offsets) }
    fun resetHudOffsets() = updateSettings { it.copy(hudOffsets = HudControlOffsets()) }

    fun resetAllStats() {
        soundEngine.playUiClick()
        viewModelScope.launch {
            statsRepo.resetAllStats()
        }
    }
}
