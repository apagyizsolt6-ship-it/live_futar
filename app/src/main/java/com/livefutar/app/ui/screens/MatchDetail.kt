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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.livefutar.app.data.HalfTimeScoreCache
import com.livefutar.app.model.*
import com.livefutar.app.ui.components.MomentumChart
import com.livefutar.app.ui.components.PitchView
import com.livefutar.app.ui.theme.AccentGold
import com.livefutar.app.ui.theme.AccentGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private enum class DetailTab {
    OVERVIEW,
    PITCH,
    STATS,
    LINEUP,
    ODDS,
    H2H
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    match: MatchModel,
    onBackClick: () -> Unit,
    onStandingsClick: (MatchModel) -> Unit,
    onOpenBetSlip: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val apiService = remember {
        FootballApiService.create()
    }

    var events by remember(match.id) {
        mutableStateOf<List<MatchEventModel>>(emptyList())
    }

    var h2hMatches by remember(match.id) {
        mutableStateOf<List<MatchModel>>(emptyList())
    }

    var lineups by remember(match.id) {
        mutableStateOf<MatchLineups?>(null)
    }

    var statistics by remember(match.id) {
        mutableStateOf<List<TeamStatistics>>(emptyList())
    }

    var predictions by remember(match.id) {
        mutableStateOf<List<PredictionItem>>(emptyList())
    }

    var isLoading by remember(match.id) {
        mutableStateOf(true)
    }

    var selectedTab by remember {
        mutableStateOf(DetailTab.OVERVIEW)
    }

    var slipCount by remember {
        mutableStateOf(
            BetSlipManager.count(context)
        )
    }

    LaunchedEffect(match.id) {
        isLoading = true

        val apiKey = ApiKeyManager.getApiKey(context)

        if (apiKey.isNotBlank()) {
            withContext(Dispatchers.IO) {

                events = try {
                    apiService
                        .getMatchEvents(
                            apiKey,
                            match.id
                        )
                        .sortedBy {
                            it.minuteSortKey
                        }
                } catch (_: Exception) {
                    emptyList()
                }

                val homeId = match.homeTeam?.id
                val awayId = match.awayTeam?.id

                h2hMatches =
                    if (homeId != null && awayId != null) {
                        try {
                            apiService
                                .getHeadToHead(
                                    apiKey,
                                    homeId,
                                    awayId
                                )
                                .filter {
                                    it.id != match.id
                                }
                                .take(8)
                        } catch (_: Exception) {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }

                lineups = try {
                    apiService.getLineups(
                        apiKey,
                        match.id
                    )
                } catch (_: Exception) {
                    null
                }

                statistics = try {
                    apiService.getMatchStatistics(
                        apiKey,
                        match.id
                    )
                } catch (_: Exception) {
                    emptyList()
                }

                predictions = try {
                    val details = apiService.getMatchDetails(apiKey, match.id)
                    val livePredictions = details.firstOrNull()?.predictions?.live.orEmpty()
                    val prematchPredictions = details.firstOrNull()?.predictions?.prematch.orEmpty()
                    (livePredictions.ifEmpty { prematchPredictions })
                        .sortedBy { it.generatedAt ?: "" }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }

        isLoading = false
    }

    /*
     * Élő meccsnél az események és a győzelmi-esély is időnként frissül,
     * hogy a Pálya fül ténylegesen "élőnek" tűnjön, amíg nyitva van a képernyő.
     */
    LaunchedEffect(match.id) {
        if (!match.isLive) return@LaunchedEffect

        val apiKey = ApiKeyManager.getApiKey(context)
        if (apiKey.isBlank()) return@LaunchedEffect

        while (true) {
            delay(25_000)

            try {
                events = apiService
                    .getMatchEvents(apiKey, match.id)
                    .sortedBy { it.minuteSortKey }
            } catch (_: Exception) {
                // csendben kihagyjuk ezt a kört
            }

            try {
                val details = apiService.getMatchDetails(apiKey, match.id)
                val livePredictions = details.firstOrNull()?.predictions?.live.orEmpty()
                if (livePredictions.isNotEmpty()) {
                    predictions = livePredictions.sortedBy { it.generatedAt ?: "" }
                }
            } catch (_: Exception) {
                // csendben kihagyjuk ezt a kört
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = match.leagueDisplayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },

                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Vissza"
                        )
                    }
                },

                actions = {

                    if (match.league?.id != null) {
                        Text(
                            text = "Tabella",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clickable {
                                    onStandingsClick(match)
                                }
                                .padding(end = 8.dp)
                        )
                    }

                    if (slipCount > 0) {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(
                                    RoundedCornerShape(10.dp)
                                )
                                .background(
                                    AccentGold.copy(alpha = 0.2f)
                                )
                                .border(
                                    1.dp,
                                    AccentGold,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    onOpenBetSlip?.invoke()
                                }
                                .padding(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                )
                        ) {
                            Text(
                                text = "Szelveny $slipCount",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                        }
                    }
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface
                    )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    MaterialTheme.colorScheme.background
                )
        ) {

            MatchScoreHeader(
                match = match,
                events = events,
                halfTimeScore = HalfTimeScoreCache.get(match.id)
            )

            ScrollableTabRow(
                selectedTabIndex =
                    selectedTab.ordinal,
                edgePadding = 8.dp,
                containerColor =
                    MaterialTheme.colorScheme.surface,
                contentColor =
                    MaterialTheme.colorScheme.primary
            ) {

                DetailTab.entries.forEach { tab ->

                    Tab(
                        selected = selectedTab == tab,

                        onClick = {
                            selectedTab = tab
                        },

                        text = {

                            Text(
                                text =
                                    when (tab) {
                                        DetailTab.OVERVIEW ->
                                            "Összegzés"

                                        DetailTab.PITCH ->
                                            "Pálya"

                                        DetailTab.STATS ->
                                            "Stat"

                                        DetailTab.LINEUP ->
                                            "Felállás"

                                        DetailTab.ODDS ->
                                            "Odds"

                                        DetailTab.H2H ->
                                            "H2H"
                                    },

                                fontSize = 13.sp,

                                fontWeight =
                                    if (selectedTab == tab) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Medium
                                    }
                            )
                        }
                    )
                }
            }

            if (isLoading) {

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }

            } else {

                when (selectedTab) {

                    DetailTab.OVERVIEW ->
                        OverviewTab(events, match)

                    DetailTab.PITCH ->
                        PitchTab(
                            match = match,
                            events = events,
                            predictions = predictions
                        )

                    DetailTab.STATS ->
                        StatsTab(statistics)

                    DetailTab.LINEUP ->
                        LineupTab(
                            lineups,
                            match
                        )

                    DetailTab.ODDS ->
                        OddsSection(
                            match = match,
                            apiService = apiService,
                            onAddToSlip = { selection ->

                                val added =
                                    BetSlipManager
                                        .addSelection(
                                            context,
                                            selection
                                        )

                                slipCount =
                                    BetSlipManager
                                        .count(context)

                                Toast.makeText(
                                    context,
                                    if (added) {
                                        "Hozzaadva a szelvenyhez"
                                    } else {
                                        "Szelveny tele (max 10)"
                                    },
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )

                    DetailTab.H2H ->
                        H2HTab(h2hMatches)
                }
            }
        }
    }
}

