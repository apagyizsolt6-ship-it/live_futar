package com.livefutar.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.theme.AccentGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchCard(match: MatchModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusBadge(match = match, modifier = Modifier.width(60.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.Start
            ) {
                TeamRow(
                    logoUrl = match.homeTeam?.logo,
                    name = match.homeTeam?.name ?: "Hazai csapat"
                )
                Spacer(modifier = Modifier.height(8.dp))
                TeamRow(
                    logoUrl = match.awayTeam?.logo,
                    name = match.awayTeam?.name ?: "Vendég csapat"
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.width(34.dp)
            ) {
                Text(
                    text = match.homeScoreDisplay,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (match.isLive) AccentGreen else MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = match.awayScoreDisplay,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (match.isLive) AccentGreen else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TeamRow(logoUrl: String?, name: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = name,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun StatusBadge(match: MatchModel, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.Start) {
        if (match.isLive) {
            val infiniteTransition = rememberInfiniteTransition(label = "live-pulse")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(700, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "live-pulse-alpha"
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
            }
        } else {
            Text(
                text = match.statusLabel,
                fontSize = 11.sp,
                color = if (match.isFinished) Color.Gray else MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        }
    }
}
