package com.livefutar.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.animateItemPlacement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import com.livefutar.app.util.Haptics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livefutar.app.data.ApiKeyManager
import com.livefutar.app.data.BestOdds
import com.livefutar.app.data.FootballApiService
import com.livefutar.app.data.OddsCache
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.MatchCard
import com.livefutar.app.ui.components.PullRefreshBox
import com.livefutar.app.ui.theme.AccentGold
import com.livefutar.app.ui.theme.AccentGreen
import com.livefutar.app.util.DateUtils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone




/*
 * ============================================================
 * LIVE FUTÁR
 * HomeScreen / Meccsek
 *
 * Változás:
 * - ország ABC
 * - bajnokság ABC
 * - Friendlies mindig a lista végén
 * - bajnokságok nyitható / zárható
 * - 3h / 6h / 9h valódi szűrő
 * ============================================================
 */

private data class LeagueGroup(
    val key: String,
    val displayName: String,
    val countryName: String,
    val matches: List<MatchModel>
)

private enum class TimeWindowFilter(val hours: Int) {
    NONE(0),
    THREE(3),
    SIX(6),
    NINE(9)
}

@OptIn(ExperimentalMaterial3Api::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    matches: List<MatchModel>,
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    favoriteTeamIds: Set<Long>,
    favoriteLeagueIds: Set<Long>,
    onToggleTeamFavorite: (Long) -> Unit,
    onToggleLeagueFavorite: (Long) -> Unit,
    showOnlyFavorites: Boolean,
    onToggleShowOnlyFavorites: () -> Unit,
    showOnlyLive: Boolean,
    onToggleShowOnlyLive: () -> Unit,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onMatchClick: (MatchModel) -> Unit,
    onStandingsClick: (MatchModel) -> Unit
) {

    var filtersExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    val collapsedLeagues = remember {
        mutableStateMapOf<String, Boolean>()
    }

    var selectedTimeWindow by rememberSaveable {
        mutableStateOf(TimeWindowFilter.NONE)
    }

    var searchExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var showOdds by rememberSaveable {
        mutableStateOf(false)
    }

    val context = LocalContext.current
    val apiService = remember { FootballApiService.create() }

    /*
     * ============================================================
     * SZŰRÉS
     * ============================================================
     */

    val filteredMatches = remember(
        matches,
        favoriteTeamIds,
        favoriteLeagueIds,
        showOnlyFavorites,
        showOnlyLive,
        selectedTimeWindow,
        searchQuery
    ) {

        var result = matches

        /*
         * KEDVENCEK
         */
        if (showOnlyFavorites) {
            result = result.filter { match ->

                (
                    match.league?.id != null &&
                        favoriteLeagueIds.contains(
                            match.league.id
                        )
                ) ||
                (
                    match.homeTeam?.id != null &&
                        favoriteTeamIds.contains(
                            match.homeTeam.id
                        )
                ) ||
                (
                    match.awayTeam?.id != null &&
                        favoriteTeamIds.contains(
                            match.awayTeam.id
                        )
                )
            }
        }

        /*
         * ÉLŐ
         */
        if (showOnlyLive) {
            result = result.filter {
                it.isLive
            }
        }

        /*
         * 3 / 6 / 9 ÓRÁS SZŰRŐ
         *
         * Csak a még el nem kezdődött meccsekre vonatkozik.
         */
        if (selectedTimeWindow != TimeWindowFilter.NONE) {

            val nowMillis =
                System.currentTimeMillis()

            val untilMillis =
                nowMillis +
                    selectedTimeWindow.hours.toLong() *
                    60L *
                    60L *
                    1000L

            result = result.filter { match ->

                if (!match.isNotStarted) {
                    false
                } else {

                    val kickoff =
                        match.kickoffMillis

                    kickoff != null &&
                        kickoff >= nowMillis &&
                        kickoff <= untilMillis
                }
            }
        }

        /*
         * KERESÉS (csapat vagy bajnokság neve alapján)
         */
        if (searchQuery.isNotBlank()) {

            val normalizedQuery =
                normalizeForSort(searchQuery)

            result = result.filter { match ->

                val haystack =
                    normalizeForSort(
                        listOfNotNull(
                            match.homeTeam?.displayName,
                            match.awayTeam?.displayName,
                            match.leagueDisplayName
                        ).joinToString(" ")
                    )

                haystack.contains(normalizedQuery)
            }
        }

        /*
         * A meccseket idő szerint rendezzük.
         */
        result.sortedWith(
            compareBy<MatchModel> {

                parseMatchDate(
                    it.date
                )?.time ?: Long.MAX_VALUE

            }.thenBy {

                it.id
            }
        )
    }

    /*
     * ============================================================
     * ÉLŐ MECCSEK SZÁMA
     * ============================================================
     */

    val liveCount =
        matches.count {
            it.isLive
        }

    /*
     * Kedvenc csapatok meccsei a szűrt listából – a lista tetején.
     * (Csak ha nem „csak kedvencek” mód van, különben duplikálódna.)
     */
    val favoriteMatchesTop =
        if (!showOnlyFavorites && searchQuery.isBlank()) {
            filteredMatches
                .filter { match ->
                    val homeId = match.homeTeam?.id
                    val awayId = match.awayTeam?.id
                    (homeId != null && homeId in favoriteTeamIds) ||
                        (awayId != null && awayId in favoriteTeamIds)
                }
                .sortedWith(
                    compareByDescending<MatchModel> { it.isLive }
                        .thenBy { parseMatchDate(it.date)?.time ?: Long.MAX_VALUE }
                        .thenBy { it.id }
                )
        } else {
            emptyList()
        }

    /*
     * ============================================================
     * BAJNOKSÁGOK CSOPORTOSÍTÁSA
     * ============================================================
     *
     * FONTOS:
     *
     * 1. Friendlies -> mindig utolsó
     * 2. Ország -> ABC
     * 3. Bajnokság -> ABC
     */

    val matchesForGrouping =
        if (favoriteMatchesTop.isNotEmpty()) {
            val favIds = favoriteMatchesTop.map { it.id }.toSet()
            filteredMatches.filter { it.id !in favIds }
        } else {
            filteredMatches
        }

    val grouped =
        matchesForGrouping
            .groupBy { match ->
                leagueKey(match)
            }
            .map { (key, list) ->

                val first =
                    list.firstOrNull()

                val displayName =
                    first?.leagueDisplayName
                        ?: "Egyéb mérkőzések"

                LeagueGroup(
                    key = key,
                    displayName = displayName,
                    countryName =
                        extractCountryName(
                            displayName
                        ),
                    matches =
                        list.sortedWith(
                            compareBy<MatchModel> {

                                parseMatchDate(
                                    it.date
                                )?.time
                                    ?: Long.MAX_VALUE

                            }.thenBy {

                                it.id
                            }
                        )
                )
            }
            .sortedWith(

                compareBy<LeagueGroup> {

                    /*
                     * Friendlies mindig utolsó.
                     */
                    if (
                        isFriendlyLeague(
                            it.displayName
                        )
                    ) {
                        1
                    } else {
                        0
                    }

                }.thenBy {

                    /*
                     * ORSZÁG ABC
                     */
                    normalizeForSort(
                        it.countryName
                    )

                }.thenBy {

                    /*
                     * BAJNOKSÁG ABC
                     */
                    normalizeForSort(
                        it.displayName
                    )
                }
            )

    /*
     * ============================================================
     * UI
     * ============================================================
     */

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Icon(
                            imageVector = Icons.Filled.SportsSoccer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Live Futár",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (liveCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            LiveBadge(count = liveCount)
                        }
                    }
                },

                actions = {
                    IconButton(onClick = {
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) searchQuery = ""
                    }) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "Keresés",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val haptic = LocalHapticFeedback.current
                    IconButton(
                        onClick = {
                            Haptics.tick(haptic)
                            onRefresh()
                        },
                        enabled = !isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Frissítés",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    var menuOpen by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Több",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (showOnlyLive) "✓ Csak élő" else "Csak élő"
                                    )
                                },
                                onClick = {
                                    onToggleShowOnlyLive()
                                    menuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (showOdds) "✓ Odds mutatása" else "Odds mutatása"
                                    )
                                },
                                onClick = {
                                    showOdds = !showOdds
                                    menuOpen = false
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (showOnlyFavorites) "✓ Csak kedvencek"
                                        else "Csak kedvencek"
                                    )
                                },
                                onClick = {
                                    onToggleShowOnlyFavorites()
                                    menuOpen = false
                                }
                            )
                        }
                    }
                },

                                colors =
                    TopAppBarDefaults
                        .topAppBarColors(
                            containerColor =
                                MaterialTheme
                                    .colorScheme
                                    .surface,
                            titleContentColor =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                        )
            )
        }

    ) { paddingValues ->

        PullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme
                            .colorScheme
                            .background
                    ),

            verticalArrangement =
                Arrangement.spacedBy(
                    1.dp
                )
        ) {

            /*
             * DÁTUM
             */
            item {

                DateStrip(
                    selectedDate =
                        selectedDate,
                    onDateSelected =
                        onDateSelected
                )
            }

            /*
             * KERESŐ MEZŐ
             */
            item {
                AnimatedVisibility(
                    visible = searchExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        placeholder = {
                            Text("Csapat vagy bajnokság keresése…", fontSize = 13.sp)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                }
            }

            /*
             * 3 / 6 / 9 ÓRÁS SZŰRŐ
             */
            item {

                TimeWindowBar(
                    liveSelected =
                        showOnlyLive,
                    timeWindow =
                        selectedTimeWindow,
                    liveCount =
                        liveCount,

                    onLiveClick = {

                        selectedTimeWindow =
                            TimeWindowFilter.NONE

                        onToggleShowOnlyLive()
                    },

                    onTimeWindowClick = {
                        window ->

                        if (showOnlyLive) {
                            onToggleShowOnlyLive()
                        }

                        selectedTimeWindow =
                            if (
                                selectedTimeWindow ==
                                    window
                            ) {
                                TimeWindowFilter.NONE
                            } else {
                                window
                            }
                    }
                )
            }

            /*
             * DÁTUM FELIRAT
             */
            item {

                Text(
                    text =
                        DateUtils.fullDateLabel(
                            selectedDate
                        ),
                    fontSize = 14.sp,
                    fontWeight =
                        FontWeight.SemiBold,
                    modifier =
                        Modifier.padding(
                            horizontal = 14.dp,
                            vertical = 6.dp
                        ),
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            /*
             * SZŰRŐ FEJLÉC
             */
            item {

                FilterHeader(
                    expanded =
                        filtersExpanded,
                    activeCount =
                        activeFilterCount(
                            showOnlyLive,
                            showOnlyFavorites,
                            selectedTimeWindow
                        ),
                    onClick = {
                        filtersExpanded =
                            !filtersExpanded
                    }
                )
            }

            /*
             * SZŰRŐPANEL
             */
            if (filtersExpanded) {

                item {

                    FilterPanel(
                        showOnlyLive =
                            showOnlyLive,
                        onToggleLive =
                            onToggleShowOnlyLive,
                        showOnlyFavorites =
                            showOnlyFavorites,
                        onToggleFavorites =
                            onToggleShowOnlyFavorites,
                        onClear = {

                            selectedTimeWindow =
                                TimeWindowFilter.NONE

                            if (showOnlyLive) {
                                onToggleShowOnlyLive()
                            }

                            if (showOnlyFavorites) {
                                onToggleShowOnlyFavorites()
                            }
                        }
                    )
                }
            }

            /*
             * NINCS TALÁLAT
             */
            if (
                filteredMatches.isEmpty()
            ) {

                item {

                    EmptyState(
                        showOnlyFavorites =
                            showOnlyFavorites,
                        showOnlyLive =
                            showOnlyLive
                    )
                }

            } else {

                /*
                 * Kedvenceim – a lista tetején
                 */
                if (favoriteMatchesTop.isNotEmpty()) {
                    item(key = "fav_home_header") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "★",
                                fontSize = 14.sp,
                                color = AccentGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Kedvenceim",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AccentGold.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = favoriteMatchesTop.size.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentGold
                                )
                            }
                        }
                    }
                    items(
                        items = favoriteMatchesTop,
                        key = { "fav_home_${it.id}" }
                    ) { match ->
                        MatchCard(
                            modifier = Modifier.animateItemPlacement(),
                            match = match,
                            isHomeFavorite = match.homeTeam?.id in favoriteTeamIds,
                            isAwayFavorite = match.awayTeam?.id in favoriteTeamIds,
                            onToggleHomeFavorite = {
                                match.homeTeam?.id?.let(onToggleTeamFavorite)
                            },
                            onToggleAwayFavorite = {
                                match.awayTeam?.id?.let(onToggleTeamFavorite)
                            },
                            onClick = { onMatchClick(match) }
                        )
                    }
                    item(key = "fav_home_spacer") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                /*
                 * =================================================
                 * BAJNOKSÁGOK
                 * =================================================
                 */
                grouped.forEach { group ->

                    val first =
                        group.matches.firstOrNull()

                    val leagueId =
                        first?.league?.id

                    val isFavoriteLeague =
                        leagueId != null &&
                            favoriteLeagueIds
                                .contains(
                                    leagueId
                                )

                    val isCollapsed =
                        collapsedLeagues[
                            group.key
                        ] == true

                    /*
                     * FEJLÉC
                     */
                    item(
                        key =
                            "league_${group.key}"
                    ) {

                        LeagueHeader(
                            leagueDisplayName =
                                group.displayName,
                            leagueLogo =
                                first?.league?.logo,
                            countryLogo =
                                first?.country?.logo,
                            isFavorite =
                                isFavoriteLeague,
                            matchCount =
                                group.matches.size,
                            collapsed =
                                isCollapsed,

                            onToggleCollapsed = {

                                collapsedLeagues[
                                    group.key
                                ] =
                                    !isCollapsed
                            },

                            onToggleFavorite = {

                                leagueId?.let(
                                    onToggleLeagueFavorite
                                )
                            },

                            onStandingsClick = {

                                first?.let(
                                    onStandingsClick
                                )
                            }
                        )
                    }

                    /*
                     * MECCSEK
                     */
                    if (!isCollapsed) {

                        items(

                            items =
                                group.matches,

                            key = { match ->

                                "match_${match.id}"
                            }

                        ) { match ->

                            MatchCardWithOdds(

                                match =
                                    match,

                                showOdds = showOdds,

                                apiService = apiService,

                                context = context,

                                isHomeFavorite =
                                    match.homeTeam?.id != null &&
                                        favoriteTeamIds
                                            .contains(
                                                match.homeTeam.id
                                            ),

                                isAwayFavorite =
                                    match.awayTeam?.id != null &&
                                        favoriteTeamIds
                                            .contains(
                                                match.awayTeam.id
                                            ),

                                onToggleHomeFavorite = {

                                    match.homeTeam
                                        ?.id
                                        ?.let(
                                            onToggleTeamFavorite
                                        )
                                },

                                onToggleAwayFavorite = {

                                    match.awayTeam
                                        ?.id
                                        ?.let(
                                            onToggleTeamFavorite
                                        )
                                },

                                onClick = {

                                    onMatchClick(
                                        match
                                    )
                                }
                            )
                        }
                    }
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            18.dp
                        )
                )
            }
        } // LazyColumn
        } // PullRefreshBox
    }
}

