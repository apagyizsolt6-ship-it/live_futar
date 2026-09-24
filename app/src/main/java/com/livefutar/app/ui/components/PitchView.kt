package com.livefutar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefutar.app.model.MatchEventModel

/**
 * Esemény-alapú pálya nézet (stilizált).
 * Nincs AnimatedVisibility – elkerüli a Compose Stack.pop crash-t.
 */
@Composable
fun PitchView(
    events: List<MatchEventModel>,
    homeTeamId: Long?
) {
    val markers = remember(events, homeTeamId) {
        buildMarkers(events, homeTeamId)
    }
    var selectedKey by remember { mutableStateOf<String?>(null) }
    val selected = markers.firstOrNull { it.key == selectedKey }

    Column(modifier = Modifier.fillMaxWidth()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.55f)
                .clip(RoundedCornerShape(12.dp))
        ) {
            PitchBackground()
            markers.forEach { marker ->
                PitchMarkerIcon(
                    marker = marker,
                    pitchWidth = maxWidth,
                    pitchHeight = maxHeight,
                    selected = marker.key == selectedKey,
                    onClick = {
                        selectedKey =
                            if (selectedKey == marker.key) null else marker.key
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selected != null) {
            val ev = selected.event
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = ev.icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${ev.time ?: "–"}' · ${ev.typeLabel}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = listOfNotNull(
                            ev.player?.takeIf { it.isNotBlank() },
                            ev.team?.displayName,
                            ev.assist?.let { "gólpassz: $it" }
                        ).joinToString(" · ").ifBlank { "Esemény" },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

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
private fun PitchMarkerIcon(
    marker: PitchMarker,
    pitchWidth: Dp,
    pitchHeight: Dp,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .offset(
                x = pitchWidth * marker.xFraction - 14.dp,
                y = pitchHeight * marker.yFraction - 14.dp
            )
            .size(28.dp)
            .clip(CircleShape)
            .background(
                if (selected) Color.White.copy(alpha = 0.35f)
                else Color.Black.copy(alpha = 0.25f)
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.4f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = marker.event.icon, fontSize = 14.sp)
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

private data class PitchMarker(
    val event: MatchEventModel,
    val key: String,
    val xFraction: Float,
    val yFraction: Float
)

private fun buildMarkers(events: List<MatchEventModel>, homeTeamId: Long?): List<PitchMarker> {
    val relevant = events.filter {
        it.type in setOf("Goal", "Penalty", "Own Goal", "Yellow Card", "Red Card", "Substitution")
    }
    return relevant.mapIndexed { index, event ->
        val isHome = event.team?.id != null && event.team.id == homeTeamId
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
