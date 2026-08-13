package com.livefutar.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livefutar.app.data.ApiKeyManager
import com.livefutar.app.data.BetSlipManager
import com.livefutar.app.data.FootballApiService
import com.livefutar.app.model.*
import com.livefutar.app.ui.theme.AccentGold
import com.livefutar.app.ui.theme.AccentGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class DetailTab { OVERVIEW, STATS, LINEUP, ODDS, H2H }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    match: MatchModel,
    onBackClick: () -> Unit,
    onStandingsClick: (MatchModel) -> Unit,
    onOpenBetSlip: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val apiService = remember { FootballApiService.create() }

    var events by remember(match.id) { mutableStateOf<List<MatchEventModel>>(emptyList()) }
    var h2hMatches by remember(match.id) { mutableStateOf<List<MatchModel>>(emptyList()) }
    var oddsItems by remember(match.id) { mutableStateOf<List<BookmakerOdd>>(emptyList()) }
    var lineups by remember(match.id) { mutableStateOf<MatchLineups?>(null) }
    var statistics by remember(match.id) { mutableStateOf<List<TeamStatistics>>(emptyList()) }
    var isLoading by remember(match.id) { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(DetailTab.OVERVIEW) }
    var slipCount by remember { mutableStateOf(BetSlipManager.count(context)) }

    LaunchedEffect(match.id) {
        isLoading = true
        val apiKey = ApiKeyManager.getApiKey(context)
        if (apiKey.isNotBlank()) {
            withContext(Dispatchers.IO) {
                events = try {
                    apiService.getMatchEvents(apiKey, match.id).sortedBy { it.minuteSortKey }
                } catch (_: Exception) { emptyList() }

                val homeId = match.homeTeam?.id
                val awayId = match.awayTeam?.id
                h2hMatches = if (homeId != null && awayId != null) {
                    try {
                        apiService.getHeadToHead(apiKey, homeId, awayId)
                            .filter { it.id != match.id }.take(8)
                    } catch (_: Exception) { emptyList() }
                } else emptyList()

                oddsItems = try {
                    val resp = apiService.getOdds(apiKey, match.id, oddsType = if (match.isLive) "live" else "prematch")
                    resp.data?.firstOrNull()?.odds ?: emptyList()
                } catch (_: Exception) {
                    try {
                        val resp = apiService.getOdds(apiKey, match.id, oddsType = "prematch")
                        resp.data?.firstOrNull()?.odds ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                }

                lineups = try {
                    apiService.getLineups(apiKey, match.id)
                } catch (_: Exception) { null }

                statistics = try {
                    apiService.getMatchStatistics(apiKey, match.id)
                } catch (_: Exception) { emptyList() }
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        match.leagueDisplayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
                    if (match.league?.id != null) {
                        Text(
                            "Tabella",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable { onStandingsClick(match) }
                                .padding(end = 8.dp)
                        )
                    }
                    if (slipCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AccentGold.copy(alpha = 0.2f))
                                .border(1.dp, AccentGold, RoundedCornerShape(10.dp))
                                .clickable { onOpenBetSlip?.invoke() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "🎟️ $slipCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            MatchScoreHeader(match)

            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 8.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                DetailTab.entries.forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Text(
                                when (tab) {
                                    DetailTab.OVERVIEW -> "Összegzés"
                                    DetailTab.STATS -> "Stat"
                                    DetailTab.LINEUP -> "Felállás"
                                    DetailTab.ODDS -> "Odds"
                                    DetailTab.H2H -> "H2H"
                                },
                                fontSize = 13.sp,
                                fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                when (selectedTab) {
                    DetailTab.OVERVIEW -> OverviewTab(match, events)
                    DetailTab.STATS -> StatsTab(statistics, match)
                    DetailTab.LINEUP -> LineupTab(lineups, match)
                    DetailTab.ODDS -> OddsTab(
                        odds = oddsItems,
                        match = match,
                        onAddToSlip = { sel ->
                            val ok = BetSlipManager.addSelection(context, sel)
                            slipCount = BetSlipManager.count(context)
                            Toast.makeText(
                                context,
                                if (ok) "Hozzáadva a szelvényhez" else "Szelvény tele (max 10)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                    DetailTab.H2H -> H2HTab(h2hMatches)
                }
            }
        }
    }
}

@Composable
private fun MatchScoreHeader(match: MatchModel) {
    val borderColor = if (match.isLive) AccentGreen.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (match.isLive) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = if (match.isLive) {
                        "ÉLŐ ${match.liveMinuteLabel ?: ""}".trim()
                    } else match.statusLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (match.isLive) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamBlock(match.homeTeam, Modifier.weight(1f))
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = if (match.hasScore) "${match.homeScoreDisplay} : ${match.awayScoreDisplay}" else "–",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (match.isLive) AccentGreen else MaterialTheme.colorScheme.onSurface
                    )
                    if (!match.isLive && !match.isFinished) {
                        Text(
                            match.kickoffTime,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                TeamBlock(match.awayTeam, Modifier.weight(1f))
            }
            if (match.isLive || match.isFinished) {
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        match.statusLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamBlock(team: TeamModel?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (!team?.logo.isNullOrBlank()) {
            AsyncImage(
                model = team?.logo,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(CircleShape),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("⚽", fontSize = 22.sp)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            team?.name ?: "?",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun OverviewTab(match: MatchModel, events: List<MatchEventModel>) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Események", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        if (events.isEmpty()) {
            Text(
                "Még nincsenek események",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        } else {
            events.forEach { ev ->
                EventRow(ev)
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun EventRow(ev: MatchEventModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            ev.time ?: "–",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(40.dp)
        )
        Text(
            when (ev.type) {
                "Goal", "Penalty" -> "⚽"
                "Own Goal" -> "🥅"
                "Yellow Card" -> "🟨"
                "Red Card" -> "🟥"
                "Substitution" -> "🔄"
                else -> "•"
            },
            fontSize = 16.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(
                ev.player ?: ev.type ?: "Esemény",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            if (!ev.assist.isNullOrBlank()) {
                Text(
                    "Gólpassz: ${ev.assist}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            ev.type ?: "",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
private fun StatsTab(statistics: List<TeamStatistics>, match: MatchModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (statistics.isEmpty()) {
            Text(
                "Nincs elérhető statisztika ehhez a meccshez",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val homeStats = statistics.getOrNull(0)?.statistics.orEmpty()
        val awayStats = statistics.getOrNull(1)?.statistics.orEmpty()
        val names = (homeStats.mapNotNull { it.displayName ?: it.name } +
                awayStats.mapNotNull { it.displayName ?: it.name }).distinct()

        names.forEach { name ->
            val h = homeStats.find { (it.displayName ?: it.name) == name }?.value?.toString() ?: "–"
            val a = awayStats.find { (it.displayName ?: it.name) == name }?.value?.toString() ?: "–"
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(h, Modifier.weight(1f), textAlign = TextAlign.End, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    name,
                    Modifier.weight(1.4f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(a, Modifier.weight(1f), textAlign = TextAlign.Start, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LineupTab(lineups: MatchLineups?, match: MatchModel) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (lineups == null) {
            Text(
                "A felallas meg nem elerheto - altalaban 30 perccel a kezdes elott",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
            return@Column
        }
        TeamLineupBlock(match.homeTeam?.name ?: "Hazai", lineups.home)
        Spacer(Modifier.height(20.dp))
        TeamLineupBlock(match.awayTeam?.name ?: "Vendeg", lineups.away)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun TeamLineupBlock(teamName: String, lineup: TeamLineup?) {
    Text(
        "$teamName ${lineup?.formation?.let { "($it)" } ?: ""}",
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
    )
    Spacer(Modifier.height(8.dp))
    if (lineup?.initialLineup.isNullOrEmpty()) {
        Text("Nincs kezdo", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
    } else {
        lineup?.initialLineup?.forEach { row ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { p ->
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(6.dp)
                    ) {
                        Column {
                            Text(
                                "${p.number ?: ""} ${p.name ?: ""}".trim(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!p.position.isNullOrBlank()) {
                                Text(p.position, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
    if (!lineup?.substitutes.isNullOrEmpty()) {
        Spacer(Modifier.height(8.dp))
        Text("Pad", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        lineup?.substitutes?.forEach { p ->
            Text(
                "${p.number ?: ""} ${p.name ?: ""}".trim(),
                fontSize = 12.sp,
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun OddsTab(
    odds: List<BookmakerOdd>,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (odds.isEmpty()) {
            Text(
                "Nincs elerheto odds ehhez a meccshez - Ultra plan + tamogatott liga kell",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
            return@Column
        }

        Text(
            "Koppints egy oddsra a szelvenyhez adashoz",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val byMarket = odds.groupBy { it.market ?: "Egyeb" }
        byMarket.forEach { (market, list) ->
            Text(
                market,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, bottom = 6.dp)
            )
            list.forEach { bo ->
                val bookie = bo.bookmakerName ?: "Bookmaker"
                Text(
                    bookie,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bo.values.orEmpty().forEach { v ->
                        val oddVal = v.odd ?: return@forEach
                        val sel = v.value ?: return@forEach
                        OddsChip(
                            label = when (sel.lowercase()) {
                                "home" -> match.homeTeam?.name?.take(10) ?: "1"
                                "away" -> match.awayTeam?.name?.take(10) ?: "2"
                                "draw" -> "X"
                                else -> sel
                            },
                            odd = oddVal,
                            onClick = {
                                onAddToSlip(
                                    BetSlipSelection(
                                        matchId = match.id,
                                        homeName = match.homeTeam?.name ?: "Hazai",
                                        awayName = match.awayTeam?.name ?: "Vendeg",
                                        leagueName = match.leagueDisplayName,
                                        market = market,
                                        selection = sel,
                                        odd = oddVal,
                                        bookmakerName = bookie
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OddsChip(
    label: String,
    odd: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(2.dp))
        Text(
            String.format("%.2f", odd),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun H2HTab(h2h: List<MatchModel>) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Korabbi talalkozok", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.height(10.dp))
        if (h2h.isEmpty()) {
            Text("Nincs elerheto H2H adat", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            h2h.forEach { m ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${m.homeTeam?.name ?: "?"} – ${m.awayTeam?.name ?: "?"}",
                        Modifier.weight(1f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (m.hasScore) "\( {m.homeScoreDisplay}: \){m.awayScoreDisplay}" else "–",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
