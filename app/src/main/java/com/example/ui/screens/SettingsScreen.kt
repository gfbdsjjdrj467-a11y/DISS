package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.model.BotDifficulty
import com.example.game.model.GameSettings
import com.example.game.model.GraphicsQuality
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SteelSurface
import com.example.ui.theme.StrikeAmber
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onApplyGraphicsPreset: (GraphicsQuality) -> Unit,
    onOpenControlsEditor: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ГРАФИКА", "УПРАВЛЕНИЕ", "АВТОМАТИЗАЦИЯ", "ЗВУК", "ИГРА")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("settings_back")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад", tint = TextPrimary)
            }
            Spacer(Modifier.width(6.dp))
            Text("НАСТРОЙКИ СИСТЕМЫ", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SteelSurface,
            contentColor = StrikeAmber,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(positions[selectedTab]),
                    color = StrikeAmber
                )
            }
        ) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = {
                        Text(
                            title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (selectedTab == idx) StrikeAmber else TextSecondary
                        )
                    }
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            color = SteelSurface,
            border = BorderStroke(1.dp, TacticalBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (selectedTab) {
                    0 -> GraphicsSettingsTab(settings, onApplyGraphicsPreset, onUpdateSettings)
                    1 -> ControlsSettingsTab(settings, onUpdateSettings, onOpenControlsEditor)
                    2 -> AutomationSettingsTab(settings, onUpdateSettings)
                    3 -> AudioSettingsTab(settings, onUpdateSettings)
                    4 -> GameplaySettingsTab(settings, onUpdateSettings)
                }
            }
        }
    }
}

@Composable
private fun GraphicsSettingsTab(
    settings: GameSettings,
    onApplyPreset: (GraphicsQuality) -> Unit,
    onUpdate: ((GameSettings) -> GameSettings) -> Unit
) {
    Text("ПРЕСЕТ КАЧЕСТВА ГРАФИКИ", style = MaterialTheme.typography.labelLarge, color = TacticalCyan)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (q in GraphicsQuality.entries) {
            FilterChip(
                selected = settings.graphicsQuality == q,
                onClick = { onApplyPreset(q) },
                label = { Text(q.displayName) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StrikeAmber, selectedLabelColor = ObsidianBg)
            )
        }
    }

    SettingChoiceRow("Детализация поверхностей", listOf("Низкая", "Средняя", "Высокая"), settings.textureQuality) {
        onUpdate { s -> s.copy(textureQuality = it) }
    }
    SettingChoiceRow("Тени окружения", listOf("Выкл", "Мягкие", "Чёткие"), settings.shadowQuality) {
        onUpdate { s -> s.copy(shadowQuality = it) }
    }
    SettingChoiceRow("Качество эффектов и частиц", listOf("Низкое", "Среднее", "Высокое"), settings.effectsQuality) {
        onUpdate { s -> s.copy(effectsQuality = it) }
    }
    SettingChoiceRow("Ограничение FPS", listOf("30 FPS", "60 FPS", "Макс"), when (settings.fpsLimit) { 30 -> 0; 60 -> 1; else -> 2 }) { idx ->
        val limit = when (idx) { 0 -> 30; 1 -> 60; else -> 120 }
        onUpdate { s -> s.copy(fpsLimit = limit) }
    }
}

@Composable
private fun ControlsSettingsTab(
    settings: GameSettings,
    onUpdate: ((GameSettings) -> GameSettings) -> Unit,
    onOpenEditor: () -> Unit
) {
    Button(
        onClick = onOpenEditor,
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("open_controls_editor"),
        colors = ButtonDefaults.buttonColors(containerColor = StrikeAmber, contentColor = ObsidianBg),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(Icons.Filled.Tune, contentDescription = "Редактор HUD")
        Spacer(Modifier.width(8.dp))
        Text("НАСТРОИТЬ РАСПОЛОЖЕНИЕ КНОПОК", style = MaterialTheme.typography.titleMedium)
    }

    SettingSliderRow("Чувствительность камеры", settings.cameraSensitivity, 0.3f..2.5f, "${String.format("%.2f", settings.cameraSensitivity)}x") {
        onUpdate { s -> s.copy(cameraSensitivity = it) }
    }
    SettingSliderRow("Чувствительность прицела (ADS)", settings.adsSensitivity, 0.2f..1.5f, "${String.format("%.2f", settings.adsSensitivity)}x") {
        onUpdate { s -> s.copy(adsSensitivity = it) }
    }
    SettingSliderRow("Размер кнопок HUD", settings.buttonScale, 0.7f..1.4f, "${(settings.buttonScale * 100).toInt()}%") {
        onUpdate { s -> s.copy(buttonScale = it) }
    }
    SettingSliderRow("Прозрачность интерфейса", settings.hudOpacity, 0.3f..1.0f, "${(settings.hudOpacity * 100).toInt()}%") {
        onUpdate { s -> s.copy(hudOpacity = it) }
    }
    SettingToggleRow("Плавающий левый джойстик", "Джойстик появляется в месте касания левой зоны", settings.joystickFloating) {
        onUpdate { s -> s.copy(joystickFloating = it) }
    }
}

