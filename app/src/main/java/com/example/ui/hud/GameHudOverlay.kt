package com.example.ui.hud

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.DoorSliding
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.game.engine.GameSimulation
import com.example.game.model.GameModeType
import com.example.game.model.GameSettings
import com.example.game.model.HudState
import com.example.game.model.MatchResultSummary
import com.example.game.model.RankSystem
import com.example.game.model.WeaponCatalog
import com.example.game.model.WeaponId
import com.example.ui.components.RankEmblemCanvas
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlphaBlue
import com.example.ui.theme.BravoRed
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelSurface
import com.example.ui.theme.StrikeAmber
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GameHudOverlay(
    hud: HudState,
    settings: GameSettings,
    sim: GameSimulation,
    onPauseToggle: (Boolean) -> Unit,
    onRestart: () -> Unit,
    onExitToMenu: () -> Unit,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit
) {
    BackHandler {
        if (hud.matchResult == null) {
            onPauseToggle(!hud.isPaused)
        }
    }

    var showInlineSettings by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Sniper Scope Vignette Overlay (underneath HUD buttons)
        if (hud.isAds && hud.activeWeaponId == WeaponId.SNIPER_AWP && !hud.isDead) {
            SniperScopeOverlay()
        }

        // 2. Damage Red Vignette Flash
        if (hud.damageFlashAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, AlertCrimson.copy(alpha = hud.damageFlashAlpha * 0.55f))
                        )
                    )
            )
        }

        // 3. Right-side Camera Look Swipe Zone (Independent multi-touch)
        if (!hud.isPaused && hud.matchResult == null && !hud.isDead) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            var activeLookPointer: PointerId? = null
                            var lastPos = Offset.Zero
                            while (true) {
                                val event = awaitPointerEvent()
                                for (change in event.changes) {
                                    if (activeLookPointer == null && change.pressed && !change.previousPressed) {
                                        if (change.position.x > size.width * 0.34f) {
                                            activeLookPointer = change.id
                                            lastPos = change.position
                                            change.consume()
                                        }
                                    } else if (change.id == activeLookPointer) {
                                        if (change.pressed) {
                                            val delta = change.position - lastPos
                                            lastPos = change.position
                                            sim.addLookDelta(delta.x, delta.y)
                                            change.consume()
                                        } else {
                                            activeLookPointer = null
                                        }
                                    }
                                }
                            }
                        }
                    }
            )
        }

        // 4. Center Dynamic Crosshair & Hitmarkers
        if (!hud.isDead && !hud.isPaused && hud.matchResult == null) {
            CrosshairCenter(
                spreadPx = hud.crosshairSpreadPx,
                hitmarkerAlpha = hud.hitmarkerAlpha,
                isHeadshot = hud.hitmarkerHeadshot,
                isAds = hud.isAds && hud.activeWeaponId != WeaponId.SNIPER_AWP
            )
        }

        // 5. Safe-Area Tactical HUD Layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            // Top Left: Tactical Minimap + FPS
            Row(
                modifier = Modifier.align(Alignment.TopStart),
                verticalAlignment = Alignment.Top
            ) {
                TacticalMinimap(hud = hud)
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ObsidianBg.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, TacticalBorder)
                ) {
                    Text(
                        text = "${hud.fpsEstimate} FPS",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldSuccess,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            // Top Center: Match Scoreboard & Timer
            TopMatchScoreboard(hud = hud, modifier = Modifier.align(Alignment.TopCenter))

            // Top Right: Pause Button + Kill Feed
            Column(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onPauseToggle(true) },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SteelSurface.copy(alpha = 0.78f))
                            .border(1.dp, TacticalBorder, RoundedCornerShape(8.dp))
                            .testTag("hud_pause_button")
                    ) {
                        Icon(Icons.Filled.Pause, contentDescription = "Пауза", tint = TextPrimary)
                    }
                }
                Spacer(Modifier.height(6.dp))
                for (entry in hud.killFeed) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = ObsidianBg.copy(alpha = 0.68f),
                        modifier = Modifier.padding(bottom = 3.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
                            Text(
                                text = entry.killerName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.killerIsPlayer) StrikeAmber else TacticalCyan
                            )
                            Text(
                                text = if (entry.isHeadshot) " [HEAD] " else " • ",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.isHeadshot) AlertCrimson else TextSecondary
                            )
                            Text(
                                text = entry.victimName,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (entry.victimIsPlayer) AlertCrimson else TextPrimary
                            )
                        }
                    }
                }
            }

            // Center-Top Target Nameplate / Pickup Toast / Reload Bar
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 110.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (hud.targetedBotName != null) {
                    Text(hud.targetedBotName, style = MaterialTheme.typography.labelSmall, color = BravoRed)
                    LinearProgressIndicator(
                        progress = { hud.targetedBotHpRatio },
                        modifier = Modifier.width(86.dp).height(4.dp),
                        color = BravoRed,
                        trackColor = ObsidianBg.copy(alpha = 0.7f)
                    )
                }
                if (hud.pickupNotification != null) {
                    Spacer(Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = EmeraldSuccess.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, EmeraldSuccess)
                    ) {
                        Text(
                            text = hud.pickupNotification,
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldSuccess,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (hud.isReloading) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(top = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("ПЕРЕЗАРЯДКА...", style = MaterialTheme.typography.labelSmall, color = StrikeAmber)
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { hud.reloadProgress },
                        modifier = Modifier.width(110.dp).height(5.dp).clip(RoundedCornerShape(3.dp)),
                        color = StrikeAmber,
                        trackColor = ObsidianBg.copy(alpha = 0.7f)
                    )
                }
            }

            // Bottom Center: Health, Armor & Active Weapon Readout
            BottomVitalsAndAmmoBar(
                hud = hud,
                modifier = Modifier.align(Alignment.BottomCenter)
            )

            // Virtual Left Joystick & Right Touch Buttons (only while alive & unpaused)
            if (!hud.isPaused && hud.matchResult == null && !hud.isDead) {
                val scale = settings.buttonScale
                val opacity = settings.hudOpacity
                val off = settings.hudOffsets

                // Left Virtual Joystick
                VirtualJoystick(
                    sizeDp = (128f * scale).toInt(),
                    opacity = opacity,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 24.dp, bottom = 18.dp)
                        .offset { IntOffset(off.joystickOffsetX.roundToInt(), off.joystickOffsetY.roundToInt()) },
                    onVectorChanged = { x, y ->
                        sim.moveInputX = x
                        sim.moveInputY = y
                    }
                )

                // Fire Button (Hold to Shoot)
                HoldActionCircleButton(
                    label = "ОГОНЬ",
                    icon = Icons.Filled.GpsFixed,
                    sizeDp = (84f * scale).toInt(),
                    opacity = opacity,
                    accent = StrikeAmber,
                    testTag = "hud_fire_button",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 20.dp, bottom = 22.dp)
                        .offset { IntOffset(off.fireOffsetX.roundToInt(), off.fireOffsetY.roundToInt()) },
                    onPressChanged = { sim.setFiring(it) }
                )

                // ADS Aim Button (Hold or toggle)
                HoldActionCircleButton(
                    label = "ПРИЦЕЛ",
                    icon = Icons.Filled.CenterFocusStrong,
                    sizeDp = (62f * scale).toInt(),
                    opacity = opacity,
                    accent = if (hud.isAds) StrikeAmber else TacticalCyan,
                    testTag = "hud_ads_button",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 118.dp, bottom = 84.dp)
                        .offset { IntOffset(off.adsOffsetX.roundToInt(), off.adsOffsetY.roundToInt()) },
                    onPressChanged = { sim.setAds(it) }
                )

                // Reload Button
                TapActionCircleButton(
                    icon = Icons.Filled.Refresh,
                    contentDesc = "Перезарядка",
                    sizeDp = (56f * scale).toInt(),
                    opacity = opacity,
                    accent = TextPrimary,
                    testTag = "hud_reload_button",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 120.dp, bottom = 16.dp)
                        .offset { IntOffset(off.reloadOffsetX.roundToInt(), off.reloadOffsetY.roundToInt()) },
                    onClick = { sim.requestReload() }
                )

                // Jump Button
                TapActionCircleButton(
                    icon = Icons.Filled.ArrowUpward,
                    contentDesc = "Прыжок",
                    sizeDp = (54f * scale).toInt(),
                    opacity = opacity,
                    accent = TextPrimary,
                    testTag = "hud_jump_button",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 22.dp, bottom = 122.dp)
                        .offset { IntOffset(off.jumpOffsetX.roundToInt(), off.jumpOffsetY.roundToInt()) },
                    onClick = { sim.requestJump() }
                )

                // Crouch Toggle Button
                TapActionCircleButton(
                    icon = Icons.Filled.Compress,
                    contentDesc = "Присесть",
                    sizeDp = (52f * scale).toInt(),
                    opacity = opacity,
                    accent = if (hud.isCrouching) StrikeAmber else TextPrimary,
                    testTag = "hud_crouch_button",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 90.dp, bottom = 156.dp)
                        .offset { IntOffset(off.crouchOffsetX.roundToInt(), off.crouchOffsetY.roundToInt()) },
                    onClick = { sim.toggleCrouch() }
                )

                // Weapon Swap Button
                TapActionCircleButton(
                    icon = Icons.Filled.SwapHoriz,
                    contentDesc = "Смена оружия",
                    sizeDp = (52f * scale).toInt(),
                    opacity = opacity,
                    accent = StrikeAmber,
                    testTag = "hud_swap_button",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 186.dp, bottom = 16.dp)
                        .offset { IntOffset(off.swapOffsetX.roundToInt(), off.swapOffsetY.roundToInt()) },
                    onClick = { sim.requestSwapWeapon() }
                )

                // Interact / Door Button (visible when near door)
                if (hud.nearDoorPrompt != null) {
                    Button(
                        onClick = { sim.requestInteract() },
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 135.dp)
                            .height(48.dp)
                            .testTag("hud_interact_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyan, contentColor = ObsidianBg)
                    ) {
                        Icon(Icons.Filled.DoorSliding, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(hud.nearDoorPrompt, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        // 6. Respawn Banner
        if (hud.isDead && hud.matchResult == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ObsidianBg.copy(alpha = 0.62f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ВЫ ВЫБЫЛИ ИЗ СТРОЯ", style = MaterialTheme.typography.headlineLarge, color = AlertCrimson)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (hud.mapMode == GameModeType.ELIMINATION ||
                            hud.mapMode == GameModeType.RANKED_2V2 ||
                            hud.mapMode == GameModeType.SURVIVAL
                        ) {
                            "Ожидание окончания раунда..."
                        } else {
                            "Возрождение через ${String.format("%.1f", hud.respawnSecondsLeft)}с"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
            }
        }

        // 7. Pause Overlay
        if (hud.isPaused && hud.matchResult == null) {
            PauseModalOverlay(
                settings = settings,
                showSettings = showInlineSettings,
                onToggleSettings = { showInlineSettings = it },
                onUpdateSettings = onUpdateSettings,
                onResume = {
                    showInlineSettings = false
                    onPauseToggle(false)
                },
                onRestart = {
                    showInlineSettings = false
                    onRestart()
                },
                onExit = onExitToMenu
            )
        }

        // 8. Match Result Screen
        if (hud.matchResult != null) {
            MatchResultOverlay(
                summary = hud.matchResult,
                onPlayAgain = onRestart,
                onMainMenu = onExitToMenu
            )
        }
    }
}

@Composable
private fun VirtualJoystick(
    sizeDp: Int,
    opacity: Float,
    modifier: Modifier = Modifier,
    onVectorChanged: (Float, Float) -> Unit
) {
    var knobOffset by remember { mutableStateOf(Offset.Zero) }
    var activePointer by remember { mutableStateOf<PointerId?>(null) }

    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .alpha(opacity)
            .pointerInput(sizeDp) {
                val maxRadius = (size.width * 0.42f).coerceAtLeast(24f)
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        for (change in event.changes) {
                            if (activePointer == null && change.pressed && !change.previousPressed) {
                                activePointer = change.id
                                val center = Offset(size.width * 0.5f, size.height * 0.5f)
                                val delta = change.position - center
                                val dist = hypot(delta.x, delta.y)
                                val clamped = if (dist > maxRadius) delta * (maxRadius / dist) else delta
                                knobOffset = clamped
                                onVectorChanged(clamped.x / maxRadius, -clamped.y / maxRadius)
                                change.consume()
                            } else if (change.id == activePointer) {
                                if (change.pressed) {
                                    val center = Offset(size.width * 0.5f, size.height * 0.5f)
                                    val delta = change.position - center
                                    val dist = hypot(delta.x, delta.y)
                                    val clamped = if (dist > maxRadius) delta * (maxRadius / dist) else delta
                                    knobOffset = clamped
                                    onVectorChanged(clamped.x / maxRadius, -clamped.y / maxRadius)
                                    change.consume()
                                } else {
                                    activePointer = null
                                    knobOffset = Offset.Zero
                                    onVectorChanged(0f, 0f)
                                }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = SteelSurface.copy(alpha = 0.55f),
                radius = size.minDimension * 0.48f
            )
            drawCircle(
                color = TacticalCyan.copy(alpha = 0.45f),
                radius = size.minDimension * 0.48f,
                style = Stroke(width = 2.dp.toPx())
            )
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            drawCircle(
                color = StrikeAmber.copy(alpha = 0.85f),
                radius = size.minDimension * 0.21f,
                center = center + knobOffset
            )
        }
    }
}

@Composable
private fun HoldActionCircleButton(
    label: String,
    icon: ImageVector,
    sizeDp: Int,
    opacity: Float,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onPressChanged: (Boolean) -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .alpha(opacity)
            .clip(CircleShape)
            .background(if (pressed) accent.copy(alpha = 0.45f) else SteelSurface.copy(alpha = 0.72f))
            .border(2.dp, accent, CircleShape)
            .testTag(testTag)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    onPressChanged(true)
                    do {
                        val event = awaitPointerEvent()
                    } while (event.changes.any { it.pressed })
                    pressed = false
                    onPressChanged(false)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size((sizeDp * 0.36f).dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

@Composable
private fun TapActionCircleButton(
    icon: ImageVector,
    contentDesc: String,
    sizeDp: Int,
    opacity: Float,
    accent: Color,
    testTag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .size(sizeDp.coerceAtLeast(48).dp)
            .alpha(opacity)
            .testTag(testTag),
        shape = CircleShape,
        color = SteelSurface.copy(alpha = 0.72f),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.75f))
    ) {
        Box(
            modifier = Modifier.fillMaxSize().clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = contentDesc, tint = accent, modifier = Modifier.size((sizeDp * 0.44f).dp))
        }
    }
}

