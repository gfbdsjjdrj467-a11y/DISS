package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.game.model.GameSettings
import com.example.game.model.HudControlOffsets
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelSurface
import com.example.ui.theme.StrikeAmber
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import kotlin.math.roundToInt

@Composable
fun ControlsLayoutEditorScreen(
    settings: GameSettings,
    onSave: (HudControlOffsets, Float, Float) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var offsets by remember(settings) { mutableStateOf(settings.hudOffsets) }
    var scale by remember(settings) { mutableFloatStateOf(settings.buttonScale) }
    var opacity by remember(settings) { mutableFloatStateOf(settings.hudOpacity) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg.copy(alpha = 0.92f))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // Top Floating Toolbar
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .fillMaxWidth(0.88f),
            shape = RoundedCornerShape(10.dp),
            color = SteelSurface,
            border = BorderStroke(1.dp, TacticalBorder)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("ПЕРЕМЕСТИТЕ ЭЛЕМЕНТЫ", style = MaterialTheme.typography.labelLarge, color = TacticalCyan)

                Column(modifier = Modifier.weight(1f)) {
                    Text("Размер: ${(scale * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 0.7f..1.4f,
                        colors = SliderDefaults.colors(thumbColor = StrikeAmber, activeTrackColor = StrikeAmber)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text("Прозрачность: ${(opacity * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    Slider(
                        value = opacity,
                        onValueChange = { opacity = it },
                        valueRange = 0.3f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = StrikeAmber, activeTrackColor = StrikeAmber)
                    )
                }

                OutlinedButton(
                    onClick = {
                        offsets = HudControlOffsets()
                        scale = 1.0f
                        opacity = 0.85f
                    },
                    modifier = Modifier.height(48.dp).testTag("hud_editor_reset"),
                    border = BorderStroke(1.dp, TacticalBorder)
                ) {
                    Icon(Icons.Filled.RestartAlt, contentDescription = "Сброс", tint = TextPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text("СБРОС", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                }

                Button(
                    onClick = {
                        onSave(offsets, scale, opacity)
                        onBack()
                    },
                    modifier = Modifier.height(48.dp).testTag("hud_editor_save"),
                    colors = ButtonDefaults.buttonColors(containerColor = StrikeAmber, contentColor = ObsidianBg)
                ) {
                    Icon(Icons.Filled.Check, contentDescription = "Сохранить")
                    Spacer(Modifier.width(4.dp))
                    Text("СОХРАНИТЬ", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Draggable Virtual Joystick preview
        DraggableHudToken(
            label = "ДЖОЙСТИК",
            sizeDp = (128f * scale).toInt(),
            opacity = opacity,
            color = TacticalCyan,
            offsetX = offsets.joystickOffsetX,
            offsetY = offsets.joystickOffsetY,
            alignment = Alignment.BottomStart,
            basePaddingX = 36,
            basePaddingY = 28,
            onDrag = { dx, dy ->
                offsets = offsets.copy(joystickOffsetX = offsets.joystickOffsetX + dx, joystickOffsetY = offsets.joystickOffsetY + dy)
            }
        )

        // Draggable Right-side Buttons
        DraggableHudToken(
            label = "ОГОНЬ",
            sizeDp = (84f * scale).toInt(),
            opacity = opacity,
            color = StrikeAmber,
            offsetX = offsets.fireOffsetX,
            offsetY = offsets.fireOffsetY,
            alignment = Alignment.BottomEnd,
            basePaddingX = 34,
            basePaddingY = 34,
            onDrag = { dx, dy ->
                offsets = offsets.copy(fireOffsetX = offsets.fireOffsetX + dx, fireOffsetY = offsets.fireOffsetY + dy)
            }
        )

        DraggableHudToken(
            label = "ПРИЦЕЛ",
            sizeDp = (62f * scale).toInt(),
            opacity = opacity,
            color = TacticalCyan,
            offsetX = offsets.adsOffsetX,
            offsetY = offsets.adsOffsetY,
            alignment = Alignment.BottomEnd,
            basePaddingX = 136,
            basePaddingY = 98,
            onDrag = { dx, dy ->
                offsets = offsets.copy(adsOffsetX = offsets.adsOffsetX + dx, adsOffsetY = offsets.adsOffsetY + dy)
            }
        )

        DraggableHudToken(
            label = "ПЕРЕЗАР.",
            sizeDp = (58f * scale).toInt(),
            opacity = opacity,
            color = TextPrimary,
            offsetX = offsets.reloadOffsetX,
            offsetY = offsets.reloadOffsetY,
            alignment = Alignment.BottomEnd,
            basePaddingX = 140,
            basePaddingY = 26,
            onDrag = { dx, dy ->
                offsets = offsets.copy(reloadOffsetX = offsets.reloadOffsetX + dx, reloadOffsetY = offsets.reloadOffsetY + dy)
            }
        )

        DraggableHudToken(
            label = "ПРЫЖОК",
            sizeDp = (56f * scale).toInt(),
            opacity = opacity,
            color = TextPrimary,
            offsetX = offsets.jumpOffsetX,
            offsetY = offsets.jumpOffsetY,
            alignment = Alignment.BottomEnd,
            basePaddingX = 34,
            basePaddingY = 136,
            onDrag = { dx, dy ->
                offsets = offsets.copy(jumpOffsetX = offsets.jumpOffsetX + dx, jumpOffsetY = offsets.jumpOffsetY + dy)
            }
        )

        DraggableHudToken(
            label = "ПРИСЕД",
            sizeDp = (54f * scale).toInt(),
            opacity = opacity,
            color = TextPrimary,
            offsetX = offsets.crouchOffsetX,
            offsetY = offsets.crouchOffsetY,
            alignment = Alignment.BottomEnd,
            basePaddingX = 104,
            basePaddingY = 172,
            onDrag = { dx, dy ->
                offsets = offsets.copy(crouchOffsetX = offsets.crouchOffsetX + dx, crouchOffsetY = offsets.crouchOffsetY + dy)
            }
        )

        DraggableHudToken(
            label = "СМЕНА",
            sizeDp = (58f * scale).toInt(),
            opacity = opacity,
            color = StrikeAmber,
            offsetX = offsets.swapOffsetX,
            offsetY = offsets.swapOffsetY,
            alignment = Alignment.BottomEnd,
            basePaddingX = 210,
            basePaddingY = 26,
            onDrag = { dx, dy ->
                offsets = offsets.copy(swapOffsetX = offsets.swapOffsetX + dx, swapOffsetY = offsets.swapOffsetY + dy)
            }
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.DraggableHudToken(
    label: String,
    sizeDp: Int,
    opacity: Float,
    color: Color,
    offsetX: Float,
    offsetY: Float,
    alignment: Alignment,
    basePaddingX: Int,
    basePaddingY: Int,
    onDrag: (Float, Float) -> Unit
) {
    val paddingMod = when (alignment) {
        Alignment.BottomStart -> Modifier.padding(start = basePaddingX.dp, bottom = basePaddingY.dp)
        else -> Modifier.padding(end = basePaddingX.dp, bottom = basePaddingY.dp)
    }
    Box(
        modifier = Modifier
            .align(alignment)
            .then(paddingMod)
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .size(sizeDp.dp)
            .alpha(opacity)
            .clip(CircleShape)
            .background(GunmetalCard.copy(alpha = 0.85f))
            .border(2.dp, color, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}
