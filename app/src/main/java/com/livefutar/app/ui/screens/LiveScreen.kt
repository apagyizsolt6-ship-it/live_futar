package com.livefutar.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.LivePulseDot
import com.livefutar.app.ui.components.MatchCard
import com.livefutar.app.ui.components.PullRefreshBox
import com.livefutar.app.ui.theme.AccentGold
import com.livefutar.app.ui.theme.AccentGreen
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone


/*
 * ============================================================
 * LIVE FUTÁR
 *
 * Élő mérkőzések képernyő
 *
 * Funkciók:
 *
 * - csak élő mérkőzések
 * - országok ABC sorrendben
 * - országon belül bajnokság ABC sorrendben
 * - Friendlies / Barátságos mérkőzések mindig legalul
 * - bajnokságok nyithatók / zárhatók
 * - élő mérkőzések számláló
 * - frissítés gomb
 * - kedvenc csapatok
 * - meccs részletek megnyitása
 * - kereső (csapat / bajnokság)
 * - „Kedvenceim élőben” szekció a lista tetején
 *
 * FONTOS:
 *
 * A 3h / 6h / 9h szűrők NEM ezen a képernyőn vannak.
 * Ezek a Meccsek / HomeScreen oldalon működnek.
 *
 * ============================================================
 */


/*
 * ============================================================
 * BAJNOKSÁG CSOPORT
 * ============================================================
 */

private data class LiveLeagueGroup(

    val key: String,

    val displayName: String,

    val countryName: String,

    val matches: List<MatchModel>
)