/*
 * ============================================================
 * SEGÉDFÜGGVÉNYEK
 * ============================================================
 */

private fun activeFilterCount(
    showOnlyLive: Boolean,
    showOnlyFavorites: Boolean,
    timeWindow: TimeWindowFilter
): Int {

    var count = 0

    if (showOnlyLive) {
        count++
    }

    if (showOnlyFavorites) {
        count++
    }

    if (
        timeWindow !=
            TimeWindowFilter.NONE
    ) {
        count++
    }

    return count
}

/*
 * BAJNOKSÁG ID
 */
private fun leagueKey(
    match: MatchModel
): String {

    return match.league
        ?.id
        ?.toString()
        ?: "${match.country?.code.orEmpty()}_${match.leagueDisplayName}"
}

/*
 * ============================================================
 * ORSZÁGNÉV KINYERÉSE
 * ============================================================
 *
 * Az API jelenlegi megjelenítési formája:
 *
 * "Chile · Primera B"
 * "Franciaország · Ligue 2"
 * "Wales · Premier League"
 *
 * Ezért a "·" előtti részt használjuk országként.
 */

private fun extractCountryName(
    leagueDisplayName: String
): String {

    val separator =
        leagueDisplayName.indexOf("·")

    if (separator > 0) {

        return leagueDisplayName
            .substring(
                0,
                separator
            )
            .trim()
    }

    /*
     * Ha nincs ország + liga formátum,
     * próbáljuk meg a gyakori formákat.
     */
    return when {

        leagueDisplayName
            .contains(
                "Friendly",
                ignoreCase = true
            ) ->
            "ZZZ"

        leagueDisplayName
            .contains(
                "Barátságos",
                ignoreCase = true
            ) ->
            "ZZZ"

        else ->
            "Egyéb"
    }
}

