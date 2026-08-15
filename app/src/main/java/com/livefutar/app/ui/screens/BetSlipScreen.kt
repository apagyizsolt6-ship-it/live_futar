package com.livefutar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefutar.app.data.BetSlipManager
import com.livefutar.app.model.BetSlipSelection
import com.livefutar.app.ui.theme.AccentGold
import com.livefutar.app.ui.theme.AccentGreen
import java.util.Locale

private val quickStakes = listOf(500.0, 1000.0, 2000.0, 5000.0, 10000.0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetSlipScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selections by remember { mutableStateOf(BetSlipManager.getSelections(context)) }
    var stake by remember { mutableStateOf(BetSlipManager.getStake(context)) }
    var stakeText by remember { mutableStateOf(formatStakeInput(BetSlipManager.getStake(context))) }

    val combined = selections.fold(1.0) { acc, s -> acc * s.odd }
    val potentialWin = stake * combined

    fun refresh() {
        selections = BetSlipManager.getSelections(context)
    }

    fun updateStake(newStake: Double) {
        val safeStake = newStake.coerceIn(0.0, 10_000_000.0)
        stake = safeStake
        stakeText = formatStakeInput(safeStake)
        BetSlipManager.setStake(context, safeStake)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Fogadási szelvény 🎟️", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                navigationIcon = {
                    Text(
                        "←",
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clickable { onBackClick() }
                            .padding(horizontal = 12.dp)
                    )
                },
                actions = {
                    if (selections.isNotEmpty()) {
                        Text(
                            "Törlés",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .clickable {
                                    BetSlipManager.clear(context)
                                    refresh()
                                }
                                .padding(end = 14.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (selections.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🎟️", fontSize = 40.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "A szelvény üres",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Nyiss meg egy meccset → Odds fül\nés koppints egy oddsra",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selections, key = { it.matchId }) { sel ->
                        SlipRow(sel) {
                            BetSlipManager.removeSelection(context, sel.matchId)
                            refresh()
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "${selections.size} tipp",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Kombinált odds ${formatOdd(combined)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            "Tét (Ft)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(6.dp))

                        OutlinedTextField(
                            value = stakeText,
                            onValueChange = { text ->
                                stakeText = text
                                val parsed = text.replace(" ", "").toDoubleOrNull()
                                if (parsed != null) {
                                    updateStake(parsed)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                Text(
                                    "Ft",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            quickStakes.forEach { amount ->
                                val selected = stake == amount
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (selected) AccentGold.copy(alpha = 0.25f)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            1.dp,
                                            if (selected) AccentGold
                                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { updateStake(amount) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        formatStakeChip(amount),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(Modifier.height(16.dp))

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                "Lehetséges nyeremény",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            "${formatStakeChip(potentialWin)} Ft",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )

                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Ez egy szimulált szelvény – a fogadást a saját bookmakerednél tudod leadni.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SlipRow(sel: BetSlipSelection, onRemove: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                sel.matchLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${sel.market} · ${sel.label}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!sel.bookmakerName.isNullOrBlank()) {
                Text(
                    sel.bookmakerName,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
        Text(
            formatOdd(sel.odd),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGreen,
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Text(
            "✕",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .clickable(onClick = onRemove)
                .padding(4.dp)
        )
    }
}

private fun formatOdd(value: Double): String =
    String.format(Locale.US, "%.2f", value)

private fun formatStakeInput(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else formatOdd(value)

private fun formatStakeChip(value: Double): String {
    val rounded = value.toLong()
    return String.format(Locale.US, "%,d", rounded).replace(",", " ")
}
