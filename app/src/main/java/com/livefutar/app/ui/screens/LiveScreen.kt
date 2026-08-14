package com.livefutar.app.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.MatchCard
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
     * CSAK ÉLŐ MECCSEK
     * ========================================================
     */

    val liveMatches =
        matches.filter { match ->

            match.isLive

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

    val grouped =
        liveMatches

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


                /*
                 * FRISSÍTÉS
                 */

                actions = {

                    Text(

                        text =
                            if (isRefreshing) {
                                "…"
                            } else {
                                "↻"
                            },

                        fontSize =
                            23.sp,

                        color =
                            AccentGreen,

                        modifier =
                            Modifier
                                .padding(
                                    end = 16.dp
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


        /*
         * ====================================================
         * NINCS ÉLŐ MECCS
         * ====================================================
         */

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

                        text =
                            "⚽",

                        fontSize =
                            42.sp

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

                        fontSize =
                            15.sp,

                        fontWeight =
                            FontWeight.Medium,

                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant

                    )
                }
            }

        } else {


            /*
             * =================================================
             * ÉLŐ LISTA
             * =================================================
             */

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
                        modifier =
                            Modifier.height(
                                20.dp
                            )
                    )
                }
            }
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