/*
 * ============================================================
 * FRIENDLY FELISMERÉS
 * ============================================================
 */

private fun isFriendlyLeague(
    name: String
): Boolean {

    val value =
        name
            .trim()
            .lowercase(
                Locale.ROOT
            )

    return value.contains(
        "friendly"
    ) ||
        value.contains(
            "friendlies"
        ) ||
        value.contains(
            "barátságos"
        ) ||
        value.contains(
            "baratsagos"
        ) ||
        value.contains(
            "club friendlies"
        ) ||
        value.contains(
            "international friendlies"
        )
}

/*
 * ============================================================
 * ABC RENDEZÉS NORMALIZÁLÁSSAL
 * ============================================================
 */

private fun normalizeForSort(
    value: String
): String {

    return value
        .trim()
        .lowercase(
            Locale.ROOT
        )
        .replace(
            "á",
            "a"
        )
        .replace(
            "é",
            "e"
        )
        .replace(
            "í",
            "i"
        )
        .replace(
            "ó",
            "o"
        )
        .replace(
            "ö",
            "o"
        )
        .replace(
            "ő",
            "o"
        )
        .replace(
            "ú",
            "u"
        )
        .replace(
            "ü",
            "u"
        )
        .replace(
            "ű",
            "u"
        )
}

/*
 * ============================================================
 * DÁTUM PARSOLÁS
 * ============================================================
 */

