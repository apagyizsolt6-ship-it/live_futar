package com.livefutar.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livefutar.app.data.BestOdds
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.theme.AccentGold
import com.livefutar.app.ui.theme.AccentGreen
import com.livefutar.app.ui.theme.LiveBorder
import com.livefutar.app.ui.theme.LiveGlow
import com.livefutar.app.util.Haptics
import java.util.Locale
import kotlin.math.abs

private val CardRadius = 14.dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MatchCard(
    match: MatchModel,
    isHomeFavorite: Boolean,
    isAwayFavorite: Boolean,
    onToggleHomeFavorite: () -> Unit,
    onToggleAwayFavorite: () -> Unit,
    oddsSummary: BestOdds? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isLive = match.isLive
    val shape = RoundedCornerShape(CardRadius)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(stiffness = 500f),
        label = "card-press"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .scale(pressScale)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .then(
                if (isLive) {
                    Modifier
                        .border(1.5.dp, LiveBorder, shape)
                        .background(LiveGlow, shape)
                } else Modifier
            ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLive) 4.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isLive) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Élő zöld sáv + glow
            Box(
                modifier = Modifier
                    .width(if (isLive) 4.dp else 0.dp)
                    .height(76.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(AccentGreen, AccentGreen.copy(alpha = 0.35f))
                        ),
                        shape = RoundedCornerShape(
                            topStart = CardRadius,
                            bottomStart = CardRadius
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(match = match, modifier = Modifier.width(52.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                ) {
                    TeamRow(
                        logoUrl = match.homeTeam?.logo,
                        name = match.homeTeam?.displayName ?: "Hazai",
                        isFavorite = isHomeFavorite,
                        onToggleFavorite = onToggleHomeFavorite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TeamRow(
                        logoUrl = match.awayTeam?.logo,
                        name = match.awayTeam?.displayName ?: "Vendég",
                        isFavorite = isAwayFavorite,
                        onToggleFavorite = onToggleAwayFavorite
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(52.dp)
                ) {
                    if (match.isNotStarted) {
                        Text(
                            text = match.kickoffTime,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (oddsSummary != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OddsMiniRow(oddsSummary)
                        }
                    } else {
                        val baseColor =
                            if (isLive) AccentGreen else MaterialTheme.colorScheme.onSurface
                        AnimatedScore(
                            score = match.homeScoreDisplay,
                            baseColor = baseColor,
                            matchId = match.id,
                            side = "home"
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        AnimatedScore(
                            score = match.awayScoreDisplay,
                            baseColor = baseColor,
                            matchId = match.id,
                            side = "away"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OddsMiniRow(odds: BestOdds) {
    Column(horizontalAlignment = Alignment.End) {
        listOfNotNull(
            odds.home?.let { "1 ${String.format(Locale.US, "%.2f", it)}" },
            odds.draw?.let { "X ${String.format(Locale.US, "%.2f", it)}" },
            odds.away?.let { "2 ${String.format(Locale.US, "%.2f", it)}" }
        ).forEach { line ->
            Text(
                text = line,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun AnimatedScore(
    score: String,
    baseColor: Color,
    matchId: Long,
    side: String
) {
    var prev by remember(matchId, side) { mutableStateOf(score) }
    val scale = remember { Animatable(1f) }
    val flash by animateColorAsState(
        targetValue = if (score != prev) AccentGreen else baseColor,
        animationSpec = tween(400),
        label = "score-color"
    )

    LaunchedEffect(score) {
        if (score != prev && prev.isNotBlank()) {
            scale.snapTo(1.25f)
            scale.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 400f))
            prev = score
        } else {
            prev = score
        }
    }

    Text(
        text = score,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        color = flash,
        modifier = Modifier.scale(scale.value)
    )
}

@Composable
private fun TeamRow(
    logoUrl: String?,
    name: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val starScale = remember { Animatable(1f) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        TeamAvatar(logoUrl = logoUrl, name = name)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = if (isFavorite) "Kedvenc" else "Kedvencnek jelöl",
            tint = if (isFavorite) AccentGold
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
                .size(18.dp)
                .scale(starScale.value)
                .clickable {
                    Haptics.tick(haptic)
                    onToggleFavorite()
                }
        )
        LaunchedEffect(isFavorite) {
            starScale.snapTo(1.35f)
            starScale.animateTo(1f, spring(stiffness = 500f))
        }
    }
}

/** Logo, vagy monogram színes körben (hash a névből). */
@Composable
fun TeamAvatar(
    logoUrl: String?,
    name: String,
    size: androidx.compose.ui.unit.Dp = 28.dp
) {
    if (!logoUrl.isNullOrBlank()) {
        AsyncImage(
            model = logoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            contentScale = ContentScale.Fit
        )
    } else {
        val bg = monogramColor(name)
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(bg.copy(alpha = 0.22f))
                .border(1.dp, bg.copy(alpha = 0.45f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = monogramLetters(name),
                fontSize = (size.value * 0.38f).sp,
                fontWeight = FontWeight.Bold,
                color = bg
            )
        }
    }
}

private fun monogramLetters(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 ->
            parts.take(2).map { it.first().uppercaseChar() }.joinToString("")
        name.isNotBlank() -> name.take(2).uppercase(Locale.getDefault())
        else -> "?"
    }
}

private fun monogramColor(name: String): Color {
    val palette = listOf(
        Color(0xFF42A5F5),
        Color(0xFF66BB6A),
        Color(0xFFFFA726),
        Color(0xFFAB47BC),
        Color(0xFF26C6DA),
        Color(0xFFEF5350),
        Color(0xFF5C6BC0),
        Color(0xFFEC407A)
    )
    val idx = abs(name.hashCode()) % palette.size
    return palette[idx]
}

/** Kompakt állapot: – / 1.F / 2.F / Szünet / Vége / ÉLŐ + perc */
@Composable
private fun StatusBadge(
    match: MatchModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        when {
            match.isLive -> {
                val infiniteTransition = rememberInfiniteTransition(label = "live-pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 0.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "live-a"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AccentGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .alpha(alpha)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "ÉLŐ",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }
                match.liveMinuteLabel?.let { minute ->
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = minute,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }
            }
            match.isFinished -> {
                Text(
                    text = "Vége",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            match.isNotStarted -> {
                Text(
                    text = "–",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            else -> {
                val short = shortStatus(match.statusLabel)
                Text(
                    text = short,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
        }
    }
}

private fun shortStatus(label: String): String = when {
    label.contains("1.", ignoreCase = true) || label.contains("First", ignoreCase = true) -> "1.F"
    label.contains("2.", ignoreCase = true) || label.contains("Second", ignoreCase = true) -> "2.F"
    label.contains("Félidő", ignoreCase = true) || label.contains("Half", ignoreCase = true) -> "Szünet"
    label.contains("Hosszabb", ignoreCase = true) || label.contains("Extra", ignoreCase = true) -> "Hossz."
    label.contains("Büntet", ignoreCase = true) || label.contains("Penalt", ignoreCase = true) -> "11-es"
    label.contains("FRISSÍT", ignoreCase = true) -> "…"
    else -> label.take(8)
}
