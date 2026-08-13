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
import java.util.Locale

private enum class DetailTab {
    OVERVIEW,
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

    var oddsItems by remember(match.id) {
        mutableStateOf<List<BookmakerOdd>>(emptyList())
    }

    var lineups by remember(match.id) {
        mutableStateOf<MatchLineups?>(null)
    }

    var statistics by remember(match.id) {
        mutableStateOf<List<TeamStatistics>>(emptyList())
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

                oddsItems = try {
                    val response =
                        apiService.getOdds(
                            apiKey,
                            match.id,
                            oddsType =
                                if (match.isLive) {
                                    "live"
                                } else {
                                    "prematch"
                                }
                        )

                    response
                        .data
                        ?.firstOrNull()
                        ?.odds
                        ?: emptyList()

                } catch (_: Exception) {

                    try {
                        val response =
                            apiService.getOdds(
                                apiKey,
                                match.id,
                                oddsType = "prematch"
                            )

                        response
                            .data
                            ?.firstOrNull()
                            ?.odds
                            ?: emptyList()

                    } catch (_: Exception) {
                        emptyList()
                    }
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
            }
        }

        isLoading = false
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
                    Text(
                        text = "<-",
                        fontSize = 22.sp,
                        modifier = Modifier
                            .clickable {
                                onBackClick()
                            }
                            .padding(horizontal = 12.dp)
                    )
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

            MatchScoreHeader(match)

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
                                            "Osszegzes"

                                        DetailTab.STATS ->
                                            "Stat"

                                        DetailTab.LINEUP ->
                                            "Felallas"

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
                        OverviewTab(events)

                    DetailTab.STATS ->
                        StatsTab(statistics)

                    DetailTab.LINEUP ->
                        LineupTab(
                            lineups,
                            match
                        )

                    DetailTab.ODDS ->
                        OddsTab(
                            odds = oddsItems,
                            match = match,
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
    match: MatchModel
) {
    val borderColor =
        if (match.isLive) {
            AccentGreen.copy(alpha = 0.55f)
        } else {
            MaterialTheme
                .colorScheme
                .outline
                .copy(alpha = 0.3f)
        }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .border(
                1.5.dp,
                borderColor,
                RoundedCornerShape(16.dp)
            ),

        shape = RoundedCornerShape(16.dp),

        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                if (match.isLive) {

                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(AccentGreen)
                    )

                    Spacer(
                        modifier = Modifier.width(6.dp)
                    )
                }

                Text(
                    text =
                        if (match.isLive) {
                            "ELO " +
                                (match.liveMinuteLabel ?: "")
                        } else {
                            match.statusLabel
                        },

                    fontSize = 13.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        if (match.isLive) {
                            AccentGreen
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
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

                        fontSize = 28.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            if (match.isLive) {
                                AccentGreen
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
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
            }
        }
    }
}

@Composable
private fun TeamBlock(
    team: TeamModel?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        if (!team?.logo.isNullOrBlank()) {

            AsyncImage(
                model = team?.logo,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )

        } else {

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme
                            .colorScheme
                            .surfaceVariant
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Text(
                    text = "o",
                    fontSize = 22.sp
                )
            }
        }

        Spacer(
            modifier = Modifier.height(6.dp)
        )

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

