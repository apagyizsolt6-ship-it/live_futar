package com.livefutar.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefutar.app.model.MatchEventModel

/**
 * Esemény-alapú, animált "pálya" nézet. FONTOS: ez NEM valós labda/játékos
 * pozíció-követés (ehhez az API nem ad koordináta-adatot) - a gólok/lapok/
 * cserék a csapat és a percek alapján, stilizáltan jelennek meg a megfelelő
 * pálya-oldalon, animálva, ahogy az esemény adat megérkezik.
 */
@Composable
fun PitchView(
    events: List<MatchEventModel>,
    homeTeamId: Long?
) {
    val markers = remember(events, homeTeamId) {
        buildMarkers(events, homeTeamId)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.55f)
        ) {
            PitchBackground()

            markers.forEach { marker ->
                PitchMarkerIcon(marker, maxWidth, maxHeight)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (markers.isEmpty()) {
            Text(
                text = "Még nincs megjeleníthető esemény ezen a pályán",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                LegendItem("⚽", "Gól")
                Spacer(modifier = Modifier.width(14.dp))
                LegendItem("🟨", "Sárga")
                Spacer(modifier = Modifier.width(14.dp))
                LegendItem("🟥", "Piros")
                Spacer(modifier = Modifier.width(14.dp))
                LegendItem("🔄", "Csere")
            }
        }
    }
}

private data class PitchMarker(
    val event: MatchEventModel,
    val key: String,
    val xFraction: Float,
    val yFraction: Float
)

@Composable
private fun PitchBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val stripeCount = 8
        val stripeWidth = size.width / stripeCount
        for (i in 0 until stripeCount) {
            drawRect(
                color = if (i % 2 == 0) Color(0xFF2E7D32) else Color(0xFF2B6E2E),
                topLeft = Offset(i * stripeWidth, 0f),
                size = Size(stripeWidth, size.height)
            )
        }

        val lineColor = Color.White.copy(alpha = 0.85f)
        val strokeWidth = 2.dp.toPx()
        val edge = 4.dp.toPx()

        drawRect(
            color = lineColor,
            topLeft = Offset(edge, edge),
            size = Size(size.width - edge * 2, size.height - edge * 2),
            style = Stroke(width = strokeWidth)
        )

        drawLine(
            color = lineColor,
            start = Offset(size.width / 2, edge),
            end = Offset(size.width / 2, size.height - edge),
            strokeWidth = strokeWidth
        )

        drawCircle(
            color = lineColor,
            radius = size.height * 0.16f,
            center = Offset(size.width / 2, size.height / 2),
            style = Stroke(width = strokeWidth)
        )

        val boxWidth = size.width * 0.14f
        val boxHeight = size.height * 0.5f

        drawRect(
            color = lineColor,
            topLeft = Offset(edge, (size.height - boxHeight) / 2),
            size = Size(boxWidth, boxHeight),
            style = Stroke(width = strokeWidth)
        )
        drawRect(
            color = lineColor,
            topLeft = Offset(size.width - boxWidth - edge, (size.height - boxHeight) / 2),
            size = Size(boxWidth, boxHeight),
            style = Stroke(width = strokeWidth)
        )
    }
}

@Composable
private fun PitchMarkerIcon(marker: PitchMarker, pitchWidth: Dp, pitchHeight: Dp) {
    val visibleState = remember(marker.key) { MutableTransitionState(false) }

    LaunchedEffect(marker.key) {
        visibleState.targetState = true
    }

    AnimatedVisibility(
        visibleState = visibleState,
        enter = scaleIn(animationSpec = tween(450)) + fadeIn(animationSpec = tween(450)),
        modifier = Modifier.offset(
            x = pitchWidth * marker.xFraction - 10.dp,
            y = pitchHeight * marker.yFraction - 10.dp
        )
    ) {
        Text(text = marker.event.icon, fontSize = 18.sp)
    }
}

@Composable
private fun LegendItem(icon: String, label: String) {
    Row {
        Text(text = icon, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun buildMarkers(events: List<MatchEventModel>, homeTeamId: Long?): List<PitchMarker> {
    val relevant = events.filter {
        it.type in setOf("Goal", "Penalty", "Own Goal", "Yellow Card", "Red Card", "Substitution")
    }

    return relevant.mapIndexed { index, event ->
        val isHome = event.team?.id == homeTeamId
        val minute = event.minuteSortKey.coerceIn(0, 120)
        val progress = minute / 120f

        val xFraction = if (isHome) {
            0.15f + progress * 0.35f
        } else {
            0.85f - progress * 0.35f
        }

        val yFraction = 0.15f + (index % 4) * 0.22f

        PitchMarker(
            event = event,
            key = "${event.time}_${event.type}_${event.player}_$index",
            xFraction = xFraction.coerceIn(0.05f, 0.95f),
            yFraction = yFraction.coerceIn(0.05f, 0.85f)
        )
    }
}