private fun parseMatchDate(
    value: String?
): Date? {

    if (
        value.isNullOrBlank()
    ) {
        return null
    }

    val patterns =
        listOf(

            "yyyy-MM-dd'T'HH:mm:ssXXX",

            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",

            "yyyy-MM-dd'T'HH:mm:ss'Z'",

            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",

            "yyyy-MM-dd'T'HH:mm:ss"
        )

    for (pattern in patterns) {

        try {

            val formatter =
                SimpleDateFormat(
                    pattern,
                    Locale.US
                )

            if (
                pattern.contains(
                    "'Z'"
                )
            ) {

                formatter.timeZone =
                    TimeZone.getTimeZone(
                        "UTC"
                    )
            }

            return formatter.parse(
                value
            )

        } catch (
            _: ParseException
        ) {
            // következő formátum
        }
    }

    return null
}

/*
 * ============================================================
 * 3 / 6 / 9 ÓRÁS SÁV
 * ============================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeWindowBar(
    liveSelected: Boolean,
    timeWindow: TimeWindowFilter,
    liveCount: Int,
    onLiveClick: () -> Unit,
    onTimeWindowClick:
        (TimeWindowFilter) -> Unit
) {

    LazyRow(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                ),

        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
            ),

        contentPadding =
            PaddingValues(
                horizontal = 2.dp
            )
    ) {

        item {

            FilterChip(

                selected =
                    liveSelected,

                onClick =
                    onLiveClick,

                label = {

                    Text(

                        text =
                            if (
                                liveCount > 0
                            ) {
                                "🔴 ÉLŐ $liveCount"
                            } else {
                                "🔴 ÉLŐ"
                            },

                        fontWeight =
                            FontWeight.Bold,

                        fontSize = 12.sp
                    )
                },

                colors =
                    FilterChipDefaults
                        .filterChipColors(

                            selectedContainerColor =
                                AccentGreen.copy(
                                    alpha =
                                        0.22f
                                ),

                            selectedLabelColor =
                                AccentGreen
                        )
            )
        }

        item {

            TimeWindowChip(
                label = "⏱ <3h",
                selected =
                    timeWindow ==
                        TimeWindowFilter.THREE,
                onClick = {

                    onTimeWindowClick(
                        TimeWindowFilter.THREE
                    )
                }
            )
        }

        item {

            TimeWindowChip(
                label = "⏱ <6h",
                selected =
                    timeWindow ==
                        TimeWindowFilter.SIX,
                onClick = {

                    onTimeWindowClick(
                        TimeWindowFilter.SIX
                    )
                }
            )
        }

        item {

            TimeWindowChip(
                label = "⏱ <9h",
                selected =
                    timeWindow ==
                        TimeWindowFilter.NINE,
                onClick = {

                    onTimeWindowClick(
                        TimeWindowFilter.NINE
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeWindowChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    FilterChip(

        selected =
            selected,

        onClick =
            onClick,

        label = {

            Text(
                text = label,
                fontWeight =
                    FontWeight.Bold,
                fontSize = 12.sp
            )
        },

        colors =
            FilterChipDefaults
                .filterChipColors(

                    selectedContainerColor =
                        MaterialTheme
                            .colorScheme
                            .primary
                            .copy(
                                alpha =
                                    0.18f
                            ),

                    selectedLabelColor =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
    )
}

/*
 * ============================================================
 * SZŰRŐ FEJLÉC
 * ============================================================
 */

