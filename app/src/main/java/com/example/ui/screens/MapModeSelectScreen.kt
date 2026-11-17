package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.model.BotDifficulty
import com.example.game.model.GameModeType
import com.example.game.model.GameSettings
import com.example.game.model.MapId
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.GunmetalElevated
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelSurface
import com.example.ui.theme.StrikeAmber
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MapModeSelectScreen(
    settings: GameSettings,
    onSelectMap: (MapId) -> Unit,
    onSelectMode: (GameModeType) -> Unit,
    onSelectDifficulty: (BotDifficulty) -> Unit,
    onSetBotCount: (Int) -> Unit,
    onStartMatch: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).testTag("map_select_back")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("ВЫБОР КАРТЫ И РЕЖИМА", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("5 уникальных локаций • 5 боевых режимов", style = MaterialTheme.typography.labelSmall, color = StrikeAmber)
                }
            }

            Button(
                onClick = onStartMatch,
                modifier = Modifier.height(48.dp).testTag("map_launch_match"),
                colors = ButtonDefaults.buttonColors(containerColor = StrikeAmber, contentColor = ObsidianBg),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Начать бой")
                Spacer(Modifier.width(6.dp))
                Text("В БОЙ", style = MaterialTheme.typography.titleLarge)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: 5 Maps
            LazyColumn(
                modifier = Modifier.weight(1.05f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(MapId.entries) { map ->
                    val selected = map == settings.selectedMap
                    val accent = Color(map.accentHex)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) GunmetalElevated else GunmetalCard,
                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) accent else TacticalBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("map_card_${map.name.lowercase()}")
                            .clickable { onSelectMap(map) }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(accent.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Terrain, contentDescription = map.displayName, tint = accent)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(map.displayName, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(map.subtitle, style = MaterialTheme.typography.labelSmall, color = accent)
                                }
                                Text(map.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, maxLines = 2)
                            }
                            if (selected) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Выбрано", tint = accent)
                            }
                        }
                    }
                }
            }

            // Right: 5 Modes + Bot Difficulty & Count
            Surface(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = SteelSurface,
                border = BorderStroke(1.dp, TacticalBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                ) {
                    Text("ИГРОВОЙ РЕЖИМ", style = MaterialTheme.typography.labelLarge, color = TacticalCyan)
                    Spacer(Modifier.height(8.dp))

                    for (mode in GameModeType.entries) {
                        val selected = mode == settings.selectedMode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) StrikeAmber.copy(alpha = 0.16f) else GunmetalCard,
                            border = BorderStroke(1.dp, if (selected) StrikeAmber else TacticalBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                                .testTag("mode_item_${mode.name.lowercase()}")
                                .clickable { onSelectMode(mode) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(mode.displayName, style = MaterialTheme.typography.titleMedium, color = if (selected) StrikeAmber else TextPrimary)
                                    Text(mode.shortCode, style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                                }
                                Text(mode.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("СЛОЖНОСТЬ БОТОВ", style = MaterialTheme.typography.labelLarge, color = TacticalCyan)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        for (diff in BotDifficulty.entries) {
                            val selected = diff == settings.botDifficulty
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectDifficulty(diff) },
                                label = { Text(diff.displayName, style = MaterialTheme.typography.labelSmall) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = StrikeAmber,
                                    selectedLabelColor = ObsidianBg
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("КОЛИЧЕСТВО БОТОВ", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        Text("${settings.botCount}", style = MaterialTheme.typography.labelLarge, color = StrikeAmber)
                    }
                    Slider(
                        value = settings.botCount.toFloat(),
                        onValueChange = { onSetBotCount(it.toInt()) },
                        valueRange = 2f..12f,
                        steps = 9,
                        colors = SliderDefaults.colors(thumbColor = StrikeAmber, activeTrackColor = StrikeAmber)
                    )
                }
            }
        }
    }
}
