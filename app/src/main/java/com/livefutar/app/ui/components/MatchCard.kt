package com.livefutar.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import java.util.Locale

private val CardRadius = 14.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchCard(
    match: MatchModel,
    isHomeFavorite: Boolean,
    isAwayFavorite: Boolean,
    onToggleHomeFavorite: () -> Unit,
    onToggleAwayFavorite: () -> Unit,
    oddsSummary: BestOdds? = null,
    onClick: () -> Unit
) {
    val isLive = match.isLive
    val shape = RoundedCornerShape(CardRadius)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .then(
                if (isLive) {
                    Modifier
                        .border(1.5.dp, LiveBorder, shape)
                        .background(LiveGlow, shape)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLive) 3.dp else 1.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isLive) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Élő bal sáv
            if (isLive) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(88.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(AccentGreen, AccentGreen.copy(alpha = 0.4f))
                            ),
                            shape = RoundedCornerShape(topStart = CardRadius, bottomStart = CardRadius)
                        )
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(match = match, modifier = Modifier.width(64.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                ) {
                    TeamRow(
                        logoUrl = match.homeTeam?.logo,
                        name = match.homeTeam?.name ?: "Hazai",
                        isFavorite = isHomeFavorite,
                        onToggleFavorite = onToggleHomeFavorite
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TeamRow(
                        logoUrl = match.awayTeam?.logo,
                        name = match.awayTeam?.name ?: "Vendég",
                        isFavorite = isAwayFavorite,
                        onToggleFavorite = onToggleAwayFavorite
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(56.dp)
                ) {
                    if (match.isNotStarted) {
                        Text(
                            text = match.kickoffTime,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (oddsSummary != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            OddsMiniRow(oddsSummary)
                        }
                    } else {
                        val scoreColor = if (isLive) AccentGreen else MaterialTheme.colorScheme.onSurface
                        Text(
                            text = match.homeScoreDisplay,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = match.awayScoreDisplay,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
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
private fun TeamRow(
    logoUrl: String?,
    name: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
            contentDescription = if (isFavorite) "Kedvenc" else "Kedvencnek jelöl",
            tint = if (isFavorite) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .size(20.dp)
                .clickable { onToggleFavorite() }
        )
    }
}

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
        if (match.isLive) {
            val infiniteTransition = rememberInfiniteTransition(label = "live-pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.35f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "live-pulse-alpha"
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentGreen.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ÉLŐ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen,
                    letterSpacing = 0.4.sp
                )
            }
            match.liveMinuteLabel?.let { minute ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = minute,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            }
        } else if (match.isStaleNotStarted) {
            Text(
                text = "FRISSÍTÉS",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "alatt",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        } else {
            Text(
                text = match.statusLabel,
                fontSize = 11.sp,
                color = if (match.isFinished) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                },
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                lineHeight = 14.sp
            )
        }
    }
}