@Composable
private fun TopMatchScoreboard(hud: HudState, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = ObsidianBg.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, TacticalBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (hud.mapMode) {
                GameModeType.RANKED_2V2 -> {
                    val mins = hud.matchTimeRemainingSec / 60
                    val secs = hud.matchTimeRemainingSec % 60
                    val alphaDots = "●".repeat(hud.aliveAlpha.coerceIn(0, 2)) + "○".repeat((2 - hud.aliveAlpha).coerceIn(0, 2))
                    val bravoDots = "●".repeat(hud.aliveBravo.coerceIn(0, 2)) + "○".repeat((2 - hud.aliveBravo).coerceIn(0, 2))
                    Text("ALPHA $alphaDots ${hud.scoreAlpha}", style = MaterialTheme.typography.labelLarge, color = AlphaBlue)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("2x2 НА ЗВАНИЕ", style = MaterialTheme.typography.labelSmall, color = StrikeAmber)
                        Text("R${hud.roundNumber}/${hud.totalRounds} • %02d:%02d".format(mins, secs), style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                    Text("${hud.scoreBravo} $bravoDots BRAVO", style = MaterialTheme.typography.labelLarge, color = BravoRed)
                }

                GameModeType.TEAM_DEATHMATCH, GameModeType.ELIMINATION -> {
                    Text("ALPHA ${hud.scoreAlpha}", style = MaterialTheme.typography.labelLarge, color = AlphaBlue)
                    val mins = hud.matchTimeRemainingSec / 60
                    val secs = hud.matchTimeRemainingSec % 60
                    Text(
                        text = if (hud.mapMode == GameModeType.ELIMINATION) "R${hud.roundNumber}/${hud.totalRounds} • %02d:%02d".format(mins, secs) else "%02d:%02d".format(mins, secs),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Text("BRAVO ${hud.scoreBravo}", style = MaterialTheme.typography.labelLarge, color = BravoRed)
                }

                GameModeType.FREE_FOR_ALL -> {
                    val mins = hud.matchTimeRemainingSec / 60
                    val secs = hud.matchTimeRemainingSec % 60
                    Text("ФРАГИ ${hud.playerKills}/20", style = MaterialTheme.typography.labelLarge, color = StrikeAmber)
                    Text("%02d:%02d".format(mins, secs), style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    Text("СМЕРТИ ${hud.playerDeaths}", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                }

                GameModeType.SURVIVAL -> {
                    Text("ВОЛНА ${hud.waveNumber}", style = MaterialTheme.typography.labelLarge, color = StrikeAmber)
                    Text("ВРАГОВ: ${hud.aliveBravo}", style = MaterialTheme.typography.labelLarge, color = BravoRed)
                    Text("УБ: ${hud.playerKills}", style = MaterialTheme.typography.labelLarge, color = EmeraldSuccess)
                }

                GameModeType.PRACTICE -> {
                    Text("ПОЛИГОН", style = MaterialTheme.typography.labelLarge, color = TacticalCyan)
                    Text("ПОПАДАНИЯ: ${hud.playerKills}", style = MaterialTheme.typography.labelLarge, color = EmeraldSuccess)
                }
            }
        }
    }
}

@Composable
private fun BottomVitalsAndAmmoBar(hud: HudState, modifier: Modifier = Modifier) {
    val wSpec = WeaponCatalog.byId(hud.activeWeaponId)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = ObsidianBg.copy(alpha = 0.76f),
        border = BorderStroke(1.dp, TacticalBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.width(120.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("HP ${hud.health}", style = MaterialTheme.typography.labelLarge, color = if (hud.health <= 30) AlertCrimson else EmeraldSuccess)
                    Text("БР ${hud.armor}", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                }
                Spacer(Modifier.height(3.dp))
                LinearProgressIndicator(
                    progress = { hud.health / hud.maxHealth.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = if (hud.health <= 30) AlertCrimson else EmeraldSuccess,
                    trackColor = GunmetalCard
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("${wSpec.name} • ${hud.activeWeaponSkin.displayName}", style = MaterialTheme.typography.labelSmall, color = StrikeAmber)
                Text("${hud.ammoInMag} / ${hud.ammoReserve}", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            }
        }
    }
}

@Composable
private fun TacticalMinimap(hud: HudState) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = ObsidianBg.copy(alpha = 0.78f),
        border = BorderStroke(1.dp, TacticalBorder),
        modifier = Modifier.size(98.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val half = size.minDimension * 0.5f
            val scale = half / 30f
            val rad = (-hud.playerYawDeg) * (Math.PI / 180.0).toFloat()
            val c = cos(rad)
            val s = sin(rad)

            // Draw walls relative to player and rotated into camera-up space
            for (w in hud.minimapWalls) {
                val wx = ((w.minX + w.maxX) * 0.5f - hud.playerX)
                val wz = ((w.minZ + w.maxZ) * 0.5f - hud.playerZ)
                val rx = wx * c - wz * s
                val rz = wx * s + wz * c
                val px = half + rx * scale
                val py = half + rz * scale
                val sw = ((w.maxX - w.minX) * scale).coerceAtLeast(2f)
                val sh = ((w.maxZ - w.minZ) * scale).coerceAtLeast(2f)
                if (px in -10f..(size.width + 10f) && py in -10f..(size.height + 10f)) {
                    drawRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(px - sw * 0.5f, py - sh * 0.5f),
                        size = Size(sw, sh)
                    )
                }
            }

            // Blips
            for (b in hud.minimapBlips) {
                val rx = b.relX * c - b.relZ * s
                val rz = b.relX * s + b.relZ * c
                val px = (half + rx * scale).coerceIn(4f, size.width - 4f)
                val py = (half + rz * scale).coerceIn(4f, size.height - 4f)
                val col = when {
                    b.isPickup -> EmeraldSuccess
                    b.isEnemy -> BravoRed
                    else -> AlphaBlue
                }
                drawCircle(color = col, radius = if (b.isPickup) 2.5f else 3.8f, center = Offset(px, py))
            }

            // Player center arrow
            val tri = Path().apply {
                moveTo(half, half - 5f)
                lineTo(half - 4f, half + 4f)
                lineTo(half + 4f, half + 4f)
                close()
            }
            drawPath(tri, color = StrikeAmber)
        }
    }
}

@Composable
private fun CrosshairCenter(
    spreadPx: Float,
    hitmarkerAlpha: Float,
    isHeadshot: Boolean,
    isAds: Boolean
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val gap = spreadPx.coerceIn(6f, 48f)
        val len = if (isAds) 7f else 11f
        val col = if (isAds) StrikeAmber else Color.White.copy(alpha = 0.88f)
        val stroke = 2.2f

        drawCircle(color = col, radius = 2.2f, center = Offset(cx, cy))
        drawLine(col, Offset(cx - gap - len, cy), Offset(cx - gap, cy), strokeWidth = stroke)
        drawLine(col, Offset(cx + gap, cy), Offset(cx + gap + len, cy), strokeWidth = stroke)
        drawLine(col, Offset(cx, cy - gap - len), Offset(cx, cy - gap), strokeWidth = stroke)
        drawLine(col, Offset(cx, cy + gap), Offset(cx, cy + gap + len), strokeWidth = stroke)

        if (hitmarkerAlpha > 0.02f) {
            val hmCol = (if (isHeadshot) AlertCrimson else StrikeAmber).copy(alpha = hitmarkerAlpha)
            val d1 = 7f
            val d2 = 16f
            drawLine(hmCol, Offset(cx - d2, cy - d2), Offset(cx - d1, cy - d1), strokeWidth = 3f)
            drawLine(hmCol, Offset(cx + d1, cy - d1), Offset(cx + d2, cy - d2), strokeWidth = 3f)
            drawLine(hmCol, Offset(cx - d2, cy + d2), Offset(cx - d1, cy + d1), strokeWidth = 3f)
            drawLine(hmCol, Offset(cx + d1, cy + d1), Offset(cx + d2, cy + d2), strokeWidth = 3f)
        }
    }
}