/*
 * ============================================================
 * LIVE SCREEN
 * ============================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(

    matches: List<MatchModel>,

    favoriteTeamIds: Set<Long>,

    onToggleTeamFavorite: (Long) -> Unit,

    isRefreshing: Boolean = false,

    onRefresh: () -> Unit = {},

    onMatchClick: (MatchModel) -> Unit

) {

    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    /** null = minden liga */
    var selectedLeagueKey by rememberSaveable { mutableStateOf<String?>(null) }

    /*
     * ========================================================
     * BAJNOKSÁGOK NYITOTT / ZÁRT ÁLLAPOTA
     *
     * FONTOS:
     *
     * Nem használunk rememberSaveable/mapSaver megoldást.
     *
     * Ez okozta a korábbi:
     *
     * Type mismatch:
     * inferred type is Any? but Boolean was expected
     *
     * hibát.
     * ========================================================
     */

    val collapsedLeagues =
        remember {

            mutableStateMapOf<String, Boolean>()

        }


    /*
     * ========================================================
     * CSAK ÉLŐ MECCSEK (+ opcionális kereső)
     * ========================================================
     */

    val allLiveMatches =
        matches.filter { match ->
            match.isLive
        }

    val queryNormalized =
        searchQuery.trim().lowercase(Locale.getDefault())

    val searchFiltered =
        if (queryNormalized.isBlank()) {
            allLiveMatches
        } else {
            allLiveMatches.filter { match ->
                val home = match.homeTeam?.name.orEmpty()
                    .lowercase(Locale.getDefault())
                val away = match.awayTeam?.name.orEmpty()
                    .lowercase(Locale.getDefault())
                val league = match.leagueDisplayName
                    .orEmpty()
                    .lowercase(Locale.getDefault())
                home.contains(queryNormalized) ||
                    away.contains(queryNormalized) ||
                    league.contains(queryNormalized)
            }
        }

    /*
     * Liga chip opciók (keresés előtt, az összes élőből).
     */
    val leagueChipOptions = remember(allLiveMatches) {
        allLiveMatches
            .groupBy { liveLeagueKey(it) }
            .map { (key, list) ->
                val name = list.firstOrNull()?.leagueDisplayName
                    ?.takeIf { it.isNotBlank() }
                    ?: "Egyéb"
                key to (name to list.size)
            }
            .sortedBy { it.second.first.lowercase(Locale.getDefault()) }
    }

    val liveMatches =
        if (selectedLeagueKey == null) {
            searchFiltered
        } else {
            searchFiltered.filter { liveLeagueKey(it) == selectedLeagueKey }
        }

    /*
     * Kedvenc csapatok élő meccsei – a lista tetején.
     */
    val favoriteLiveMatches =
        liveMatches.filter { match ->
            val homeId = match.homeTeam?.id
            val awayId = match.awayTeam?.id
            (homeId != null && homeId in favoriteTeamIds) ||
                (awayId != null && awayId in favoriteTeamIds)
        }

    /*
     * ========================================================
     * ÉLŐ MECCSEK SZÁMA
     * ========================================================
     */

    val liveCount =
        liveMatches.size


    /*
     * ========================================================
     * BAJNOKSÁGOK CSOPORTOSÍTÁSA
     * ========================================================
     *
     * Először bajnokság szerint csoportosítunk.
     *
     * Utána:
     *
     * 1. normál bajnokságok
     * 2. Friendlies legalul
     *
     * Normál bajnokságoknál:
     *
     * ország ABC
     * majd bajnokság ABC
     *
     * ========================================================
     */

    // Kedvencek szekció mellett ne ismétlődjenek a liga-listában (keresésnél viszont minden)
    val matchesForGrouping =
        if (queryNormalized.isBlank() && favoriteLiveMatches.isNotEmpty()) {
            liveMatches.filter { match ->
                val homeId = match.homeTeam?.id
                val awayId = match.awayTeam?.id
                !((homeId != null && homeId in favoriteTeamIds) ||
                    (awayId != null && awayId in favoriteTeamIds))
            }
        } else {
            liveMatches
        }

    val grouped =
        matchesForGrouping
            .groupBy { match ->
                liveLeagueKey(match)
            }

            .map { (key, leagueMatches) ->

                val firstMatch =
                    leagueMatches.firstOrNull()

                val displayName =
                    firstMatch
                        ?.leagueDisplayName
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: "Egyéb mérkőzések"

                LiveLeagueGroup(

                    key = key,

                    displayName =
                        displayName,

                    countryName =
                        extractLiveCountry(
                            displayName
                        ),

                    matches =
                        leagueMatches.sortedWith(

                            compareBy<MatchModel> {

                                parseLiveDate(
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

                compareBy<LiveLeagueGroup> {

                    /*
                     * Friendlies mindig utolsó.
                     */

                    if (
                        isLiveFriendly(
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

                    normalizeLiveSort(
                        it.countryName
                    )

                }.thenBy {

                    /*
                     * BAJNOKSÁG ABC
                     */

                    normalizeLiveSort(
                        leagueNameOnly(
                            it.displayName
                        )
                    )

                }
            )


    /*
     * ========================================================
     * KÉPERNYŐ
     * ========================================================
     */

    Scaffold(

        topBar = {

            TopAppBar(

                title = {

                    Row(

                        verticalAlignment =
                            Alignment.CenterVertically

                    ) {

                        /*
                         * ZÖLD ÉLŐ PONT
                         */

                        Box(

                            modifier =
                                Modifier
                                    .size(10.dp)
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        AccentGreen
                                    )

                        )

                        Spacer(
                            modifier =
                                Modifier.width(
                                    8.dp
                                )
                        )


                        /*
                         * CÍM
                         */

                        Text(

                            text =
                                "Élő mérkőzések",

                            fontSize =
                                19.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurface

                        )


                        Spacer(
                            modifier =
                                Modifier.width(
                                    9.dp
                                )
                        )


                        /*
                         * ÉLŐ DARABSZÁM
                         */

                        Text(

                            text =
                                "($liveCount)",

                            fontSize =
                                14.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                AccentGreen

                        )
                    }
                },


                actions = {
                    Text(
                        text = "🔍",
                        fontSize = 18.sp,
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .clickable {
                                searchExpanded = !searchExpanded
                                if (!searchExpanded) searchQuery = ""
                            }
                    )
                    Text(
                        text = if (isRefreshing) "…" else "↻",
                        fontSize = 23.sp,
                        color = AccentGreen,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable(enabled = !isRefreshing) {
                                onRefresh()
                            }
                    )
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            AnimatedVisibility(
                visible = searchExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                LiveSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onClear = {
                        searchQuery = ""
                        searchExpanded = false
                    }
                )
            }

            if (leagueChipOptions.isNotEmpty() && !searchExpanded) {
                LiveLeagueChips(
                    options = leagueChipOptions,
                    selectedKey = selectedLeagueKey,
                    totalCount = allLiveMatches.size,
                    onSelect = { key ->
                        selectedLeagueKey =
                            if (selectedLeagueKey == key) null else key
                    },
                    onClear = { selectedLeagueKey = null }
                )
            }

            PullRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.weight(1f)
            ) {
            if (liveMatches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (queryNormalized.isNotBlank()) "🔍" else "⚽",
                            fontSize = 42.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (queryNormalized.isNotBlank()) {
                                "Nincs találat: \"$searchQuery\""
                            } else {
                                "Jelenleg nincs élő mérkőzés"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (queryNormalized.isBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Húzd le a frissítést, vagy nézz vissza később",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            } else {
            /*
             * =================================================
             * ÉLŐ LISTA
             * =================================================
             */
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                /*
                 * Kedvenceim élőben – a lista tetején
                 */
                if (favoriteLiveMatches.isNotEmpty() && queryNormalized.isBlank()) {
                    item(key = "fav_header") {
                        FavoriteLiveHeader(count = favoriteLiveMatches.size)
                    }
                    items(
                        items = favoriteLiveMatches,
                        key = { "fav_${it.id}" }
                    ) { match ->
                        MatchCard(
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
                    item(key = "fav_spacer") {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                /*
                 * =================================================
                 * BAJNOKSÁGOK
                 * =================================================
                 */

                grouped.forEach { group ->

                    val firstMatch =
                        group.matches
                            .firstOrNull()


                    /*
                     * =================================================
                     * BAJNOKSÁG NYITOTT / ZÁRT
                     * =================================================
                     */

                    val isCollapsed =
                        collapsedLeagues[
                            group.key
                        ] == true


                    /*
                     * =================================================
                     * BAJNOKSÁG FEJLÉC
                     * =================================================
                     */

                    item(

                        key =
                            "live_league_${group.key}"

                    ) {

                        LiveLeagueHeader(

                            leagueDisplayName =
                                group.displayName,

                            countryLogo =
                                firstMatch
                                    ?.country
                                    ?.logo,

                            leagueLogo =
                                firstMatch
                                    ?.league
                                    ?.logo,

                            matchCount =
                                group.matches.size,

                            collapsed =
                                isCollapsed,

                            onToggleCollapsed = {

                                collapsedLeagues[
                                    group.key
                                ] =
                                    !isCollapsed

                            }
                        )
                    }


                    /*
                     * =================================================
                     * MECCSEK
                     *
                     * Csak akkor jelennek meg,
                     * ha a bajnokság nyitva van.
                     * =================================================
                     */

                    if (!isCollapsed) {

                        items(

                            items =
                                group.matches,

                            key = { match ->

                                "live_match_${match.id}"

                            }

                        ) { match ->


                            /*
                             * =================================================
                             * MECCSKÁRTYA
                             * =================================================
                             */

                            MatchCard(

                                match =
                                    match,


                                /*
                                 * HAZAI KEDVENC
                                 */

                                isHomeFavorite =
                                    match
                                        .homeTeam
                                        ?.id
                                        ?.let { id ->

                                            favoriteTeamIds
                                                .contains(id)

                                        }
                                        ?: false,


                                /*
                                 * VENDÉG KEDVENC
                                 */

                                isAwayFavorite =
                                    match
                                        .awayTeam
                                        ?.id
                                        ?.let { id ->

                                            favoriteTeamIds
                                                .contains(id)

                                        }
                                        ?: false,


                                /*
                                 * HAZAI KEDVENC KAPCSOLÁSA
                                 */

                                onToggleHomeFavorite = {

                                    match
                                        .homeTeam
                                        ?.id
                                        ?.let(
                                            onToggleTeamFavorite
                                        )

                                },


                                /*
                                 * VENDÉG KEDVENC KAPCSOLÁSA
                                 */

                                onToggleAwayFavorite = {

                                    match
                                        .awayTeam
                                        ?.id
                                        ?.let(
                                            onToggleTeamFavorite
                                        )

                                },


                                /*
                                 * MECCS RÉSZLETEK
                                 */

                                onClick = {

                                    onMatchClick(
                                        match
                                    )

                                }
                            )
                        }
                    }
                }


                /*
                 * =================================================
                 * ALSÓ TÉRKÖZ
                 * =================================================
                 */

                item {
                    Spacer(
                        modifier = Modifier.height(20.dp)
                    )
                }
            } // LazyColumn
            } // else (van élő meccs)
            } // PullRefreshBox
        } // Column
    } // Scaffold content
}


@Composable
private fun LiveLeagueChips(
    options: List<Pair<String, Pair<String, Int>>>,
    selectedKey: String?,
    totalCount: Int,
    onSelect: (String) -> Unit,
    onClear: () -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
    ) {
        item {
            FilterChip(
                selected = selectedKey == null,
                onClick = onClear,
                label = {
                    Text(
                        text = "Összes ($totalCount)",
                        fontSize = 12.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentGreen.copy(alpha = 0.22f),
                    selectedLabelColor = AccentGreen
                )
            )
        }
        items(options, key = { it.first }) { (key, nameAndCount) ->
            val (name, count) = nameAndCount
            FilterChip(
                selected = selectedKey == key,
                onClick = { onSelect(key) },
                label = {
                    Text(
                        text = "$name ($count)",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentGreen.copy(alpha = 0.22f),
                    selectedLabelColor = AccentGreen
                )
            )
        }
    }
}

@Composable
private fun LiveSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "🔍", fontSize = 15.sp)
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
            ),
            cursorBrush = SolidColor(AccentGreen),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        text = "Csapat vagy bajnokság…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
                inner()
            }
        )
        if (query.isNotEmpty()) {
            Text(
                text = "✕",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable { onQueryChange("") }
                    .padding(start = 8.dp)
            )
        }
        Text(
            text = "Bezár",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = AccentGreen,
            modifier = Modifier
                .clickable(onClick = onClear)
                .padding(start = 12.dp)
        )
    }
}

@Composable
private fun FavoriteLiveHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LivePulseDot(color = AccentGold)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Kedvenceim élőben",
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
                text = count.toString(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGold
            )
        }
    }
}

/*
 * ============================================================
 * ÉLŐ BAJNOKSÁG FEJLÉC
 * ============================================================
 *
 * Példa:
 *
 * ▼ 🇫🇷 Franciaország · Ligue 2       9
 *
 * vagy becsukva:
 *
 * ▶ 🇫🇷 Franciaország · Ligue 2       9
 *
 * ============================================================
 */

@Composable
private fun LiveLeagueHeader(

    leagueDisplayName: String,

    countryLogo: String?,

    leagueLogo: String?,

    matchCount: Int,

    collapsed: Boolean,

    onToggleCollapsed: () -> Unit

) {

    Row(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 8.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically

    ) {


        /*
         * ========================================================
         * NYITÁS / ZÁRÁS GOMB
         * ========================================================
         */

        Box(

            modifier =
                Modifier
                    .size(28.dp)
                    .clip(
                        RoundedCornerShape(
                            8.dp
                        )
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

                fontSize =
                    11.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant

            )
        }


        /*
         * ========================================================
         * ORSZÁG LOGÓ
         * ========================================================
         */

        if (
            !countryLogo.isNullOrBlank()
        ) {

            AsyncImage(

                model =
                    countryLogo,

                contentDescription =
                    null,

                modifier =
                    Modifier
                        .size(18.dp)
                        .clip(
                            RoundedCornerShape(
                                3.dp
                            )
                        ),

                contentScale =
                    ContentScale.Fit

            )

            Spacer(
                modifier =
                    Modifier.width(
                        6.dp
                    )
            )
        }


        /*
         * ========================================================
         * BAJNOKSÁG LOGÓ
         * ========================================================
         */

        if (
            !leagueLogo.isNullOrBlank()
        ) {

            AsyncImage(

                model =
                    leagueLogo,

                contentDescription =
                    null,

                modifier =
                    Modifier
                        .size(19.dp)
                        .clip(
                            CircleShape
                        ),

                contentScale =
                    ContentScale.Fit

            )

            Spacer(
                modifier =
                    Modifier.width(
                        8.dp
                    )
            )
        }


        /*
         * ========================================================
         * BAJNOKSÁG NEVE
         * ========================================================
         */

        Column(

            modifier =
                Modifier
                    .weight(1f)
                    .clickable {

                        onToggleCollapsed()

                    }

        ) {

            Text(

                text =
                    leagueDisplayName,

                fontSize =
                    13.sp,

                fontWeight =
                    FontWeight.Bold,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                maxLines =
                    1,

                overflow =
                    TextOverflow.Ellipsis

            )

            Text(

                text =
                    "$matchCount meccs",

                fontSize =
                    10.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant
                        .copy(
                            alpha =
                                0.75f
                        )

            )
        }


        /*
         * ========================================================
         * MECCSSZÁM
         * ========================================================
         */

        Text(

            text =
                matchCount.toString(),

            fontSize =
                13.sp,

            fontWeight =
                FontWeight.Bold,

            color =
                AccentGreen,

            modifier =
                Modifier.padding(
                    start = 6.dp
                )

        )


        /*
         * ========================================================
         * JOBB OLDALI NYÍL
         * ========================================================
         */

        Text(

            text =
                if (collapsed) {
                    "▶"
                } else {
                    "▼"
                },

            fontSize =
                10.sp,

            color =
                MaterialTheme
                    .colorScheme
                    .onSurfaceVariant,

            modifier =
                Modifier.padding(
                    start = 7.dp
                )

        )
    }
}


/*
 * ============================================================
 * BAJNOKSÁG AZONOSÍTÓ
 * ============================================================
 */

private fun liveLeagueKey(
    match: MatchModel
): String {

    return match
        .league
        ?.id
        ?.toString()
        ?: run {

            val countryCode =
                match
                    .country
                    ?.code
                    .orEmpty()

            "${countryCode}_${match.leagueDisplayName}"
        }
}


/*
 * ============================================================
 * ORSZÁGNÉV KINYERÉSE
 * ============================================================
 *
 * Például:
 *
 * "Franciaország · Ligue 2"
 *
 * -> "Franciaország"
 *
 * ============================================================
 */

private fun extractLiveCountry(
    leagueDisplayName: String
): String {

    val separatorIndex =
        leagueDisplayName.indexOf("·")

    if (separatorIndex > 0) {

        return leagueDisplayName
            .substring(
                0,
                separatorIndex
            )
            .trim()
    }


    /*
     * Ha nincs "·", akkor próbáljuk
     * meg a kötőjelet.
     */

    val dashIndex =
        leagueDisplayName.indexOf(" - ")

    if (dashIndex > 0) {

        return leagueDisplayName
            .substring(
                0,
                dashIndex
            )
            .trim()
    }


    /*
     * Ha nincs ország + liga formátum,
     * akkor egyébként kezeljük.
     */

    return "Egyéb"
}


/*
 * ============================================================
 * CSAK A BAJNOKSÁG NEVE
 * ============================================================
 *
 * "Franciaország · Ligue 2"
 *
 * ->
 *
 * "Ligue 2"
 *
 * ============================================================
 */

private fun leagueNameOnly(
    leagueDisplayName: String
): String {

    val separatorIndex =
        leagueDisplayName.indexOf("·")

    if (separatorIndex >= 0) {

        return leagueDisplayName
            .substring(
                separatorIndex + 1
            )
            .trim()
    }

    return leagueDisplayName.trim()
}


/*
 * ============================================================
 * FRIENDLIES / BARÁTSÁGOS
 * ============================================================
 *
 * Ezek minden esetben a lista végére kerülnek.
 *
 * ============================================================
 */

private fun isLiveFriendly(
    leagueName: String
): Boolean {

    val value =
        leagueName
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
            "club friendly"
        ) ||

        value.contains(
            "club friendlies"
        ) ||

        value.contains(
            "international friendly"
        ) ||

        value.contains(
            "international friendlies"
        ) ||

        value.contains(
            "barátságos"
        ) ||

        value.contains(
            "baratsagos"
        )
}