@Composable
private fun FilterHeader(
    expanded: Boolean,
    activeCount: Int,
    onClick: () -> Unit
) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 6.dp
                )
                .clip(
                    RoundedCornerShape(
                        14.dp
                    )
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
                        .copy(
                            alpha =
                                0.35f
                        ),
                    RoundedCornerShape(
                        14.dp
                    )
                )
                .clickable {
                    onClick()
                }
                .padding(
                    horizontal = 14.dp,
                    vertical = 11.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = "⚙ Szűrők",
            fontSize = 14.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurface
        )

        if (activeCount > 0) {

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )

            Box(

                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                8.dp
                            )
                        )
                        .background(
                            AccentGreen.copy(
                                alpha =
                                    0.16f
                            )
                        )
                        .padding(
                            horizontal = 7.dp,
                            vertical = 3.dp
                        )
            ) {

                Text(
                    text =
                        activeCount.toString(),
                    fontSize = 11.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        AccentGreen
                )
            }
        }

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        Text(
            text =
                if (expanded) {
                    "▲"
                } else {
                    "▼"
                },
            fontSize = 13.sp,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant
        )
    }
}

/*
 * ============================================================
 * SZŰRŐPANEL
 * ============================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterPanel(
    showOnlyLive: Boolean,
    onToggleLive: () -> Unit,
    showOnlyFavorites: Boolean,
    onToggleFavorites: () -> Unit,
    onClear: () -> Unit
) {

    Column(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 2.dp
                )
                .clip(
                    RoundedCornerShape(
                        16.dp
                    )
                )
                .background(
                    MaterialTheme
                        .colorScheme
                        .surfaceVariant
                        .copy(
                            alpha =
                                0.55f
                        )
                )
                .padding(12.dp)
    ) {

        Text(
            text =
                "További szűrők",
            fontSize = 12.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,
            modifier =
                Modifier.padding(
                    bottom = 6.dp
                )
        )

        LazyRow(

            horizontalArrangement =
                Arrangement.spacedBy(
                    7.dp
                )
        ) {

            item {

                FilterChip(
                    selected =
                        showOnlyLive,
                    onClick =
                        onToggleLive,
                    label = {
                        Text(
                            "🔴 Élő"
                        )
                    },
                    colors =
                        FilterChipDefaults
                            .filterChipColors(
                                selectedContainerColor =
                                    AccentGreen.copy(
                                        alpha =
                                            0.18f
                                    ),
                                selectedLabelColor =
                                    AccentGreen
                            )
                )
            }

            item {

                FilterChip(
                    selected =
                        showOnlyFavorites,
                    onClick =
                        onToggleFavorites,
                    label = {
                        Text(
                            "★ Kedvencek"
                        )
                    },
                    colors =
                        FilterChipDefaults
                            .filterChipColors(
                                selectedContainerColor =
                                    AccentGold.copy(
                                        alpha =
                                            0.18f
                                    ),
                                selectedLabelColor =
                                    AccentGold
                            )
                )
            }

            item {

                AssistChip(
                    onClick =
                        onClear,
                    label = {
                        Text(
                            "Szűrők törlése"
                        )
                    }
                )
            }
        }
    }
}

/*
 * ============================================================
 * ÉLŐ BADGE
 * ============================================================
 */

