package com.livefutar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BetSlipScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    var selections by remember { mutableStateOf(BetSlipManager.getSelections(context)) }
    val combined = selections.fold(1.0) { acc, s -> acc * s.odd }

    fun refresh() {
        selections = BetSlipManager.getSelections(context)
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
                                "Kombinált odds",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            String.format("%.2f", combined),
                            fontSize = 28.sp,
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
            String.format("%.2f", sel.odd),
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
