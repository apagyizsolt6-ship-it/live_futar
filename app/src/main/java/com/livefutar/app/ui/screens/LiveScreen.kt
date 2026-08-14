package com.livefutar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/*
 * ============================================================
 * LIVE FUTÁR
 * Élő mérkőzések
 *
 * Változás:
 * - ország ABC
 * - bajnokság ABC
 * - Friendlies mindig legvégén
 * - bajnokságok nyitható / zárható
 * ============================================================
 */

private data class LiveLeagueGroup(
    val key: String,
    val displayName: String,
    val countryName: String,
    val matches: List<MatchModel>
)

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

    /*
     * ============================================================
     * BAJNOKSÁGOK NYITOTT / ZÁRT ÁLLAPOTA
     * ============================================================
     */

    val collapsedLeagues =
        rememberSaveable(
            saver =
                androidx.compose.runtime.saveable
                    .mapSaver(
                        save = {
                            it.toMap()
                        },
                        restore = {
                            mutableMapOf<String, Boolean>()
                                .apply {
                                    putAll(
                                        it.mapKeys { entry ->
                                            entry.key.toString()
                                        }
                                    )
                                }
                        }
                    )
        ) {
            mutableStateMapOf<String, Boolean>()
        }

    /*
     * ============================================================
     * CSAK ÉLŐ MECCSEK
     * ============================================================
     */

    val liveMatches =
        matches.filter {
            it.isLive
        }

    /*
     * ============================================================
     * ÉLŐ MECCSEK SZÁMA
     * ============================================================
     */

    val liveCount =
        liveMatches.size

    /*
     * ============================================================
     * BAJNOKSÁGOK CSOPORTOSÍTÁSA
     * ============================================================
     */

    val grouped =
        liveMatches

            .groupBy { match ->
                liveLeagueKey(
                    match
                )
            }

            .map { (key, list) ->

                val first =
                    list.firstOrNull()

                val displayName =
                    first
                        ?.leagueDisplayName
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
                        list.sortedWith(

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

            /*
             * =====================================================
             * VÉGLEGES RENDEZÉS
             *
             * 0 = normál bajnokság
             * 1 = Friendly
             *
             * Ez garantálja, hogy a barátságos
             * mérkőzések mindig legalul legyenek.
             * =====================================================
             */

            .sortedWith(

                compareBy<LiveLeagueGroup> {

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

                    normalizeLiveSort(
                        it.countryName
                    )

                }.thenBy {

                    normalizeLiveSort(
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
                                Modifier.width(8.dp)
                        )

                        Text(

                            text =
                                "Élő mérkőzések",

                            fontSize = 19.sp,

                            fontWeight =
                                FontWeight.Bold
                        )

                        Spacer(
                            modifier =
                                Modifier.width(8.dp)
                        )

                        Text(

                            text =
                                "($liveCount)",

                            fontSize = 14.sp,

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                AccentGreen
                        )
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

                        fontSize = 22.sp,

                        color =
                            AccentGreen,

                        modifier =
                            Modifier
                                .padding(
                                    end = 14.dp
                                )
                                .clickable(
                                    enabled =
                                        !isRefreshing
                                ) {
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

        if (liveMatches.isEmpty()) {

            Box(

                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(
                            paddingValues
                        ),

                contentAlignment =
                    Alignment.Center
            ) {

                Column(

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "⚽",
                        fontSize = 42.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.height(
                                12.dp
                            )
                    )

                    Text(

                        text =
                            "Jelenleg nincs élő mérkőzés",

                        fontSize = 15.sp,

                        fontWeight =
                            FontWeight.Medium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }
            }

            return@Scaffold
        }

        LazyColumn(

            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        paddingValues
                    )
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

            grouped.forEach { group ->

                val first =
                    group.matches.firstOrNull()

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
                            first
                                ?.country
                                ?.logo,

                        leagueLogo =
                            first
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
                 * BAJNOKSÁG MECCSEI
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

                        MatchCard(

                            match =
                                match,

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

            item {

                Spacer(
                    modifier =
                        Modifier.height(
                            20.dp
                        )
                )
            }
        }
    }
}

/*
 * ============================================================
 * ÉLŐ BAJNOKSÁG FEJLÉC
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
         * NYITÁS / ZÁRÁS
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
                        .size(17.dp)
                        .clip(
                            RoundedCornerShape(
                                2.dp
                            )
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
                    Modifier.width(8.dp)
            )
        }

        /*
         * BAJNOKSÁG NEVE
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
                            alpha =
                                0.75f
                        )
            )
        }

        /*
         * Kinyitott / becsukott állapot
         */
        Text(

            text =
                if (collapsed) {
                    "▶"
                } else {
                    "▼"
                },

            fontSize = 12.sp,

            color =
                AccentGreen,

            modifier =
                Modifier
                    .padding(
                        start = 8.dp
                    )
        )
    }
}

/*
 * ============================================================
 * BAJNOKSÁG ID
 * ============================================================
 */

private fun liveLeagueKey(
    match: MatchModel
): String {

    return match.league
        ?.id
        ?.toString()
        ?: "${match.country?.code.orEmpty()}_${match.leagueDisplayName}"
}

/*
 * ============================================================
 * ORSZÁG KINYERÉSE
 * ============================================================
 */

private fun extractLiveCountry(
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

    return "Egyéb"
}

/*
 * ============================================================
 * FRIENDLY
 * ============================================================
 */

private fun isLiveFriendly(
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
 * ABC RENDEZÉS
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
 * DÁTUM
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