@Composable
private fun OverviewTab(
    events: List<MatchEventModel>
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
            text = "Esemenyek",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        if (events.isEmpty()) {

            Text(
                text = "Meg nincsenek esemenyek",
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                fontSize = 14.sp
            )

        } else {

            events.forEach { event ->

                EventRow(event)

                Spacer(
                    modifier = Modifier.height(6.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun EventRow(
    ev: MatchEventModel
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(10.dp)
            )
            .background(
                MaterialTheme.colorScheme.surface
            )
            .padding(
                horizontal = 12.dp,
                vertical = 8.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = ev.time ?: "-",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .primary,

            modifier = Modifier.width(40.dp)
        )

        Text(
            text =
                when (ev.type) {
                    "Goal",
                    "Penalty" -> "G"

                    "Own Goal" -> "OG"

                    "Yellow Card" -> "S"

                    "Red Card" -> "P"

                    "Substitution" -> "Cs"

                    else -> "*"
                },

            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,

            modifier =
                Modifier.padding(end = 8.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text =
                    ev.player
                        ?: ev.type
                        ?: "Esemeny",

                fontSize = 13.sp,
                fontWeight =
                    FontWeight.Medium
            )

            if (!ev.assist.isNullOrBlank()) {

                Text(
                    text =
                        "Golpassz: " +
                            ev.assist,

                    fontSize = 11.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )
            }
        }

        Text(
            text =
                when (ev.type) {
                    "Goal" ->
                        "Gol"

                    "Penalty" ->
                        "Bunteto"

                    "Own Goal" ->
                        "OnGol"

                    "Yellow Card" ->
                        "Sarga"

                    "Red Card" ->
                        "Piros"

                    "Substitution" ->
                        "Csere"

                    "Missed Penalty" ->
                        "Kihagyott 11-es"

                    else ->
                        ev.type ?: ""
                },

            fontSize = 11.sp,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun StatsTab(
    statistics: List<TeamStatistics>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)
    ) {

        if (statistics.isEmpty()) {

            Text(
                text =
                    "Nincs elerheto statisztika ehhez a meccshez",

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )

            return@Column
        }

        val homeStats =
            statistics
                .getOrNull(0)
                ?.statistics
                .orEmpty()

        val awayStats =
            statistics
                .getOrNull(1)
                ?.statistics
                .orEmpty()

        val names =
            (
                homeStats.mapNotNull {
                    it.displayName ?: it.name
                } +
                    awayStats.mapNotNull {
                        it.displayName ?: it.name
                    }
                )
                .distinct()

        names.forEach { name ->

            val homeValue =
                homeStats
                    .find {
                        (it.displayName ?: it.name) == name
                    }
                    ?.value
                    ?.toString()
                    ?: "-"

            val awayValue =
                awayStats
                    .find {
                        (it.displayName ?: it.name) == name
                    }
                    ?.value
                    ?.toString()
                    ?: "-"

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = homeValue,

                    modifier =
                        Modifier.weight(1f),

                    textAlign =
                        TextAlign.End,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize = 14.sp
                )

                Text(
                    text = huStatName(name),

                    modifier =
                        Modifier.weight(1.4f),

                    textAlign =
                        TextAlign.Center,

                    fontSize = 12.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                )

                Text(
                    text = awayValue,

                    modifier =
                        Modifier.weight(1f),

                    textAlign =
                        TextAlign.Start,

                    fontWeight =
                        FontWeight.Bold,

                    fontSize = 14.sp
                )
            }

            Divider(
                color =
                    MaterialTheme
                        .colorScheme
                        .outline
                        .copy(alpha = 0.2f)
            )
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

private fun huStatName(
    name: String
): String =
    when (name.lowercase()) {

        "ball possession",
        "possession" ->
            "Labdabirtoklas"

        "total shots",
        "shots" ->
            "Lovesek"

        "shots on goal",
        "shots on target" ->
            "Kapura loves"

        "shots off goal",
        "shots off target" ->
            "Kapu melle"

        "blocked shots" ->
            "Blokkolt loves"

        "corner kicks",
        "corners" ->
            "Szogletek"

        "offsides" ->
            "Les"

        "fouls" ->
            "Szabalysertesek"

        "yellow cards" ->
            "Sarga lapok"

        "red cards" ->
            "Piros lapok"

        "goalkeeper saves",
        "saves" ->
            "Vedesek"

        "total passes",
        "passes" ->
            "Passzok"

        "passes accurate",
        "accurate passes" ->
            "Pontos passz"

        "expected goals",
        "xg" ->
            "xG"

        else ->
            name
    }

@Composable
private fun LineupTab(
    lineups: MatchLineups?,
    match: MatchModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)
    ) {

        if (lineups == null) {

            Text(
                text =
                    "A felallas meg nem elerheto - altalaban 30 perccel a kezdes elott",

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                textAlign =
                    TextAlign.Center,

                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
            )

            return@Column
        }

        TeamLineupBlock(
            teamName =
                match.homeTeam?.name
                    ?: "Hazai",

            lineup = lineups.home
        )

        Spacer(
            modifier = Modifier.height(20.dp)
        )

        TeamLineupBlock(
            teamName =
                match.awayTeam?.name
                    ?: "Vendeg",

            lineup = lineups.away
        )

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

@Composable
private fun TeamLineupBlock(
    teamName: String,
    lineup: TeamLineup?
) {
    val formation =
        lineup?.formation

    val title =
        if (formation.isNullOrBlank()) {
            teamName
        } else {
            "$teamName ($formation)"
        }

    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
    )

    Spacer(
        modifier = Modifier.height(8.dp)
    )

    if (lineup?.initialLineup.isNullOrEmpty()) {

        Text(
            text = "Nincs kezdo",
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            fontSize = 13.sp
        )

    } else {

        lineup?.initialLineup?.forEach { row ->

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),

                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                row.forEach { player ->

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(
                                RoundedCornerShape(8.dp)
                            )
                            .background(
                                MaterialTheme
                                    .colorScheme
                                    .surface
                            )
                            .padding(6.dp)
                    ) {

                        Column {

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

                                fontWeight =
                                    FontWeight.Medium,

                                maxLines = 1,

                                overflow =
                                    TextOverflow.Ellipsis
                            )

                            if (
                                !player.position
                                    .isNullOrBlank()
                            ) {

                                Text(
                                    text =
                                        player.position,

                                    fontSize = 10.sp,

                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant
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

/*
 * ============================================================
 * ODDS TAB - CRASH SAFE VERSION
 * ============================================================
 *
 * FONTOS:
 *
 * A korábbi verzióban az OddsTab() belsejében egymásba ágyazott
 * Compose key() blokkok voltak:
 *
 * key("market...")
 *     key("bookmaker...")
 *         key("odds...")
 *
 * A képernyőn jelentkező hiba:
 *
 * java.lang.IndexOutOfBoundsException:
 * Index -1 out of bounds for length 0
 *
 * android...compose.runtime.Stack.pop()
 *
 * Ezért ebben a verzióban teljesen eltávolítottuk a key()
 * használatát ebből a dinamikus odds-listából.
 *
 * Ugyanígy nincs remember(odds) sem.
 *
 * Az oddslista egyszerű Kotlin adatstruktúrából épül fel,
 * majd stateless Compose UI-ként jelenik meg.
 *
 * Ez a rész szándékosan egyszerűbb és stabilabb.
 * ============================================================
 */

@Composable
private fun OddsTab(
    odds: List<BookmakerOdd>,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    /*
     * Az API válaszát megtisztítjuk.
     *
     * Kiszűrjük:
     * - null odds
     * - üres selection
     * - 0 vagy negatív odds
     * - NaN
     * - Infinity
     * - üres bookmaker
     * - üres market
     */

    val validOdds =
        odds.mapNotNull { bookmaker ->

            val market =
                bookmaker.market
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }
                    ?: "Egyeb"

            val bookmakerName =
                bookmaker.bookmakerName
                    ?.trim()
                    ?.takeIf {
                        it.isNotEmpty()
                    }
                    ?: "Bookmaker"

            val entries =
                bookmaker.values
                    .orEmpty()
                    .mapNotNull { value ->

                        val odd =
                            value.odd
                                ?: return@mapNotNull null

                        val selection =
                            value.value
                                ?.trim()
                                ?.takeIf {
                                    it.isNotEmpty()
                                }
                                ?: return@mapNotNull null

                        if (
                            !odd.isFinite() ||
                            odd <= 0.0
                        ) {
                            return@mapNotNull null
                        }

                        OddEntry(
                            selection = selection,
                            odd = odd
                        )
                    }

            if (entries.isEmpty()) {

                null

            } else {

                OddsGroup(
                    market = market,
                    bookmakerName =
                        bookmakerName,
                    entries = entries
                )
            }
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(16.dp)
    ) {

        /*
         * Ha nincs odds, NEM próbálunk üres Row-t,
         * üres weight-et vagy dinamikus key-t létrehozni.
         */

        if (validOdds.isEmpty()) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        vertical = 32.dp
                    ),

                contentAlignment =
                    Alignment.Center
            ) {

                Column(
                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            "Nincs elerheto odds",

                        fontSize = 16.sp,

                        fontWeight =
                            FontWeight.Bold,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurface
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "Az odds adat jelenleg nem erheto el ehhez a merkozeshez.",

                        fontSize = 13.sp,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,

                        textAlign =
                            TextAlign.Center
                    )
                }
            }

            return@Column
        }

        Text(
            text =
                "Koppints egy oddsra a szelvenyhez adashoz",

            fontSize = 12.sp,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,

            modifier =
                Modifier.padding(
                    bottom = 12.dp
                )
        )

        /*
         * Fontos:
         *
         * groupBy() csak Kotlin oldalon csoportosít.
         * Nem hoz létre Compose key/group struktúrát.
         */

        val groupsByMarket =
            validOdds.groupBy {
                it.market
            }

        /*
         * Piacok megjelenítése.
         *
         * NINCS:
         * key(...)
         *
         * NINCS:
         * LazyColumn key
         *
         * NINCS:
         * remember(...)
         */

        groupsByMarket.forEach {
                (market, groups) ->

            Text(
                text =
                    huMarket(market),

                fontWeight =
                    FontWeight.Bold,

                fontSize = 14.sp,

                modifier =
                    Modifier.padding(
                        top = 8.dp,
                        bottom = 6.dp
                    )
            )

            groups.forEach { group ->

                Text(
                    text =
                        group.bookmakerName,

                    fontSize = 11.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    modifier =
                        Modifier.padding(
                            bottom = 4.dp
                        )
                )

                /*
                 * Az oddsok Row-ban vannak.
                 *
                 * A weight() kizárólag layout művelet.
                 * Nem használ Compose group/key rendszert.
                 */

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    group.entries.forEach { entry ->

                        val selectionLower =
                            entry.selection
                                .trim()
                                .lowercase()

                        /*
                         * A címkét többféle API-formátumhoz
                         * is biztonságosan meghatározzuk.
                         */

                        val label =
                            when {

                                selectionLower == "home" ||
                                selectionLower == "1" ->

                                    match.homeTeam
                                        ?.name
                                        ?.take(12)
                                        ?: "1"

                                selectionLower == "away" ||
                                selectionLower == "2" ->

                                    match.awayTeam
                                        ?.name
                                        ?.take(12)
                                        ?: "2"

                                selectionLower == "draw" ||
                                selectionLower == "x" ->

                                    "X"

                                else ->

                                    entry.selection
                                        .take(12)
                            }

                        /*
                         * Nincs key() körülötte.
                         */

                        OddsChip(
                            label = label,

                            odd = entry.odd,

                            onClick = {

                                val selection =
                                    BetSlipSelection(

                                        matchId =
                                            match.id,

                                        homeName =
                                            match.homeTeam
                                                ?.name
                                                ?: "Hazai",

                                        awayName =
                                            match.awayTeam
                                                ?.name
                                                ?: "Vendeg",

                                        leagueName =
                                            match.leagueDisplayName,

                                        market =
                                            market,

                                        selection =
                                            entry.selection,

                                        odd =
                                            entry.odd,

                                        bookmakerName =
                                            group.bookmakerName
                                    )

                                onAddToSlip(
                                    selection
                                )
                            },

                            modifier =
                                Modifier.weight(1f)
                        )
                    }
                }

                Spacer(
                    modifier =
                        Modifier.height(8.dp)
                )
            }
        }

        Spacer(
            modifier = Modifier.height(24.dp)
        )
    }
}

private data class OddsGroup(
    val market: String,
    val bookmakerName: String,
    val entries: List<OddEntry>
)

private data class OddEntry(
    val selection: String,
    val odd: Double
)

private fun huMarket(
    market: String
): String =
    when (market.lowercase()) {

        "full time result",
        "match result",
        "1x2" ->
            "Vegeredmeny (1X2)"

        "double chance" ->
            "Kettos esely"

        "both teams to score",
        "btts" ->
            "Mindket csapat szerez golt"

        "total goals",
        "goals over/under",
        "over/under" ->
            "Golok szama"

        "draw no bet" ->
            "Dontetlennel visszajar"

        "first half result" ->
            "1. felido eredmenye"

        "second half result" ->
            "2. felido eredmenye"

        "correct score" ->
            "Pontos eredmeny"

        "team total" ->
            "Csapat golok"

        else ->
            market
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
            .clip(
                RoundedCornerShape(10.dp)
            )
            .background(
                MaterialTheme
                    .colorScheme
                    .surface
            )
            .border(
                1.dp,

                MaterialTheme
                    .colorScheme
                    .outline
                    .copy(alpha = 0.4f),

                RoundedCornerShape(10.dp)
            )
            .clickable(
                onClick = onClick
            )
            .padding(
                vertical = 10.dp,
                horizontal = 6.dp
            ),

        horizontalAlignment =
            Alignment.CenterHorizontally
    ) {

        Text(
            text = label,

            fontSize = 11.sp,

            maxLines = 1,

            overflow =
                TextOverflow.Ellipsis,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )

        Spacer(
            modifier =
                Modifier.height(2.dp)
        )

        Text(
            text =
                String.format(
                    Locale.US,
                    "%.2f",
                    odd
                ),

            fontSize = 16.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme
                    .colorScheme
                    .primary
        )
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
