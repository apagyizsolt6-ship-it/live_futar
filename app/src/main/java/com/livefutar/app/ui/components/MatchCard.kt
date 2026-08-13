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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.theme.AccentGold
import com.livefutar.app.ui.theme.AccentGreen
import com.livefutar.app.ui.theme.LiveBorder
import com.livefutar.app.ui.theme.LiveGlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchCard(
    match: MatchModel,
    isHomeFavorite: Boolean,
    isAwayFavorite: Boolean,
    onToggleHomeFavorite: () -> Unit,
    onToggleAwayFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val isLive = match.isLive
    val shape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
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
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
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
            // Bal oldali élő accent sáv
            if (isLive) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(72.dp)
                        .background(AccentGreen, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                )
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(match = match, modifier = Modifier.width(58.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    TeamRow(
                        logoUrl = match.homeTeam?.logo,
                        name = match.homeTeam?.name ?: "Hazai csapat",
                        isFavorite = isHomeFavorite,
                        onToggleFavorite = onToggleHomeFavorite
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    TeamRow(
                        logoUrl = match.awayTeam?.logo,
                        name = match.awayTeam?.name ?: "Vendég csapat",
                        isFavorite = isAwayFavorite,
                        onToggleFavorite = onToggleAwayFavorite
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.width(48.dp)
                ) {
                    if (match.isNotStarted) {
                        Text(
                            text = match.kickoffTime,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        val scoreColor = if (isLive) AccentGreen else MaterialTheme.colorScheme.onSurface
                        Text(
                            text = match.homeScoreDisplay,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = match.awayScoreDisplay,
                            fontSize = 17.sp,
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
                    .size(22.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
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
        Text(
            text = if (isFavorite) "★" else "☆",
            fontSize = 14.sp,
            color = if (isFavorite) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onToggleFavorite() }
        )
    }
}

@Composable
private fun StatusBadge(match: MatchModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
    ) {
        if (match.isLive) {
            val infiniteTransition = rememberInfiniteTransition(label = "live-pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.3f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "live-pulse-alpha"
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(AccentGreen)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "ÉLŐ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen,
                    letterSpacing = 0.5.sp
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
