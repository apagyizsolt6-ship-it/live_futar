package com.livefutar.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shimmer / skeleton betöltő komponensek a meccslistákhoz.
 */
@Composable
fun SkeletonMatchList(
    count: Int = 6,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Felső „liga fejléc” skeleton
        SkeletonLeagueHeader()
        repeat(count) {
            SkeletonMatchCard()
        }
    }
}

@Composable
fun SkeletonLeagueHeader() {
    val shimmer = shimmerBrush()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(shimmer)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer)
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer)
        )
    }
}

@Composable
fun SkeletonMatchCard() {
    val shimmer = shimmerBrush()
    val shape = RoundedCornerShape(16.dp)
    val surface = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(surface)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        // Idő / státusz sor
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmer)
            )
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmer)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Hazai
        TeamSkeletonRow(shimmer)
        Spacer(modifier = Modifier.height(10.dp))
        // Vendég
        TeamSkeletonRow(shimmer)
    }
}

@Composable
private fun TeamSkeletonRow(shimmer: Brush) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(shimmer)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(shimmer)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .width(22.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmer)
        )
    }
}

@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)

    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = androidx.compose.ui.geometry.Offset(translate - 200f, 0f),
        end = androidx.compose.ui.geometry.Offset(translate + 200f, 0f)
    )
}

/** Finom élő pont pulzálás – újrafelhasználható. */
@Composable
fun LivePulseDot(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary
) {
    val transition = rememberInfiniteTransition(label = "livePulse")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier.size(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha * 0.25f))
        )
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha))
        )
    }
}
