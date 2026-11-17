package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.CareerStatsEntity
import com.example.game.model.GameSettings
import com.example.game.model.WeaponCatalog
import com.example.ui.AppRoute
import com.example.ui.components.RankBadgePill
import com.example.ui.theme.AlphaBlue
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
fun MainMenuScreen(
    settings: GameSettings,
    careerStats: CareerStatsEntity,
    onPlayClick: () -> Unit,
    onPlayRankedClick: () -> Unit,
    onNavigate: (AppRoute) -> Unit
) {
    val context = LocalContext.current
    val primaryWeapon = WeaponCatalog.byId(settings.primaryWeapon)
    val activeSkin = settings.skinFor(settings.primaryWeapon)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        ObsidianBg.copy(alpha = 0.88f),
                        ObsidianBg.copy(alpha = 0.42f),
                        ObsidianBg.copy(alpha = 0.25f)
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Tactical Navigation Column
            Column(
                modifier = Modifier
                    .width(290.dp)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.Center
            ) {
                // Logo Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(StrikeAmber.copy(alpha = 0.16f))
                            .border(1.5.dp, StrikeAmber, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GpsFixed,
                            contentDescription = "Strike Arena Logo",
                            tint = StrikeAmber,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "STRIKE ARENA",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontSize = 26.sp,
                                letterSpacing = 2.sp
                            ),
                            color = TextPrimary
                        )
                        Text(
                            text = "MOBILE TACTICAL FPS",
                            style = MaterialTheme.typography.labelSmall,
                            color = StrikeAmber
                        )
                    }
                }

                // Primary Play Button
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("menu_play_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StrikeAmber,
                        contentColor = ObsidianBg
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Играть", modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "ИГРАТЬ",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onPlayRankedClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("menu_play_ranked_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TacticalCyan.copy(alpha = 0.22f),
                        contentColor = TacticalCyan
                    ),
                    border = BorderStroke(1.5.dp, TacticalCyan)
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = "2x2 На звание", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "2x2 НА ЗВАНИЕ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(Modifier.height(8.dp))

                MenuActionButton(
                    label = "ВЫБОР КАРТЫ",
                    icon = Icons.Filled.Map,
                    tag = "menu_maps_button",
                    onClick = { onNavigate(AppRoute.MAP_MODE_SELECT) }
                )
                Spacer(Modifier.height(6.dp))

                MenuActionButton(
                    label = "АРСЕНАЛ",
                    icon = Icons.Filled.Shield,
                    tag = "menu_arsenal_button",
                    onClick = { onNavigate(AppRoute.ARSENAL) }
                )
                Spacer(Modifier.height(6.dp))

                MenuActionButton(
                    label = "НАСТРОЙКИ",
                    icon = Icons.Filled.Settings,
                    tag = "menu_settings_button",
                    onClick = { onNavigate(AppRoute.SETTINGS) }
                )
                Spacer(Modifier.height(6.dp))

                MenuActionButton(
                    label = "СТАТИСТИКА",
                    icon = Icons.Filled.BarChart,
                    tag = "menu_stats_button",
                    onClick = { onNavigate(AppRoute.STATS) }
                )
                Spacer(Modifier.height(6.dp))

                MenuActionButton(
                    label = "ВЫХОД",
                    icon = Icons.AutoMirrored.Filled.ExitToApp,
                    tag = "menu_exit_button",
                    accent = Color(0xFFFF5252),
                    onClick = { (context as? Activity)?.finishAffinity() }
                )
            }

            // Right Tactical Deployment Card + Rank Badge over live 3D Polygon
            Column(
                modifier = Modifier
                    .width(310.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                RankBadgePill(rp = careerStats.rankPoints)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SteelSurface.copy(alpha = 0.86f),
                    border = BorderStroke(1.dp, TacticalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_tactical_banner),
                                contentDescription = "Tactical Deployment Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.85f)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, ObsidianBg.copy(alpha = 0.88f))
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = settings.selectedMap.displayName.uppercase(),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${settings.selectedMode.displayName} • ${settings.botDifficulty.displayName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TacticalCyan
                                )
                            }
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(text = "ОРУЖИЕ И СКИН", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                Text(
                                    text = primaryWeapon.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = StrikeAmber,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = activeSkin.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TacticalCyan,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BriefStatChip("БОТЫ", "${settings.botCount}", AlphaBlue)
                            BriefStatChip("K/D", String.format("%.2f", careerStats.kdRatio), EmeraldSuccess)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuActionButton(
    label: String,
    icon: ImageVector,
    tag: String,
    accent: Color = TextPrimary,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag(tag),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, TacticalBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = GunmetalCard.copy(alpha = 0.84f),
            contentColor = accent
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = accent)
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.8.sp),
                color = accent
            )
        }
    }
}

@Composable
private fun BriefStatChip(label: String, value: String, color: Color) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = color)
    }
}
