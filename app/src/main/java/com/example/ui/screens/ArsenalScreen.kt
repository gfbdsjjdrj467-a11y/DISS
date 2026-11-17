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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.CareerStatsEntity
import com.example.game.engine.WeaponPreviewGLView
import com.example.game.model.GameSettings
import com.example.game.model.WeaponCatalog
import com.example.game.model.WeaponCategory
import com.example.game.model.WeaponId
import com.example.ui.theme.EmeraldSuccess
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
fun ArsenalScreen(
    settings: GameSettings,
    careerStats: CareerStatsEntity,
    onEquipPrimary: (WeaponId) -> Unit,
    onEquipSecondary: (WeaponId) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    var inspectedWeaponId by remember { mutableStateOf(settings.primaryWeapon) }
    val inspectedSpec = WeaponCatalog.byId(inspectedWeaponId)
    val isUnlocked = careerStats.totalKills >= inspectedSpec.unlockKillsRequired
    val isPrimary = settings.primaryWeapon == inspectedWeaponId
    val isSecondary = settings.secondaryWeapon == inspectedWeaponId

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
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("arsenal_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
                }
                Spacer(Modifier.width(6.dp))
                Column {
                    Text("АРСЕНАЛ И 3D-ОСМОТР", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text("6 классов вооружения • Вращайте 3D-модель свайпом", style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                }
            }
            Text(
                text = "ФРАГИ: ${careerStats.totalKills}",
                style = MaterialTheme.typography.labelLarge,
                color = StrikeAmber
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Left: Weapon Roster
            LazyColumn(
                modifier = Modifier.weight(0.95f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(WeaponCatalog.allWeapons) { w ->
                    val selected = w.id == inspectedWeaponId
                    val unlocked = careerStats.totalKills >= w.unlockKillsRequired
                    val equippedTag = when {
                        settings.primaryWeapon == w.id -> "ОСНОВНОЕ"
                        settings.secondaryWeapon == w.id -> "ДОП."
                        else -> null
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) GunmetalElevated else GunmetalCard,
                        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) StrikeAmber else TacticalBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weapon_item_${w.id.name.lowercase()}")
                            .clickable { inspectedWeaponId = w.id }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(w.name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                                Text(w.category.displayName, style = MaterialTheme.typography.labelSmall, color = TacticalCyan)
                            }
                            if (!unlocked) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Lock, contentDescription = "Заблокировано", tint = StrikeAmber, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("${w.unlockKillsRequired} уб.", style = MaterialTheme.typography.labelSmall, color = StrikeAmber)
                                }
                            } else if (equippedTag != null) {
                                Surface(
                                    color = EmeraldSuccess.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = equippedTag,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldSuccess,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Right: 3D Preview + Stat Bars + Equip Actions
            Surface(
                modifier = Modifier.weight(1.25f).fillMaxHeight(),
                shape = RoundedCornerShape(12.dp),
                color = SteelSurface,
                border = BorderStroke(1.dp, TacticalBorder)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
                    // 3D OpenGL Weapon Model Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF101720))
                    ) {
                        WeaponPreviewGLView(
                            weaponId = inspectedWeaponId,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text(
                            text = "СВАЙП ДЛЯ ВРАЩЕНИЯ 3D",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        )
                        Column(modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
                            Text(inspectedSpec.name, style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                            Text(inspectedSpec.category.displayName, style = MaterialTheme.typography.labelSmall, color = StrikeAmber)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text(inspectedSpec.description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            StatBar("УРОН", (inspectedSpec.damage * inspectedSpec.pellets).coerceAtMost(130) / 130f, "${inspectedSpec.damage}${if (inspectedSpec.pellets > 1) "x${inspectedSpec.pellets}" else ""}")
                            StatBar("ТЕМП", inspectedSpec.rpm / 900f, "${inspectedSpec.rpm.toInt()} RPM")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            StatBar("ТОЧНОСТЬ", (1f - inspectedSpec.adsSpreadDeg / 4.5f).coerceIn(0.15f, 1f), "${inspectedSpec.magSize}/${inspectedSpec.reserveAmmo}")
                            StatBar("СКОРОСТЬ", inspectedSpec.moveSpeedMult / 1.1f, "${inspectedSpec.reloadTimeSec}с пер.")
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onEquipPrimary(inspectedWeaponId) },
                            enabled = isUnlocked && !isPrimary,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("equip_primary_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = StrikeAmber, contentColor = ObsidianBg),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isPrimary) Icon(Icons.Filled.Check, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = when {
                                    !isUnlocked -> "НУЖНО ${inspectedSpec.unlockKillsRequired} УБ."
                                    isPrimary -> "ОСНОВНОЕ"
                                    else -> "В ОСНОВНОЙ СЛОТ"
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                        }

                        OutlinedButton(
                            onClick = { onEquipSecondary(inspectedWeaponId) },
                            enabled = isUnlocked && !isSecondary && inspectedSpec.category == WeaponCategory.PISTOL,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("equip_secondary_btn"),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TacticalBorder)
                        ) {
                            Text(
                                text = if (isSecondary) "ВТОРОЙ СЛОТ" else if (inspectedSpec.category == WeaponCategory.PISTOL) "ВО ВТОРОЙ СЛОТ" else "ТОЛЬКО ОСНОВНОЙ",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSecondary) EmeraldSuccess else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBar(label: String, ratio: Float, valueText: String) {
    Column(modifier = Modifier.padding(bottom = 5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Text(valueText, style = MaterialTheme.typography.labelSmall, color = TextPrimary)
        }
        Spacer(Modifier.height(2.dp))
        LinearProgressIndicator(
            progress = { ratio.coerceIn(0.05f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = StrikeAmber,
            trackColor = GunmetalCard
        )
    }
}
