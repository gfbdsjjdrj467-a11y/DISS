package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.engine.FpsGLView
import com.example.ui.AppRoute
import com.example.ui.MainViewModel
import com.example.ui.hud.GameHudOverlay
import com.example.ui.screens.ArsenalScreen
import com.example.ui.screens.ControlsLayoutEditorScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.MapModeSelectScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.StrikeArenaTheme

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()
    private var glView: FpsGLView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureImmersiveMode()

        setContent {
            StrikeArenaTheme {
                StrikeArenaRoot(
                    viewModel = viewModel,
                    onGlViewCreated = { glView = it }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        glView?.onResume()
        configureImmersiveMode()
    }

    override fun onPause() {
        glView?.onPause()
        if (viewModel.route.value == AppRoute.IN_GAME && !viewModel.simulation.isPaused) {
            viewModel.togglePause(true)
        }
        super.onPause()
    }

    private fun configureImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

@Composable
private fun StrikeArenaRoot(
    viewModel: MainViewModel,
    onGlViewCreated: (FpsGLView) -> Unit
) {
    val route by viewModel.route.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val careerStats by viewModel.careerStats.collectAsStateWithLifecycle()
    val matchHistory by viewModel.matchHistory.collectAsStateWithLifecycle()
    val hudState by viewModel.hudState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        // Persistent hardware-accelerated 3D OpenGL ES viewport
        // Renders live tactical military diorama behind Main Menu and full 3D FPS gameplay during matches
        if (route == AppRoute.MAIN_MENU || route == AppRoute.IN_GAME) {
            var localView: FpsGLView? = null
            androidx.compose.runtime.DisposableEffect(Unit) {
                onDispose {
                    localView?.onPause()
                }
            }
            AndroidView(
                factory = { ctx ->
                    FpsGLView(ctx, viewModel.simulation) { snapshot ->
                        viewModel.onHudSnapshot(snapshot)
                    }.also {
                        localView = it
                        onGlViewCreated(it)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        when (route) {
            AppRoute.MAIN_MENU -> MainMenuScreen(
                settings = settings,
                careerStats = careerStats,
                onPlayClick = { viewModel.startMatch() },
                onNavigate = { viewModel.navigateTo(it) }
            )

            AppRoute.MAP_MODE_SELECT -> MapModeSelectScreen(
                settings = settings,
                onSelectMap = { viewModel.selectMap(it) },
                onSelectMode = { viewModel.selectMode(it) },
                onSelectDifficulty = { viewModel.selectDifficulty(it) },
                onSetBotCount = { viewModel.setBotCount(it) },
                onStartMatch = { viewModel.startMatch() },
                onBack = { viewModel.navigateTo(AppRoute.MAIN_MENU) }
            )

            AppRoute.ARSENAL -> ArsenalScreen(
                settings = settings,
                careerStats = careerStats,
                onEquipPrimary = { viewModel.equipPrimaryWeapon(it) },
                onEquipSecondary = { viewModel.equipSecondaryWeapon(it) },
                onBack = { viewModel.navigateTo(AppRoute.MAIN_MENU) }
            )

            AppRoute.SETTINGS -> SettingsScreen(
                settings = settings,
                onUpdateSettings = { viewModel.updateSettings(it) },
                onApplyGraphicsPreset = { viewModel.applyGraphicsPreset(it) },
                onOpenControlsEditor = { viewModel.navigateTo(AppRoute.CONTROLS_EDITOR) },
                onBack = { viewModel.navigateTo(AppRoute.MAIN_MENU) }
            )

            AppRoute.CONTROLS_EDITOR -> ControlsLayoutEditorScreen(
                settings = settings,
                onSave = { offsets, scale, opacity ->
                    viewModel.updateSettings {
                        it.copy(hudOffsets = offsets, buttonScale = scale, hudOpacity = opacity)
                    }
                },
                onBack = { viewModel.navigateTo(AppRoute.SETTINGS) }
            )

            AppRoute.STATS -> StatsScreen(
                career = careerStats,
                history = matchHistory,
                onResetStats = { viewModel.resetAllStats() },
                onBack = { viewModel.navigateTo(AppRoute.MAIN_MENU) }
            )

            AppRoute.IN_GAME -> GameHudOverlay(
                hud = hudState,
                settings = settings,
                sim = viewModel.simulation,
                onPauseToggle = { viewModel.togglePause(it) },
                onRestart = { viewModel.restartMatch() },
                onExitToMenu = { viewModel.exitMatchToMenu() },
                onUpdateSettings = { viewModel.updateSettings(it) }
            )
        }
    }
}