@Composable
private fun MatchScoreHeader(
    match: MatchModel,
    events: List<MatchEventModel> = emptyList(),
    halfTimeScore: String? = null
) {
    val borderColor =
        if (match.isLive) {
            AccentGreen.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        }

    val goalMinutes = events
        .filter { it.type in setOf("Goal", "Penalty", "Own Goal") }
        .mapNotNull { it.time }
        .distinct()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = if (match.isLive) {
                        "ÉLŐ " + (match.liveMinuteLabel ?: "")
                    } else {
                        match.statusLabel
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (match.isLive) {
                        AccentGreen
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(
                modifier = Modifier.height(14.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                TeamBlock(
                    team = match.homeTeam,
                    modifier = Modifier.weight(1f)
                )

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally,

                    modifier =
                        Modifier.padding(
                            horizontal = 8.dp
                        )
                ) {

                    Text(
                        text =
                            if (match.hasScore) {
                                match.homeScoreDisplay +
                                    " : " +
                                    match.awayScoreDisplay
                            } else {
                                "-"
                            },

                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (match.isLive) {
                                AccentGreen
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                    )

                    if (
                        !match.isLive &&
                        !match.isFinished
                    ) {
                        Text(
                            text = match.kickoffTime,
                            fontSize = 13.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary
                        )
                    }
                }

                TeamBlock(
                    team = match.awayTeam,
                    modifier = Modifier.weight(1f)
                )
            }

            if (
                match.isLive ||
                match.isFinished
            ) {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                Box(
                    modifier = Modifier
                        .clip(
                            RoundedCornerShape(8.dp)
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .surfaceVariant
                        )
                        .padding(
                            horizontal = 10.dp,
                            vertical = 4.dp
                        )
                ) {

                    Text(
                        text = match.statusLabel,
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.Medium,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

                if (!halfTimeScore.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Félidő: $halfTimeScore",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (goalMinutes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = goalMinutes.joinToString("  ·  ") { "$it'" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentGreen,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun TeamBlock(
    team: TeamModel?,
    modifier: Modifier = Modifier
) {
    val monogram = teamMonogram(team?.name)
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!team?.logo.isNullOrBlank()) {
            AsyncImage(
                model = team?.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = monogram,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = team?.name ?: "?",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun teamMonogram(name: String?): String {
    if (name.isNullOrBlank()) return "?"
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.size >= 2 ->
            parts.take(2).map { it.first().uppercaseChar() }.joinToString("")
        else -> name.take(2).uppercase()
    }
}

@Composable
private fun OverviewTab(
    events: List<MatchEventModel>,
    match: MatchModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val yellowHome = events.count { it.type == "Yellow Card" && it.team?.id == match.homeTeam?.id }
        val yellowAway = events.count { it.type == "Yellow Card" && it.team?.id == match.awayTeam?.id }
        val redHome = events.count { it.type == "Red Card" && it.team?.id == match.homeTeam?.id }
        val redAway = events.count { it.type == "Red Card" && it.team?.id == match.awayTeam?.id }

        if (yellowHome + yellowAway + redHome + redAway > 0) {
            Text("Lapok", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(10.dp))
            CardsSummaryRow(label = "🟨 Sárga", homeValue = yellowHome, awayValue = yellowAway)
            Spacer(modifier = Modifier.height(6.dp))
            CardsSummaryRow(label = "🟥 Piros", homeValue = redHome, awayValue = redAway)
            Spacer(modifier = Modifier.height(20.dp))
        }

        Text("Események", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(12.dp))

        if (events.isEmpty()) {
            Text(
                text = "Még nincsenek események",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        } else {
            val homeId = match.homeTeam?.id
            var homeGoals = 0
            var awayGoals = 0
            events.forEach { event ->
                val isHome = event.team?.id == homeId
                val isGoal = event.type in setOf("Goal", "Penalty")
                val isOwnGoal = event.type == "Own Goal"
                if (isGoal) {
                    if (isHome) homeGoals++ else awayGoals++
                } else if (isOwnGoal) {
                    // Own goal credits the opposing side
                    if (isHome) awayGoals++ else homeGoals++
                }
                val scoreAfter = if (isGoal || isOwnGoal) "$homeGoals–$awayGoals" else null
                TimelineEventRow(
                    event = event,
                    isHome = isHome,
                    scoreAfter = scoreAfter,
                    homeName = match.homeTeam?.name ?: "Hazai",
                    awayName = match.awayTeam?.name ?: "Vendég"
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun PitchTab(
    match: MatchModel,
    events: List<MatchEventModel>,
    predictions: List<PredictionItem>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (predictions.isNotEmpty()) {
            Text(
                text = "Győzelmi esély (élő)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            MomentumChart(
                predictions = predictions,
                homeTeamName = match.homeTeam?.name ?: "Hazai",
                awayTeamName = match.awayTeam?.name ?: "Vendég"
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        Text(
            text = "Esemény-pálya",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Stilizált nézet – az események a csapat és a perc alapján. Koppints egy markerre a részletekért.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        PitchView(
            events = events,
            homeTeamId = match.homeTeam?.id
        )

        val goals = events.filter {
            it.type in setOf("Goal", "Penalty", "Own Goal")
        }
        if (goals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Gólok",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            goals.forEach { g ->
                val isHome = g.team?.id == match.homeTeam?.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = g.time?.let { "$it'" } ?: "–",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = AccentGreen,
                        modifier = Modifier.width(40.dp)
                    )
                    Text(text = g.icon, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = g.player?.takeIf { it.isNotBlank() }
                                ?: g.team?.name
                                ?: if (isHome) match.homeTeam?.name ?: "Hazai"
                                else match.awayTeam?.name ?: "Vendég",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = g.typeLabel + if (isHome) " · hazai" else " · vendég",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CardsSummaryRow(
    label: String,
    homeValue: Int,
    awayValue: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = homeValue.toString(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = label,
            modifier = Modifier.weight(1.4f),
            textAlign = TextAlign.Center,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = awayValue.toString(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun TimelineEventRow(
    event: MatchEventModel,
    isHome: Boolean,
    scoreAfter: String?,
    homeName: String,
    awayName: String
) {
    val accent = when (event.type) {
        "Goal", "Penalty" -> AccentGreen
        "Own Goal" -> AccentGold
        "Yellow Card" -> Color(0xFFFFD600)
        "Red Card" -> Color(0xFFFF5252)
        "Substitution" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val title = event.player?.takeIf { it.isNotBlank() }
        ?: event.team?.name
        ?: if (isHome) homeName else awayName
    val subtitle = buildString {
        append(event.typeLabel)
        if (!event.assist.isNullOrBlank()) append(" · gólpassz: ${event.assist}")
        if (!event.substituted.isNullOrBlank()) append(" · ${event.substituted}")
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Home side
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (isHome) {
                EventChip(
                    title = title,
                    subtitle = subtitle,
                    scoreAfter = scoreAfter,
                    accent = accent,
                    alignEnd = true
                )
            }
        }

        // Center spine
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(52.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f))
                    .border(1.5.dp, accent.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = event.icon, fontSize = 14.sp)
            }
            Text(
                text = event.time?.let { "$it'" } ?: "–",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Away side
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (!isHome) {
                EventChip(
                    title = title,
                    subtitle = subtitle,
                    scoreAfter = scoreAfter,
                    accent = accent,
                    alignEnd = false
                )
            }
        }
    }
}

@Composable
private fun EventChip(
    title: String,
    subtitle: String,
    scoreAfter: String?,
    accent: Color,
    alignEnd: Boolean
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        if (scoreAfter != null) {
            Text(
                text = scoreAfter,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGreen
            )
        }
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatsTab(
    statistics: List<TeamStatistics>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        if (statistics.isEmpty()) {
            Text(
                text = "Nincs elérhető statisztika ehhez a meccshez",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        val homeStats = statistics.getOrNull(0)?.statistics.orEmpty()
        val awayStats = statistics.getOrNull(1)?.statistics.orEmpty()
        val names = (
            homeStats.mapNotNull { it.displayName ?: it.name } +
                awayStats.mapNotNull { it.displayName ?: it.name }
            ).distinct()

        names.forEach { name ->
            val homeRaw = homeStats.find { (it.displayName ?: it.name) == name }?.value
            val awayRaw = awayStats.find { (it.displayName ?: it.name) == name }?.value
            val homeNum = parseStatNumber(homeRaw)
            val awayNum = parseStatNumber(awayRaw)
            StatBarRow(
                label = huStatName(name),
                homeValue = homeRaw?.toString() ?: "–",
                awayValue = awayRaw?.toString() ?: "–",
                homeNum = homeNum,
                awayNum = awayNum
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StatBarRow(
    label: String,
    homeValue: String,
    awayValue: String,
    homeNum: Float?,
    awayNum: Float?
) {
    val h = homeNum ?: 0f
    val a = awayNum ?: 0f
    val total = (h + a).coerceAtLeast(0.001f)
    val homeFrac = h / total
    val awayFrac = a / total
    val homeWins = h >= a

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = homeValue,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                fontWeight = if (homeWins) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (homeWins) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                modifier = Modifier.weight(1.6f),
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = awayValue,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
                fontWeight = if (!homeWins) FontWeight.Bold else FontWeight.Medium,
                fontSize = 14.sp,
                color = if (!homeWins) AccentGreen else MaterialTheme.colorScheme.onSurface
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .weight(homeFrac.coerceAtLeast(0.02f))
                    .fillMaxSize()
                    .background(
                        if (homeWins) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    )
            )
            Box(
                modifier = Modifier
                    .weight(awayFrac.coerceAtLeast(0.02f))
                    .fillMaxSize()
                    .background(
                        if (!homeWins) AccentGreen
                        else AccentGreen.copy(alpha = 0.45f)
                    )
            )
        }
    }
}

private fun parseStatNumber(value: Any?): Float? {
    return when (value) {
        null -> null
        is Number -> value.toFloat()
        is String -> value.replace("%", "").replace(",", ".").toFloatOrNull()
        else -> value.toString().replace("%", "").toFloatOrNull()
    }
}

private fun huStatName(name: String): String =
    when (name.lowercase()) {
        "ball possession", "possession" -> "Labdabirtoklás"
        "total shots", "shots" -> "Lövések"
        "shots on goal", "shots on target" -> "Kapura lövés"
        "shots off goal", "shots off target" -> "Kapu mellé"
        "blocked shots" -> "Blokkolt lövés"
        "corner kicks", "corners" -> "Szögletek"
        "offsides" -> "Les"
        "fouls" -> "Szabálytalanságok"
        "yellow cards" -> "Sárga lapok"
        "red cards" -> "Piros lapok"
        "goalkeeper saves", "saves" -> "Védések"
        "total passes", "passes" -> "Passzok"
        "passes accurate", "accurate passes" -> "Pontos passz"
        "expected goals", "xg" -> "xG"
        "attacks" -> "Támadások"
        "dangerous attacks" -> "Veszélyes támadások"
        "free kicks" -> "Szabadrúgások"
        "throw-ins", "throw ins" -> "Bedobások"
        "goal kicks" -> "Kirúgások"
        else -> name
    }

@Composable
private fun LineupTab(
    lineups: MatchLineups?,
    match: MatchModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val homeEmpty = lineups?.home?.initialLineup.isNullOrEmpty()
        val awayEmpty = lineups?.away?.initialLineup.isNullOrEmpty()

        if (lineups == null || (homeEmpty && awayEmpty)) {
            LineupEmptyState()
            return@Column
        }

        TeamLineupBlock(
            teamName = match.homeTeam?.name ?: "Hazai",
            lineup = lineups.home
        )
        Spacer(modifier = Modifier.height(20.dp))
        TeamLineupBlock(
            teamName = match.awayTeam?.name ?: "Vendég",
            lineup = lineups.away
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun LineupEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text("📋", fontSize = 32.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Nincs felállás ehhez a meccshez",
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Az API gyakran nem ad kezdőcsapatot alsóbb ligákban, vagy csak a kezdés előtt ~30 perccel.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TeamLineupBlock(
    teamName: String,
    lineup: TeamLineup?
) {
    val formation = lineup?.formation
    val title = if (formation.isNullOrBlank()) teamName else "$teamName · $formation"

    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    Spacer(modifier = Modifier.height(8.dp))

    if (lineup?.initialLineup.isNullOrEmpty()) {
        Text(
            text = "Nincs kezdő adat",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    } else {
        lineup?.initialLineup?.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row.forEach { player ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Column {
                            val number = player.number?.toString() ?: ""
                            val playerName = player.name ?: ""
                            Text(
                                text = listOf(number, playerName).filter { it.isNotBlank() }.joinToString(" "),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!player.position.isNullOrBlank()) {
                                Text(
                                    text = player.position,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (!lineup?.substitutes.isNullOrEmpty()) {

        Spacer(
            modifier = Modifier.height(8.dp)
        )

        Text(
            text = "Pad",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        lineup?.substitutes?.forEach { player ->

            val number =
                player.number
                    ?.toString()
                    ?: ""

            val playerName =
                player.name ?: ""

            Text(
                text =
                    (
                        number +
                            " " +
                            playerName
                        ).trim(),

                fontSize = 12.sp,

                modifier =
                    Modifier.padding(
                        vertical = 2.dp
                    )
            )
        }
    }
}

@Composable
private fun H2HTab(
    h2h: List<MatchModel>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)
    ) {

        Text(
            text = "Korabbi talalkozok",
            fontWeight =
                FontWeight.Bold,
            fontSize = 16.sp
        )

        Spacer(
            modifier =
                Modifier.height(10.dp)
        )

        if (h2h.isEmpty()) {

            Text(
                text =
                    "Nincs elerheto H2H adat",

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

        } else {

            h2h.forEach { match ->

                val scoreText =
                    if (match.hasScore) {

                        match.homeScoreDisplay +
                            ":" +
                            match.awayScoreDisplay

                    } else {

                        "-"
                    }

                val matchLabel =
                    (
                        match.homeTeam?.name
                            ?: "?"
                        ) +
                        " - " +
                        (
                            match.awayTeam?.name
                                ?: "?"
                            )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(10.dp)
                        )
                        .background(
                            MaterialTheme
                                .colorScheme
                                .surface
                        )
                        .padding(12.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = matchLabel,

                        modifier =
                            Modifier.weight(1f),

                        fontSize = 13.sp,

                        maxLines = 1,

                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Text(
                        text = scoreText,

                        fontWeight =
                            FontWeight.Bold,

                        fontSize = 14.sp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .primary
                    )
                }

                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(24.dp)
        )
    }
}
