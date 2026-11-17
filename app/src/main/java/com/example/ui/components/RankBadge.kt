package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.game.model.RankSystem
import com.example.game.model.RankTier
import com.example.ui.theme.GunmetalCard
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun RankEmblemCanvas(
    tier: RankTier,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 44.dp
) {
    val primary = Color(tier.primaryHex)
    val secondary = Color(tier.secondaryHex)

    Canvas(modifier = modifier.size(sizeDp)) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        // Outer metallic shield silhouette
        val shield = Path().apply {
            moveTo(cx, h * 0.08f)
            lineTo(w * 0.88f, h * 0.24f)
            lineTo(w * 0.82f, h * 0.68f)
            lineTo(cx, h * 0.94f)
            lineTo(w * 0.18f, h * 0.68f)
            lineTo(w * 0.12f, h * 0.24f)
            close()
        }
        drawPath(
            path = shield,
            brush = Brush.verticalGradient(listOf(primary.copy(alpha = 0.92f), secondary.copy(alpha = 0.95f)))
        )
        drawPath(
            path = shield,
            color = Color.White.copy(alpha = 0.55f),
            style = Stroke(width = w * 0.045f)
        )

        // Winged apex accents for higher ranks (Silver+)
        if (tier.ordinal >= RankTier.SILVER.ordinal) {
            val leftWing = Path().apply {
                moveTo(w * 0.14f, h * 0.30f)
                lineTo(w * 0.02f, h * 0.18f)
                lineTo(w * 0.10f, h * 0.48f)
                close()
            }
            val rightWing = Path().apply {
                moveTo(w * 0.86f, h * 0.30f)
                lineTo(w * 0.98f, h * 0.18f)
                lineTo(w * 0.90f, h * 0.48f)
                close()
            }
            drawPath(leftWing, color = primary)
            drawPath(rightWing, color = primary)
        }

        // Inner chevron / diamond / star insignia scaled by rank tier
        when (tier) {
            RankTier.BRONZE, RankTier.SILVER -> {
                val count = if (tier == RankTier.SILVER) 2 else 1
                for (i in 0 until count) {
                    val yOff = i * h * 0.14f
                    val chev = Path().apply {
                        moveTo(w * 0.30f, h * 0.50f + yOff)
                        lineTo(cx, h * 0.34f + yOff)
                        lineTo(w * 0.70f, h * 0.50f + yOff)
                        lineTo(cx, h * 0.42f + yOff)
                        close()
                    }
                    drawPath(chev, color = ObsidianBg.copy(alpha = 0.82f))
                }
            }
            RankTier.GOLD, RankTier.PLATINUM -> {
                val gem = Path().apply {
                    moveTo(cx, h * 0.24f)
                    lineTo(w * 0.68f, cy)
                    lineTo(cx, h * 0.76f)
                    lineTo(w * 0.32f, cy)
                    close()
                }
                drawPath(gem, color = Color.White.copy(alpha = 0.88f))
                drawCircle(color = secondary, radius = w * 0.09f, center = Offset(cx, cy))
            }
            RankTier.DIAMOND, RankTier.MASTER -> {
                val crown = Path().apply {
                    moveTo(w * 0.26f, h * 0.60f)
                    lineTo(w * 0.30f, h * 0.32f)
                    lineTo(cx, h * 0.44f)
                    lineTo(w * 0.70f, h * 0.32f)
                    lineTo(w * 0.74f, h * 0.60f)
                    close()
                }
                drawPath(crown, color = Color.White.copy(alpha = 0.92f))
                drawCircle(color = primary, radius = w * 0.08f, center = Offset(cx, h * 0.26f))
            }
            RankTier.LEGEND -> {
                val star = Path()
                val pts = 8
                for (i in 0 until pts) {
                    val r = if (i % 2 == 0) w * 0.28f else w * 0.12f
                    val ang = Math.toRadians((i * 45.0) - 90.0)
                    val x = cx + (cos(ang) * r).toFloat()
                    val y = cy + (sin(ang) * r).toFloat()
                    if (i == 0) star.moveTo(x, y) else star.lineTo(x, y)
                }
                star.close()
                drawPath(star, color = Color(0xFFFFF3B0))
                drawCircle(color = Color.White, radius = w * 0.07f, center = Offset(cx, cy))
            }
        }
    }
}

@Composable
fun RankBadgePill(
    rp: Int,
    modifier: Modifier = Modifier
) {
    val progress = RankSystem.progressForRp(rp)
    val accent = Color(progress.tier.primaryHex)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = GunmetalCard.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.72f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankEmblemCanvas(tier = progress.tier, sizeDp = 28.dp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = progress.fullTitle.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
                Text(
                    text = "${progress.rp} RP",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
fun RankProgressCard(
    rp: Int,
    modifier: Modifier = Modifier
) {
    val progress = RankSystem.progressForRp(rp)
    val accent = Color(progress.tier.primaryHex)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = GunmetalCard,
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.65f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RankEmblemCanvas(tier = progress.tier, sizeDp = 48.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = progress.fullTitle.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = accent
                    )
                    Text(
                        text = "${progress.rp} RP",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                }
                Spacer(Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = { progress.progressInTier.coerceIn(0.04f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = accent,
                    trackColor = ObsidianBg
                )
                Spacer(Modifier.height(4.dp))
                val next = progress.nextTier
                Text(
                    text = if (next != null) {
                        "До «${next.displayName}»: ${progress.rpToNextTier} RP"
                    } else {
                        "Максимальный ранг — Легенда!"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}
