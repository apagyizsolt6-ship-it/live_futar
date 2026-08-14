package com.livefutar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.MatchCard
import com.livefutar.app.ui.theme.AccentGold
import com.livefutar.app.ui.theme.AccentGreen
import com.livefutar.app.util.DateUtils
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private enum class MatchTimeFilter {
    ALL,
    WITHIN_3_HOURS,
    WITHIN_6_HOURS,
    WITHIN_9_HOURS
}

private data class LeagueGroup(
    val key: String,
    val displayName: String,
    val matches: List<MatchModel>
)

@OptIn(ExperimentalMaterial3Api::class)
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
    var filtersExpanded by rememberSaveable { mutableStateOf(false) }
    var timeFilter by rememberSaveable {
        mutableStateOf(MatchTimeFilter.ALL)
    }

    val collapsedLeagues = remember {
        mutableStateMapOf<String, Boolean>()
    }

    val now = remember {
        mutableStateOf(System.currentTimeMillis())
    }

    LaunchedEffect(matches, selectedDate) {
        now.value = System.currentTimeMillis()
    }

    val filteredMatches = remember(
        matches,
        favoriteTeamIds,
        favoriteLeagueIds,
        showOnlyFavorites,
        showOnlyLive,
        timeFilter,
        now.value
    ) {
        var result = matches

        /*
         * KEDVENCEK SZŰRÉSE
         */
        if (showOnlyFavorites) {
            result = result.filter { match ->
                (match.league?.id != null &&
                    favoriteLeagueIds.contains(match.league.id)) ||

                    (match.homeTeam?.id != null &&
                        favoriteTeamIds.contains(match.homeTeam.id)) ||

                    (match.awayTeam?.id != null &&
                        favoriteTeamIds.contains(match.awayTeam.id))
            }
        }

        /*
         * ÉLŐ SZŰRÉS
         */
        if (showOnlyLive) {
            result = result.filter { it.isLive }
        }

        /*
         * 3 / 6 / 9 ÓRÁS SZŰRÉS
         */
        val windowHours = when (timeFilter) {
            MatchTimeFilter.ALL -> null
            MatchTimeFilter.WITHIN_3_HOURS -> 3L
            MatchTimeFilter.WITHIN_6_HOURS -> 6L
            MatchTimeFilter.WITHIN_9_HOURS -> 9L
        }

        if (windowHours != null) {
            val start = now.value
            val end =
                start + windowHours * 60L * 60L * 1000L

            result = result.filter { match ->

                if (!match.isNotStarted) {
                    return@filter false
                }

                val kickoff =
                    parseMatchDate(match.date)?.time
                        ?: return@filter false

                kickoff in start..end
            }
        }

        /*
         * LEGKORÁBBI MECCSTŐL FELFELÉ
         */
        result.sortedWith(
            compareBy<MatchModel> {
                parseMatchDate(it.date)?.time
                    ?: Long.MAX_VALUE
            }.thenBy {
                it.id
            }
        )
    }

    /*
     * ÉLŐ MECCSEK SZÁMA
     */
    val liveCount = matches.count {
        it.isLive
    }

    /*
     * BAJNOKSÁGOK CSOPORTOSÍTÁSA
     */
    val grouped = filteredMatches
        .groupBy { match ->
            leagueKey(match)
        }
        .map { (key, list) ->

            LeagueGroup(
                key = key,
                displayName =
                    list.firstOrNull()?.leagueDisplayName
                        ?: "Egyéb mérkőzések",

                matches = list.sortedWith(
                    compareBy<MatchModel> {
                        parseMatchDate(it.date)?.time
                            ?: Long.MAX_VALUE
                    }.thenBy {
                        it.id
                    }
                )
            )
        }
        .sortedWith(
            compareBy<LeagueGroup> {
                it.matches.firstOrNull()?.let { match ->
                    parseMatchDate(match.date)?.time
                } ?: Long.MAX_VALUE
            }.thenBy {
                it.displayName
            }
        )

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Text(
                            text = "LIVE FUTÁR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = "⚽",
                            fontSize = 16.sp
                        )

                        if (liveCount > 0) {

                            Spacer(
                                modifier = Modifier.width(8.dp)
                            )

                            LiveBadge(
                                count = liveCount
                            )
                        }
                    }
                },

                actions = {

                    /*
                     * FRISSÍTÉS
                     */
                    Text(
                        text =
                            if (isRefreshing) {
                                "…"
                            } else {
                                "↻"
                            },

                        fontSize = 20.sp,

                        color =
                            MaterialTheme.colorScheme.primary,

                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable(
                                enabled = !isRefreshing
                            ) {
                                onRefresh()
                            }
                    )

                    /*
                     * ÉLŐ SZŰRŐ
                     */
                    FilterChip(

                        selected = showOnlyLive,

                        onClick =
                            onToggleShowOnlyLive,

                        label = {

                            Text(
                                text =
                                    if (showOnlyLive) {
                                        "ÉLŐ"
                                    } else {
                                        "Élő"
                                    },

                                fontSize = 12.sp,
                                fontWeight =
                                    FontWeight.Bold
                            )
                        },

                        colors =
                            FilterChipDefaults.filterChipColors(

                                selectedContainerColor =
                                    AccentGreen.copy(
                                        alpha = 0.25f
                                    ),

                                selectedLabelColor =
                                    AccentGreen,

                                containerColor =
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceVariant,

                                labelColor =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant
                            ),

                        modifier =
                            Modifier.padding(end = 4.dp)
                    )

                    /*
                     * KEDVENCEK
                     */
                    Text(

                        text =
                            if (showOnlyFavorites) {
                                "★"
                            } else {
                                "☆"
                            },

                        fontSize = 22.sp,

                        color =
                            if (showOnlyFavorites) {
                                AccentGold
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                            },

                        modifier = Modifier
                            .padding(end = 14.dp)
                            .clickable {
                                onToggleShowOnlyFavorites()
                            }
                    )
                },

                colors =
                    TopAppBarDefaults.topAppBarColors(

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

        LazyColumn(

            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    MaterialTheme
                        .colorScheme
                        .background
                ),

            verticalArrangement =
                Arrangement.spacedBy(1.dp)
        ) {

            /*
             * DÁTUMVÁLASZTÓ
             */
            item {

                DateStrip(
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )
            }

            /*
             * DÁTUM
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
                            timeFilter,
                            showOnlyLive,
                            showOnlyFavorites
                        ),

                    onClick = {
                        filtersExpanded =
                            !filtersExpanded
                    }
                )
            }

            /*
             * LENYITHATÓ SZŰRŐPANEL
             */
            if (filtersExpanded) {

                item {

                    FilterPanel(

                        timeFilter =
                            timeFilter,

                        onTimeFilterSelected = {
                            timeFilter = it
                        },

                        showOnlyLive =
                            showOnlyLive,

                        onToggleLive =
                            onToggleShowOnlyLive,

                        showOnlyFavorites =
                            showOnlyFavorites,

                        onToggleFavorites =
                            onToggleShowOnlyFavorites,

                        onClear = {

                            timeFilter =
                                MatchTimeFilter.ALL

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
            if (filteredMatches.isEmpty()) {

                item {

                    EmptyState(

                        showOnlyFavorites =
                            showOnlyFavorites,

                        showOnlyLive =
                            showOnlyLive,

                        timeFilter =
                            timeFilter
                    )
                }

            } else {

                /*
                 * BAJNOKSÁGOK
                 */
                grouped.forEach { group ->

                    val first =
                        group.matches.firstOrNull()

                    val leagueId =
                        first?.league?.id

                    val isFavoriteLeague =
                        leagueId != null &&
                            favoriteLeagueIds.contains(
                                leagueId
                            )

                    val isCollapsed =
                        collapsedLeagues[group.key] == true

                    /*
                     * BAJNOKSÁG FEJLÉC
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
                                ] = !isCollapsed
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
                     * BAJNOKSÁG MECCSEI
                     */
                    if (!isCollapsed) {

                        items(

                            items = group.matches,

                            key = { match ->
                                "match_${match.id}"
                            }

                        ) { match ->

                            MatchCard(

                                match = match,

                                isHomeFavorite =
                                    match.homeTeam?.id != null &&
                                        favoriteTeamIds.contains(
                                            match.homeTeam.id
                                        ),

                                isAwayFavorite =
                                    match.awayTeam?.id != null &&
                                        favoriteTeamIds.contains(
                                            match.awayTeam.id
                                        ),

                                onToggleHomeFavorite = {

                                    match.homeTeam?.id?.let(
                                        onToggleTeamFavorite
                                    )
                                },

                                onToggleAwayFavorite = {

                                    match.awayTeam?.id?.let(
                                        onToggleTeamFavorite
                                    )
                                },

                                onClick = {
                                    onMatchClick(match)
                                }
                            )
                        }
                    }
                }
            }

            item {

                Spacer(
                    modifier =
                        Modifier.height(18.dp)
                )
            }
        }
    }
}

/*
 * AKTÍV SZŰRŐK SZÁMA
 */
private fun activeFilterCount(
    timeFilter: MatchTimeFilter,
    showOnlyLive: Boolean,
    showOnlyFavorites: Boolean
): Int {

    var count = 0

    if (timeFilter != MatchTimeFilter.ALL) {
        count++
    }

    if (showOnlyLive) {
        count++
    }

    if (showOnlyFavorites) {
        count++
    }

    return count
}

/*
 * BAJNOKSÁG AZONOSÍTÓ
 */
private fun leagueKey(
    match: MatchModel
): String {

    return match.league?.id?.toString()
        ?: "${match.country?.code.orEmpty()}_${match.leagueDisplayName}"
}

/*
 * MECCS IDŐPONT PARSOLÁSA
 */
private fun parseMatchDate(
    value: String?
): Date? {

    if (value.isNullOrBlank()) {
        return null
    }

    val patterns = listOf(

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

            if (pattern.contains("'Z'")) {

                formatter.timeZone =
                    TimeZone.getTimeZone("UTC")
            }

            return formatter.parse(value)

        } catch (_: ParseException) {

            // Következő formátum.
        }
    }

    return null
}

/*
 * SZŰRŐ FEJLÉC
 */
@Composable
private fun FilterHeader(
    expanded: Boolean,
    activeCount: Int,
    onClick: () -> Unit
) {

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 6.dp
            )
            .clip(
                RoundedCornerShape(14.dp)
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
                    .copy(alpha = 0.35f),
                RoundedCornerShape(14.dp)
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

                modifier = Modifier
                    .clip(
                        RoundedCornerShape(8.dp)
                    )
                    .background(
                        AccentGreen.copy(
                            alpha = 0.16f
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
 * SZŰRŐPANEL
 */
/*
 * SZŰRŐPANEL
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterPanel(
    timeFilter: MatchTimeFilter,
    onTimeFilterSelected: (MatchTimeFilter) -> Unit,
    showOnlyLive: Boolean,
    onToggleLive: () -> Unit,
    showOnlyFavorites: Boolean,
    onToggleFavorites: () -> Unit,
    onClear: () -> Unit
) {
    Column(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 12.dp,
                vertical = 2.dp
            )
            .clip(
                RoundedCornerShape(16.dp)
            )
            .background(
                MaterialTheme
                    .colorScheme
                    .surfaceVariant
                    .copy(alpha = 0.55f)
            )
            .padding(12.dp)
    ) {

        Text(

            text = "Idő szerinti szűrés",

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
                Arrangement.spacedBy(7.dp),

            contentPadding =
                PaddingValues(
                    bottom = 4.dp
                )
        ) {

            item {

                FilterChip(

                    selected =
                        timeFilter ==
                            MatchTimeFilter.ALL,

                    onClick = {

                        onTimeFilterSelected(
                            MatchTimeFilter.ALL
                        )
                    },

                    label = {
                        Text("Összes")
                    }
                )
            }

            item {

                FilterChip(

                    selected =
                        timeFilter ==
                            MatchTimeFilter.WITHIN_3_HOURS,

                    onClick = {

                        onTimeFilterSelected(
                            MatchTimeFilter.WITHIN_3_HOURS
                        )
                    },

                    label = {
                        Text("3 órán belül")
                    }
                )
            }

            item {

                FilterChip(

                    selected =
                        timeFilter ==
                            MatchTimeFilter.WITHIN_6_HOURS,

                    onClick = {

                        onTimeFilterSelected(
                            MatchTimeFilter.WITHIN_6_HOURS
                        )
                    },

                    label = {
                        Text("6 órán belül")
                    }
                )
            }

            item {

                FilterChip(

                    selected =
                        timeFilter ==
                            MatchTimeFilter.WITHIN_9_HOURS,

                    onClick = {

                        onTimeFilterSelected(
                            MatchTimeFilter.WITHIN_9_HOURS
                        )
                    },

                    label = {
                        Text("9 órán belül")
                    }
                )
            }
        }

        Spacer(
            modifier =
                Modifier.height(6.dp)
        )

        Text(

            text = "További szűrők",

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
                Arrangement.spacedBy(7.dp)
        ) {

            item {

                FilterChip(

                    selected =
                        showOnlyLive,

                    onClick =
                        onToggleLive,

                    label = {
                        Text("🔴 Élő")
                    },

                    colors =
                        FilterChipDefaults
                            .filterChipColors(

                                selectedContainerColor =
                                    AccentGreen.copy(
                                        alpha = 0.18f
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
                        Text("★ Kedvencek")
                    },

                    colors =
                        FilterChipDefaults
                            .filterChipColors(

                                selectedContainerColor =
                                    AccentGold.copy(
                                        alpha = 0.18f
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
                        Text("Szűrők törlése")
                    }
                )
            }
        }
    }
}

/*
 * ÉLŐ JELZŐ
 */
@Composable
private fun LiveBadge(
    count: Int
) {

    Box(

        modifier = Modifier
            .clip(
                RoundedCornerShape(10.dp)
            )
            .background(
                AccentGreen.copy(
                    alpha = 0.18f
                )
            )
            .border(
                1.dp,
                AccentGreen.copy(
                    alpha = 0.5f
                ),
                RoundedCornerShape(10.dp)
            )
            .padding(
                horizontal = 8.dp,
                vertical = 3.dp
            )
    ) {

        Text(

            text = "ÉLŐ $count",

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
 * ÜRES ÁLLAPOT
 */
@Composable
private fun EmptyState(
    showOnlyFavorites: Boolean,
    showOnlyLive: Boolean,
    timeFilter: MatchTimeFilter
) {

    val message = when {

        showOnlyLive ->
            "Jelenleg nincs élő mérkőzés"

        showOnlyFavorites ->
            "Nincs kedvenc mérkőzés ezen a napon"

        timeFilter != MatchTimeFilter.ALL ->
            "Nincs mérkőzés ebben az időablakban"

        else ->
            "Nincsenek mérkőzések ezen a napon"
    }

    Box(

        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),

        contentAlignment =
            Alignment.Center
    ) {

        Column(

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Text(

                text = when {

                    showOnlyLive ->
                        "⚽"

                    showOnlyFavorites ->
                        "★"

                    timeFilter !=
                        MatchTimeFilter.ALL ->
                        "⏱"

                    else ->
                        "📅"
                },

                fontSize = 36.sp
            )

            Spacer(
                modifier =
                    Modifier.height(12.dp)
            )

            Text(

                text = message,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                fontSize = 15.sp,

                fontWeight =
                    FontWeight.Medium
            )
        }
    }
}

/*
 * DÁTUMVÁLASZTÓ
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

        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),

        horizontalArrangement =
            Arrangement.spacedBy(8.dp),

        contentPadding =
            PaddingValues(
                horizontal = 12.dp
            )
    ) {

        items(dates) { dateStr ->

            val isSelected =
                dateStr == selectedDate

            Box(

                modifier = Modifier
                    .clip(
                        RoundedCornerShape(14.dp)
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
                                            alpha = 0.85f
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
                                        alpha = 0.5f
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
                        DateUtils.shortChipLabel(
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
 * BAJNOKSÁG FEJLÉC
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

    Row(

        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = 14.dp,
                vertical = 8.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        /*
         * NYITÁS / ZÁRÁS
         */
        Box(

            modifier = Modifier
                .size(28.dp)
                .clip(
                    RoundedCornerShape(8.dp)
                )
                .clickable {
                    onToggleCollapsed()
                },

            contentAlignment =
                Alignment.Center
        ) {

            Text(

                text =
                    if (collapsed) {
                        "▶"
                    } else {
                        "▼"
                    },

                fontSize = 11.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
            )
        }

        /*
         * ORSZÁG LOGÓ
         */
        if (!countryLogo.isNullOrBlank()) {

            AsyncImage(

                model = countryLogo,

                contentDescription = null,

                modifier = Modifier
                    .size(17.dp)
                    .clip(
                        RoundedCornerShape(2.dp)
                    ),

                contentScale =
                    ContentScale.Fit
            )

            Spacer(
                modifier =
                    Modifier.width(6.dp)
            )
        }

        /*
         * BAJNOKSÁG LOGÓ
         */
        if (!leagueLogo.isNullOrBlank()) {

            AsyncImage(

                model = leagueLogo,

                contentDescription = null,

                modifier = Modifier
                    .size(19.dp)
                    .clip(
                        CircleShape
                    ),

                contentScale =
                    ContentScale.Fit
            )

            Spacer(
                modifier =
                    Modifier.width(8.dp)
            )
        }

        /*
         * BAJNOKSÁG NEVE
         */
        Column(

            modifier = Modifier
                .weight(1f)
                .clickable {
                    onToggleCollapsed()
                }
        ) {

            Text(

                text =
                    leagueDisplayName,

                fontSize = 13.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis
            )

            Text(

                text =
                    "$matchCount meccs",

                fontSize = 10.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                        .copy(
                            alpha = 0.75f
                        )
            )
        }

        /*
         * TABELLA
         */
        Text(

            text = "Tabella",

            fontSize = 12.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                MaterialTheme
                    .colorScheme
                    .primary,

            modifier = Modifier
                .clickable {
                    onStandingsClick()
                }
                .padding(
                    horizontal = 8.dp,
                    vertical = 8.dp
                )
        )

        /*
         * BAJNOKSÁG KEDVENC
         */
        Text(

            text =
                if (isFavorite) {
                    "★"
                } else {
                    "☆"
                },

            fontSize = 17.sp,

            color =
                if (isFavorite) {
                    AccentGold
                } else {
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                },

            modifier = Modifier
                .clickable {
                    onToggleFavorite()
                }
                .padding(start = 4.dp)
        )
    }
}