@Composable
private fun SniperScopeOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val radius = size.minDimension * 0.38f
        drawRect(color = Color.Black.copy(alpha = 0.92f), topLeft = Offset.Zero, size = Size(cx - radius, size.height))
        drawRect(color = Color.Black.copy(alpha = 0.92f), topLeft = Offset(cx + radius, 0f), size = Size(size.width - (cx + radius), size.height))
        drawCircle(color = Color.Black, radius = radius * 1.45f, style = Stroke(width = radius * 0.92f))
        drawCircle(color = TacticalCyan.copy(alpha = 0.45f), radius = radius, style = Stroke(width = 2.5f))
        drawLine(Color.Black, Offset(cx - radius, cy), Offset(cx + radius, cy), strokeWidth = 2f)
        drawLine(Color.Black, Offset(cx, cy - radius), Offset(cx, cy + radius), strokeWidth = 2f)
        drawCircle(AlertCrimson, radius = 3.5f, center = Offset(cx, cy))
    }
}

@Composable
private fun PauseModalOverlay(
    settings: GameSettings,
    showSettings: Boolean,
    onToggleSettings: (Boolean) -> Unit,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SteelSurface,
            border = BorderStroke(1.dp, TacticalBorder),
            modifier = Modifier.width(360.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(if (showSettings) "БЫСТРЫЕ НАСТРОЙКИ" else "ПАУЗА", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)

                if (showSettings) {
                    Text("Чувствительность: ${String.format("%.2f", settings.cameraSensitivity)}x", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                    Slider(
                        value = settings.cameraSensitivity,
                        onValueChange = { v -> onUpdateSettings { it.copy(cameraSensitivity = v) } },
                        valueRange = 0.3f..2.5f,
                        colors = SliderDefaults.colors(thumbColor = StrikeAmber, activeTrackColor = StrikeAmber)
                    )
                    Text("Громкость SFX: ${(settings.sfxVolume * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                    Slider(
                        value = settings.sfxVolume,
                        onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = StrikeAmber, activeTrackColor = StrikeAmber)
                    )
                    Button(
                        onClick = { onToggleSettings(false) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StrikeAmber, contentColor = ObsidianBg)
                    ) {
                        Text("НАЗАД")
                    }
                } else {
                    Button(
                        onClick = onResume,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_resume_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = StrikeAmber, contentColor = ObsidianBg)
                    ) { Text("ПРОДОЛЖИТЬ", style = MaterialTheme.typography.titleMedium) }

                    OutlinedButton(
                        onClick = { onToggleSettings(true) },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        border = BorderStroke(1.dp, TacticalBorder)
                    ) { Text("НАСТРОЙКИ", color = TextPrimary) }

                    OutlinedButton(
                        onClick = onRestart,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        border = BorderStroke(1.dp, TacticalBorder)
                    ) { Text("ПЕРЕЗАПУСТИТЬ", color = TextPrimary) }

                    OutlinedButton(
                        onClick = onExit,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pause_exit_btn"),
                        border = BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.6f))
                    ) { Text("В ГЛАВНОЕ МЕНЮ", color = AlertCrimson) }
                }
            }
        }
    }
}