/*
 * ============================================================
 * ABC RENDEZÉS
 * ============================================================
 *
 * Magyar ékezetek normalizálása azért,
 * hogy például:
 *
 * É
 *
 * ne kerüljön teljesen más helyre.
 * ============================================================
 */

private fun normalizeLiveSort(
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
 * MECCS DÁTUM PARSOLÁSA
 * ============================================================
 */

private fun parseLiveDate(
    value: String?
): Date? {

    if (
        value.isNullOrBlank()
    ) {
        return null
    }


    val patterns =
        listOf(

            /*
             * ISO + timezone
             */
            "yyyy-MM-dd'T'HH:mm:ssXXX",

            /*
             * ISO + millis + timezone
             */
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",

            /*
             * UTC Z
             */
            "yyyy-MM-dd'T'HH:mm:ss'Z'",

            /*
             * UTC Z + millis
             */
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",

            /*
             * timezone nélküli
             */
            "yyyy-MM-dd'T'HH:mm:ss"

        )


    for (
        pattern in patterns
    ) {

        try {

            val formatter =
                SimpleDateFormat(
                    pattern,
                    Locale.US
                )


            /*
             * Z formátum esetén UTC.
             */

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


            val parsed =
                formatter.parse(
                    value
                )


            if (parsed != null) {

                return parsed

            }

        } catch (
            _: ParseException
        ) {

            /*
             * Próbáljuk a következő
             * dátumformátumot.
             */

        }
    }


    return null
}
