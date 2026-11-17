package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.CareerStatsEntity
import com.example.data.MatchRecordEntity
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelSurface
import com.example.ui.theme.StrikeAmber
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StatsScreen(
    career: CareerStatsEntity,
    history: List<MatchRecordEntity>,
    onResetStats: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var showConfirmReset by remember { mutableStateOf(false) }

    if (showConfirmReset) {
        AlertDialog(
            onDismissRequest = { showConfirmReset = false },
            containerColor = SteelSurface,
            title = { Text("Сбросить статистику?", color = TextPrimary) },
            text = { Text("Вся история матчей и карьерные показатели будут удалены.", color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    onResetStats()
                    showConfirmReset = false
                }) {
                    Text("СБРОСИТЬ", color = AlertCrimson)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmReset = false }) {
                    Text("ОТМЕНА", color = TextPrimary)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("stats_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("БОЕВАЯ СТАТИСТИКА", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("Локальный профиль оперативника", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                }
            }

            OutlinedButton(
                onClick = { showConfirmReset = true },
                modifier = Modifier.height(48.dp),
                border = BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.6f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertCrimson)
            ) {
                Icon(Icons.Filled.DeleteForever, contentDescription = "Сброс", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("СБРОСИТЬ", style = MaterialTheme.typography.labelLarge)
            }
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left Career Summary Grid
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareerTile("МАТЧИ", "${career.matchesPlayed}", TextPrimary, Modifier.weight(1f))
                    CareerTile("ПОБЕДЫ", "${career.wins}", EmeraldSuccess, Modifier.weight(1f))
                    CareerTile("ПОРАЖЕНИЯ", "${career.losses}", AlertCrimson, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareerTile("УСТРАНЕНИЯ", "${career.totalKills}", StrikeAmber, Modifier.weight(1f))
                    CareerTile("СМЕРТИ", "${career.totalDeaths}", TextSecondary, Modifier.weight(1f))
                    CareerTile("K/D", String.format("%.2f", career.kdRatio), TacticalCyan, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CareerTile("ТОЧНОСТЬ", String.format("%.1f%%", career.accuracyPercent), TacticalCyan, Modifier.weight(1f))
                    CareerTile("ЛУЧШИЙ СЧЁТ", "${career.bestScore}", StrikeAmber, Modifier.weight(1f))
                    CareerTile("МАКС. ВОЛНА", "${career.highestSurvivalWave}", EmeraldSuccess, Modifier.weight(1f))
                }
            }

            // Right Match History
            Surface(
                modifier = Modifier.weight(1.1f).fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = SteelSurface,
                border = BorderStroke(1.dp, TacticalBorder)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Text("ПОСЛЕДНИЕ МАТЧИ", style = MaterialTheme.typography.labelLarge, color = StrikeAmber)
                    Spacer(Modifier.height(8.dp))
                    if (history.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Нет завершённых матчей", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                            Text("Сыграйте первый бой, чтобы увидеть отчёт", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(history) { item ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = GunmetalCard,
                                    border = BorderStroke(1.dp, TacticalBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("${item.mapName} • ${item.modeName}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                                            Text(
                                                "Уб: ${item.kills} | См: ${item.deaths} | Точн: ${String.format("%.0f%%", item.accuracyPercent)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextSecondary
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                if (item.victory) "ПОБЕДА" else "ПОРАЖЕНИЕ",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (item.victory) EmeraldSuccess else AlertCrimson
                                            )
                                            Text("${item.score} ОЧК.", style = MaterialTheme.typography.labelSmall, color = StrikeAmber)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(84.dp),
        shape = RoundedCornerShape(10.dp),
        color = GunmetalCard,
        border = BorderStroke(1.dp, TacticalBorder)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(value, style = MaterialTheme.typography.headlineMedium, color = accent)
        }
    }
}