@Composable
private fun AutomationSettingsTab(
    settings: GameSettings,
    onUpdate: ((GameSettings) -> GameSettings) -> Unit
) {
    SettingToggleRow("Автоматический бег", "Ускорение при отклонении джойстика до упора вперёд", settings.autoSprint) {
        onUpdate { s -> s.copy(autoSprint = it) }
    }
    SettingToggleRow("Автоматическая перезарядка", "Перезарядка сразу после опустошения магазина", settings.autoReload) {
        onUpdate { s -> s.copy(autoReload = it) }
    }
    SettingToggleRow("Автопереключение оружия", "Переход на запасное оружие при полном расходе патронов", settings.autoSwitchWeapon) {
        onUpdate { s -> s.copy(autoSwitchWeapon = it) }
    }
    SettingToggleRow("Помощь в наведении (Aim Assist)", "Плавное доведение прицела на видимых противников", settings.aimAssistEnabled) {
        onUpdate { s -> s.copy(aimAssistEnabled = it) }
    }
    if (settings.aimAssistEnabled) {
        SettingSliderRow("Интенсивность Aim Assist", settings.aimAssistStrength, 0.1f..1.0f, "${(settings.aimAssistStrength * 100).toInt()}%") {
            onUpdate { s -> s.copy(aimAssistStrength = it) }
        }
    }
    SettingToggleRow("Автоподбор предметов", "Подбирать аптечки, броню и патроны при приближении", settings.autoPickup) {
        onUpdate { s -> s.copy(autoPickup = it) }
    }
    SettingToggleRow("Автооткрытие дверей", "Тактические двери открываются автоматически рядом с игроком", settings.autoOpenDoors) {
        onUpdate { s -> s.copy(autoOpenDoors = it) }
    }
    SettingToggleRow("Автоприцеливание при стрельбе", "Удержание огня автоматически переводит в режим прицела", settings.autoAdsOnFire) {
        onUpdate { s -> s.copy(autoAdsOnFire = it) }
    }
}

@Composable
private fun AudioSettingsTab(
    settings: GameSettings,
    onUpdate: ((GameSettings) -> GameSettings) -> Unit
) {
    SettingSliderRow("Общая громкость", settings.masterVolume, 0f..1f, "${(settings.masterVolume * 100).toInt()}%") {
        onUpdate { s -> s.copy(masterVolume = it) }
    }
    SettingSliderRow("Музыка меню", settings.musicVolume, 0f..1f, "${(settings.musicVolume * 100).toInt()}%") {
        onUpdate { s -> s.copy(musicVolume = it) }
    }
    SettingSliderRow("Звуковые эффекты (выстрелы, шаги)", settings.sfxVolume, 0f..1f, "${(settings.sfxVolume * 100).toInt()}%") {
        onUpdate { s -> s.copy(sfxVolume = it) }
    }
    SettingSliderRow("Звуки интерфейса", settings.uiVolume, 0f..1f, "${(settings.uiVolume * 100).toInt()}%") {
        onUpdate { s -> s.copy(uiVolume = it) }
    }
}

@Composable
private fun GameplaySettingsTab(
    settings: GameSettings,
    onUpdate: ((GameSettings) -> GameSettings) -> Unit
) {
    Text("СЛОЖНОСТЬ ИИ БОТОВ", style = MaterialTheme.typography.labelLarge, color = TacticalCyan)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (d in BotDifficulty.entries) {
            FilterChip(
                selected = settings.botDifficulty == d,
                onClick = { onUpdate { s -> s.copy(botDifficulty = d) } },
                label = { Text(d.displayName) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StrikeAmber, selectedLabelColor = ObsidianBg)
            )
        }
    }
    SettingSliderRow("Количество ботов в матче", settings.botCount.toFloat(), 2f..12f, "${settings.botCount}") {
        onUpdate { s -> s.copy(botCount = it.toInt()) }
    }
}

@Composable
private fun SettingToggleRow(title: String, desc: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GunmetalCard,
        border = BorderStroke(1.dp, TacticalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(desc, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            Switch(
                checked = checked,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBg, checkedTrackColor = StrikeAmber)
            )
        }
    }
}

@Composable
private fun SettingSliderRow(title: String, value: Float, range: ClosedFloatingPointRange<Float>, valueLabel: String, onChanged: (Float) -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GunmetalCard,
        border = BorderStroke(1.dp, TacticalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(valueLabel, style = MaterialTheme.typography.labelLarge, color = StrikeAmber)
            }
            Slider(
                value = value,
                onValueChange = onChanged,
                valueRange = range,
                colors = SliderDefaults.colors(thumbColor = StrikeAmber, activeTrackColor = StrikeAmber)
            )
        }
    }
}

@Composable
private fun SettingChoiceRow(title: String, options: List<String>, selectedIdx: Int, onSelect: (Int) -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = GunmetalCard,
        border = BorderStroke(1.dp, TacticalBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = idx == selectedIdx,
                        onClick = { onSelect(idx) },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = StrikeAmber, selectedLabelColor = ObsidianBg)
                    )
                }
            }
        }
    }
}