@Composable
private fun MatchResultOverlay(
    summary: MatchResultSummary,
    onPlayAgain: () -> Unit,
    onMainMenu: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.86f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SteelSurface,
            border = BorderStroke(1.5.dp, if (summary.victory) EmeraldSuccess else AlertCrimson),
            modifier = Modifier.width(460.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = summary.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = if (summary.victory) EmeraldSuccess else AlertCrimson
                )
                Text(summary.subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)

                if (summary.mode == GameModeType.RANKED_2V2) {
                    val beforeProg = RankSystem.progressForRp(summary.rpBefore)
                    val afterProg = RankSystem.progressForRp(summary.rpAfter)
                    val tierColor = Color(afterProg.tier.primaryHex)
                    val deltaSign = if (summary.rankDelta >= 0) "+" else ""
                    val statusTag = when {
                        afterProg.tier.ordinal > beforeProg.tier.ordinal -> "ПОВЫШЕНИЕ РАНГА!"
                        afterProg.tier.ordinal < beforeProg.tier.ordinal -> "ПОНИЖЕНИЕ РАНГА"
                        else -> "РАНГОВЫЙ РЕЙТИНГ"
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = GunmetalCard,
                        border = BorderStroke(1.dp, tierColor.copy(alpha = 0.7f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RankEmblemCanvas(tier = afterProg.tier, sizeDp = 42.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(statusTag, style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                                    Text(
                                        text = "$deltaSign${summary.rankDelta} RP",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (summary.rankDelta >= 0) EmeraldSuccess else AlertCrimson
                                    )
                                }
                                Text("${afterProg.fullTitle} • ${summary.rpAfter} RP", style = MaterialTheme.typography.titleMedium, color = tierColor)
                                Spacer(Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { afterProg.progressInTier.coerceIn(0.05f, 1f) },
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                    color = tierColor,
                                    trackColor = ObsidianBg
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ResultMetric("УСТРАНЕНИЯ", "${summary.playerKills}", StrikeAmber)
                    ResultMetric("СМЕРТИ", "${summary.playerDeaths}", TextPrimary)
                    ResultMetric("ТОЧНОСТЬ", String.format("%.0f%%", summary.accuracyPercent), TacticalCyan)
                    ResultMetric("ВРЕМЯ", "${summary.durationSec}с", TextSecondary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onPlayAgain,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("result_play_again"),
                        colors = ButtonDefaults.buttonColors(containerColor = StrikeAmber, contentColor = ObsidianBg)
                    ) { Text("ИГРАТЬ СНОВА") }

                    OutlinedButton(
                        onClick = onMainMenu,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("result_main_menu"),
                        border = BorderStroke(1.dp, TacticalBorder)
                    ) { Text("ГЛАВНОЕ МЕНЮ", color = TextPrimary) }
                }
            }
        }
    }
}

@Composable
private fun ResultMetric(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = color)
    }
}
