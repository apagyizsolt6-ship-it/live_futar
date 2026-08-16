package com.livefutar.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefutar.app.model.PredictionEntry
import com.livefutar.app.ui.theme.AccentGold
import com.livefutar.app.ui.theme.AccentGreen

/**
 * A meccs közbeni győzelmi-esély alakulását mutatja "hullámzó" oszlopdiagramon,
 * balról jobbra időrendben - minden oszlop egy-egy előrejelzési pillanatot
 * jelöl (Hazai / Döntetlen / Vendég % egymásra rakva).
 *
 * FONTOS: ez az API "predictions.live" mezőjéből jön, ami nem minden
 * meccsnél / minden pillanatban elérhető (elsősorban népszerűbb ligáknál).
 */
@Composable
fun MomentumChart(
    predictions: List<PredictionEntry>,
    homeTeamName: String,
    awayTeamName: String
) {
    if (predictions.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = homeTeamName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentGreen,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Döntetlen",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = awayTeamName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
        ) {
            val barCount = predictions.size
            val gap = 2.dp.toPx()
            val totalGap = gap * (barCount - 1).coerceAtLeast(0)
            val barWidth = ((size.width - totalGap) / barCount).coerceAtLeast(1f)

            predictions.forEachIndexed { index, entry ->
                val home = parsePercent(entry.probabilities?.home)
                val draw = parsePercent(entry.probabilities?.draw)
                val away = parsePercent(entry.probabilities?.away)
                val total = (home + draw + away).takeIf { it > 0f } ?: 100f

                val homeHeight = size.height * (home / total)
                val drawHeight = size.height * (draw / total)
                val awayHeight = size.height * (away / total)

                val x = index * (barWidth + gap)

                drawRect(
                    color = AccentGreen,
                    topLeft = Offset(x, 0f),
                    size = Size(barWidth, homeHeight)
                )
                drawRect(
                    color = Color.Gray.copy(alpha = 0.4f),
                    topLeft = Offset(x, homeHeight),
                    size = Size(barWidth, drawHeight)
                )
                drawRect(
                    color = AccentGold,
                    topLeft = Offset(x, homeHeight + drawHeight),
                    size = Size(barWidth, awayHeight)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Győzelmi esély alakulása a meccs során",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun parsePercent(value: String?): Float {
    return value?.replace("%", "")?.trim()?.toFloatOrNull() ?: 0f
}