@Composable
private fun LiveBadge(
    count: Int
) {

    Box(

        modifier =
            Modifier
                .clip(
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .background(
                    AccentGreen.copy(
                        alpha =
                            0.18f
                    )
                )
                .border(
                    1.dp,
                    AccentGreen.copy(
                        alpha =
                            0.5f
                    ),
                    RoundedCornerShape(
                        10.dp
                    )
                )
                .padding(
                    horizontal = 8.dp,
                    vertical = 3.dp
                )
    ) {

        Text(
            text =
                "ÉLŐ $count",
            fontSize = 11.sp,
            fontWeight =
                FontWeight.Bold,
            color =
                AccentGreen,
            letterSpacing =
                0.4.sp
        )
    }
}

/*
 * ============================================================
 * ÜRES ÁLLAPOT
 * ============================================================
 */

@Composable
private fun EmptyState(
    showOnlyFavorites: Boolean,
    showOnlyLive: Boolean
) {
    val message = when {
        showOnlyLive -> "Jelenleg nincs élő mérkőzés"
        showOnlyFavorites -> "Nincs kedvenc mérkőzés ezen a napon"
        else -> "Nincsenek mérkőzések ezen a napon"
    }
    val icon = when {
        showOnlyLive -> Icons.Filled.SportsSoccer
        showOnlyFavorites -> Icons.Filled.Star
        else -> Icons.Filled.CalendarMonth
    }
    val iconTint = when {
        showOnlyLive -> AccentGreen
        showOnlyFavorites -> AccentGold
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Húzd le a frissítéshez, vagy válassz másik napot",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

/*
 * ============================================================
 * DÁTUMVÁLASZTÓ
 * ============================================================
 */

@Composable
private fun DateStrip(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {

    val dates =
        remember(Unit) {
            DateUtils.dateStrip()
        }

    LazyRow(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 10.dp
                ),

        horizontalArrangement =
            Arrangement.spacedBy(
                8.dp
            ),

        contentPadding =
            PaddingValues(
                horizontal = 12.dp
            )
    ) {

        items(dates) { dateStr ->

            val isSelected =
                dateStr ==
                    selectedDate

            Box(

                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(
                                14.dp
                            )
                        )
                        .background(

                            if (isSelected) {

                                Brush.horizontalGradient(
                                    listOf(

                                        MaterialTheme
                                            .colorScheme
                                            .primary,

                                        MaterialTheme
                                            .colorScheme
                                            .primary
                                            .copy(
                                                alpha =
                                                    0.85f
                                            )
                                    )
                                )

                            } else {

                                Brush.horizontalGradient(
                                    listOf(

                                        MaterialTheme
                                            .colorScheme
                                            .surface,

                                        MaterialTheme
                                            .colorScheme
                                            .surface
                                    )
                                )
                            }
                        )
                        .then(

                            if (!isSelected) {

                                Modifier.border(
                                    1.dp,

                                    MaterialTheme
                                        .colorScheme
                                        .outline
                                        .copy(
                                            alpha =
                                                0.5f
                                        ),

                                    RoundedCornerShape(
                                        14.dp
                                    )
                                )

                            } else {

                                Modifier
                            }
                        )
                        .clickable {

                            onDateSelected(
                                dateStr
                            )
                        }
                        .padding(
                            horizontal = 16.dp,
                            vertical = 9.dp
                        )
            ) {

                Text(
                    text =
                        DateUtils
                            .shortChipLabel(
                                dateStr
                            ),
                    fontSize = 13.sp,
                    fontWeight =
                        FontWeight.Bold,
                    color =
                        if (isSelected) {
                            Color.White
                        } else {
                            MaterialTheme
                                .colorScheme
                                .onSurface
                        }
                )
            }
        }
    }
}

/*
 * ============================================================
 * MECCSKÁRTYA ODDS-LEKÉRÉSSEL
 * ============================================================
 *
 * Csak akkor kér le odds-ot, ha be van kapcsolva a 💰 kapcsoló,
 * a meccs még nem kezdődött el, és még nincs a gyorsítótárban -
 * így nem terheljük feleslegesen az API-t minden meccsre.
 */
@Composable
private fun MatchCardWithOdds(
    match: MatchModel,
    showOdds: Boolean,
    apiService: FootballApiService,
    context: android.content.Context,
    isHomeFavorite: Boolean,
    isAwayFavorite: Boolean,
    onToggleHomeFavorite: () -> Unit,
    onToggleAwayFavorite: () -> Unit,
    onClick: () -> Unit
) {

    LaunchedEffect(match.id, showOdds) {

        if (
            showOdds &&
            match.isNotStarted &&
            !OddsCache.has(match.id) &&
            !OddsCache.isLoading(match.id)
        ) {

            OddsCache.markLoading(match.id)

            val apiKey = ApiKeyManager.getApiKey(context)

            if (apiKey.isBlank()) {

                OddsCache.set(match.id, null)

            } else {

                try {

                    val response =
                        apiService.getOdds(
                            apiKey,
                            match.id
                        )

                    OddsCache.set(
                        match.id,
                        OddsCache.extractBest1X2(response)
                    )

                } catch (_: Exception) {

                    OddsCache.set(match.id, null)
                }
            }
        }
    }

    MatchCard(
        match = match,
        isHomeFavorite = isHomeFavorite,
        isAwayFavorite = isAwayFavorite,
        onToggleHomeFavorite = onToggleHomeFavorite,
        onToggleAwayFavorite = onToggleAwayFavorite,
        oddsSummary = if (showOdds) OddsCache.get(match.id) else null,
        onClick = onClick
    )
}

/*
 * ============================================================
 * BAJNOKSÁG FEJLÉC
 * ============================================================
 */


@Composable
private fun LeagueHeader(
    leagueDisplayName: String,
    leagueLogo: String?,
    countryLogo: String?,
    isFavorite: Boolean,
    matchCount: Int,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    onToggleFavorite: () -> Unit,
    onStandingsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleCollapsed)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (collapsed) "▶" else "▼",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(18.dp)
            )
            if (!countryLogo.isNullOrBlank()) {
                AsyncImage(
                    model = countryLogo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            if (!leagueLogo.isNullOrBlank()) {
                AsyncImage(
                    model = leagueLogo,
                    contentDescription = null,
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = leagueDisplayName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$matchCount meccs",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Tabella",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onStandingsClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            Text(
                text = if (isFavorite) "★" else "☆",
                fontSize = 18.sp,
                color = if (isFavorite) AccentGold
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier
                    .clickable(onClick = onToggleFavorite)
                    .padding(start = 4.dp, end = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(1.dp)
                .background(
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
        )
    }
}



