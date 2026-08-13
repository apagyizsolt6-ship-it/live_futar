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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefutar.app.data.ApiKeyManager
import com.livefutar.app.data.FootballApiService
import com.livefutar.app.model.BetSlipSelection
import com.livefutar.app.model.BookmakerOdd
import com.livefutar.app.model.MatchModel
import java.util.Locale

/**
 * OddsSection
 *
 * Teljesen új Odds megjelenítő rendszer.
 *
 * Főbb tulajdonságok:
 * - csak az Odds fül megnyitásakor kéri le az oddsokat
 * - minden API-ból érkező piac megmarad
 * - piacok szerint csoportosít
 * - minden kimenetelnél a legjobb odds kerül előre
 * - a legjobb odds külön ki van emelve
 * - magyar piacnevek
 * - magyar kimenetelnevek
 * - ismeretlen piacokat is megtartja
 * - API hiba esetén nem omlik össze az alkalmazás
 * - hibás odds értékeket biztonságosan kihagy
 * - az odds gomb továbbra is hozzáadható a szelvényhez
 */
@Composable
fun OddsSection(
    match: MatchModel,
    apiService: FootballApiService,
    onAddToSlip: (BetSlipSelection) -> Unit
) {
    val context = LocalContext.current

    var odds by remember(match.id) {
        mutableStateOf<List<BookmakerOdd>>(emptyList())
    }

    var isLoading by remember(match.id) {
        mutableStateOf(true)
    }

    var errorMessage by remember(match.id) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(match.id, match.isLive) {
        isLoading = true
        errorMessage = null
        odds = emptyList()

        val apiKey = ApiKeyManager.getApiKey(context)

        if (apiKey.isBlank()) {
            errorMessage = "Nincs beállítva API-kulcs."
            isLoading = false
            return@LaunchedEffect
        }

        try {
            val oddsType = if (match.isLive) {
                "live"
            } else {
                "prematch"
            }

            val response = apiService.getOdds(
                apiKey = apiKey,
                matchId = match.id,
                oddsType = oddsType,
                limit = 5
            )

            odds = response.data
                .orEmpty()
                .firstOrNull { item ->
                    item.matchId == match.id
                }
                ?.odds
                .orEmpty()
                .mapNotNull { bookmaker ->

                    val validValues = bookmaker.values
                        .orEmpty()
                        .filter { value ->

                            val selection = value.value
                                ?.trim()
                                .orEmpty()

                            val odd = value.odd

                            selection.isNotBlank() &&
                                odd != null &&
                                odd.isFinite() &&
                                odd > 0.0
                        }

                    if (validValues.isEmpty()) {
                        null
                    } else {
                        bookmaker.copy(
                            values = validValues
                        )
                    }
                }

            if (odds.isEmpty()) {
                errorMessage =
                    "Ehhez a mérkőzéshez jelenleg nincs elérhető odds."
            }

        } catch (e: Exception) {

            odds = emptyList()

            errorMessage =
                "Az oddsok most nem tölthetők be."

        } finally {

            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {

        when {

            isLoading -> {
                OddsLoading()
            }

            odds.isEmpty() -> {
                OddsEmptyState(
                    message = errorMessage
                )
            }

            else -> {
                OddsList(
                    odds = odds,
                    match = match,
                    onAddToSlip = onAddToSlip
                )
            }
        }
    }
}


/* -------------------------------------------------------------------------- */
/* BETÖLTÉS                                                                   */
/* -------------------------------------------------------------------------- */

@Composable
private fun OddsLoading() {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            CircularProgressIndicator(
                modifier = Modifier.size(38.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(
                modifier = Modifier.height(12.dp)
            )

            Text(
                text = "Oddsok betöltése...",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* ÜRES ÁLLAPOT                                                               */
/* -------------------------------------------------------------------------- */

@Composable
private fun OddsEmptyState(
    message: String?
) {

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surface
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Nincs elérhető odds",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = message
                        ?: "Az odds adat jelenleg nem érhető el.",
                    fontSize = 13.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}


/* -------------------------------------------------------------------------- */
/* ODDS LISTA                                                                 */
/* -------------------------------------------------------------------------- */

@Composable
private fun OddsList(
    odds: List<BookmakerOdd>,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    /*
     * FONTOS:
     *
     * Itt NEM szűrjük a piacokat.
     *
     * Minden API-ból érkező market megmarad.
     *
     * A groupBy kizárólag a megjelenítés rendszerezésére szolgál.
     */

    val markets = odds
        .groupBy { bookmaker ->

            bookmaker.market
                ?.trim()
                .takeUnless {
                    it.isNullOrBlank()
                }
                ?: "Egyéb piac"
        }
        .toList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        item {

            Text(
                text =
                    "Minden elérhető piac • a legjobb odds kiemelve",
                fontSize = 12.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    top = 8.dp,
                    bottom = 2.dp
                )
            )
        }

        items(
            items = markets,
            key = {
                "${it.first}_${it.second.hashCode()}"
            }
        ) { marketEntry ->

            OddsMarketCard(
                marketName = marketEntry.first,
                bookmakers = marketEntry.second,
                match = match,
                onAddToSlip = onAddToSlip
            )
        }

        item {

            Spacer(
                modifier = Modifier.height(20.dp)
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* PIAC KÁRTYA                                                                */
/* -------------------------------------------------------------------------- */

@Composable
private fun OddsMarketCard(
    marketName: String,
    bookmakers: List<BookmakerOdd>,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    val translatedMarket =
        huMarket(marketName)

    /*
     * Az azonos kimeneteleket összegyűjtjük.
     *
     * Példa:
     *
     * Over
     *   2.05 LeoVegas
     *   2.00 Vbet
     *   1.98 Stake
     *
     * Így a legjobb odds automatikusan előre kerül.
     */

    val selections = bookmakers
        .flatMap { bookmaker ->

            val bookmakerName =
                bookmaker.bookmakerName
                    ?.trim()
                    .takeUnless {
                        it.isNullOrBlank()
                    }
                    ?: "Ismeretlen iroda"

            bookmaker.values
                .orEmpty()
                .mapNotNull { value ->

                    val selection =
                        value.value
                            ?.trim()
                            .orEmpty()

                    val odd =
                        value.odd

                    if (
                        selection.isBlank() ||
                        odd == null ||
                        !odd.isFinite() ||
                        odd <= 0.0
                    ) {

                        null

                    } else {

                        OddsDisplayValue(
                            selection = selection,
                            bookmakerName =
                                bookmakerName,
                            odd = odd
                        )
                    }
                }
        }

        /*
         * Azonos kimenetel összegyűjtése.
         */
        .groupBy {
            normalizeSelectionKey(
                it.selection
            )
        }

        /*
         * Minden kimenetelnél:
         * legmagasabb odds → első.
         */
        .values
        .map { values ->

            values.sortedByDescending {
                it.odd
            }
        }

    if (selections.isEmpty()) {
        return
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 13.dp
                )
        ) {

            Text(
                text = translatedMarket,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(4.dp)
            )

            Text(
                text =
                    "${selections.sumOf { it.size }} elérhető odds",
                fontSize = 11.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(
                modifier = Modifier.height(10.dp)
            )

            selections.forEachIndexed {
                    index,
                    selectionOdds ->

                OddsSelectionGroup(
                    selectionOdds =
                        selectionOdds,
                    match = match,
                    marketName = marketName,
                    onAddToSlip =
                        onAddToSlip
                )

                if (
                    index < selections.lastIndex
                ) {

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )
                }
            }
        }
    }
}


/* -------------------------------------------------------------------------- */
/* KIMENETEL CSOPORT                                                          */
/* -------------------------------------------------------------------------- */

@Composable
private fun OddsSelectionGroup(
    selectionOdds: List<OddsDisplayValue>,
    match: MatchModel,
    marketName: String,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    val selection =
        selectionOdds
            .firstOrNull()
            ?.selection
            ?: return

    val displayLabel =
        selectionLabel(
            selection = selection,
            match = match
        )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = displayLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        /*
         * Az eredeti API értéket is megmutatjuk,
         * ha a magyar fordítás eltér.
         *
         * Példa:
         *
         * Több
         * Over
         */

        if (
            !displayLabel.equals(
                selection,
                ignoreCase = true
            )
        ) {

            Text(
                text = selection,
                fontSize = 10.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(
                    top = 1.dp
                )
            )
        }

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        selectionOdds.forEachIndexed {
                index,
                value ->

            OddsBookmakerRow(
                value = value,
                isBest = index == 0,
                match = match,
                marketName = marketName,
                onAddToSlip = onAddToSlip
            )

            if (
                index < selectionOdds.lastIndex
            ) {

                Spacer(
                    modifier = Modifier.height(5.dp)
                )
            }
        }
    }
}


/* -------------------------------------------------------------------------- */
/* FOGADÓIRODA SOR                                                            */
/* -------------------------------------------------------------------------- */

@Composable
private fun OddsBookmakerRow(
    value: OddsDisplayValue,
    isBest: Boolean,
    match: MatchModel,
    marketName: String,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            Arrangement.spacedBy(8.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 2.dp)
        ) {

            Text(
                text = value.bookmakerName,
                fontSize = 11.sp,
                fontWeight =
                    if (isBest) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    },
                color =
                    if (isBest) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            if (isBest) {

                Text(
                    text = "LEGJOBB ODDS",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color =
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(
                        top = 1.dp
                    )
                )
            }
        }

        /*
         * Odds gomb.
         *
         * Kattintás → hozzáadás a szelvényhez.
         */

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(11.dp)
                )
                .background(
                    if (isBest) {
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.08f)
                    }
                )
                .border(
                    width =
                        if (isBest) {
                            1.5.dp
                        } else {
                            1.dp
                        },
                    color =
                        MaterialTheme.colorScheme.primary
                            .copy(
                                alpha =
                                    if (isBest) {
                                        0.65f
                                    } else {
                                        0.35f
                                    }
                            ),
                    shape =
                        RoundedCornerShape(11.dp)
                )
                .clickable {

                    onAddToSlip(
                        BetSlipSelection(
                            matchId =
                                match.id,

                            homeName =
                                match.homeTeam?.name
                                    ?: "Hazai",

                            awayName =
                                match.awayTeam?.name
                                    ?: "Vendég",

                            leagueName =
                                match.leagueDisplayName,

                            market =
                                marketName,

                            selection =
                                value.selection,

                            odd =
                                value.odd,

                            bookmakerName =
                                value.bookmakerName
                        )
                    )
                }
                .padding(
                    horizontal = 15.dp,
                    vertical = 8.dp
                ),
            contentAlignment =
                Alignment.Center
        ) {

            Text(
                text =
                    String.format(
                        Locale.US,
                        "%.2f",
                        value.odd
                    ),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.primary
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* ODDS MEGJELENÍTÉSI MODEL                                                   */
/* -------------------------------------------------------------------------- */

private data class OddsDisplayValue(
    val selection: String,
    val bookmakerName: String,
    val odd: Double
)


/* -------------------------------------------------------------------------- */
/* KIMENETEL NORMALIZÁLÁS                                                      */
/* -------------------------------------------------------------------------- */

private fun normalizeSelectionKey(
    value: String
): String {

    return value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(" ", "")
        .replace(",", ".")
}


/* -------------------------------------------------------------------------- */
/* KIMENETEL MAGYARÍTÁS                                                        */
/* -------------------------------------------------------------------------- */

private fun selectionLabel(
    selection: String,
    match: MatchModel
): String {

    return when (
        selection
            .trim()
            .lowercase(Locale.ROOT)
    ) {

        "home",
        "1" -> {

            match.homeTeam?.name
                ?: "Hazai csapat"
        }

        "away",
        "2" -> {

            match.awayTeam?.name
                ?: "Vendég csapat"
        }

        "draw",
        "x" -> {

            "Döntetlen"
        }

        "yes" -> {

            "Igen"
        }

        "no" -> {

            "Nem"
        }

        "over" -> {

            "Több"
        }

        "under" -> {

            "Kevesebb"
        }

        "both teams to score yes" -> {

            "Igen"
        }

        "both teams to score no" -> {

            "Nem"
        }

        else -> {

            translateSelection(
                selection
            )
        }
    }
}


/* -------------------------------------------------------------------------- */
/* PIAC NEVEK MAGYARÍTÁSA                                                      */
/* -------------------------------------------------------------------------- */

private fun huMarket(
    market: String
): String {

    val raw =
        market.trim()

    val lower =
        raw.lowercase(Locale.ROOT)

    return when {

        /*
         * 1X2
         */

        lower == "full time result" ||
            lower == "match result" ||
            lower == "1x2" ||
            lower == "match winner" -> {

            "Mérkőzés eredménye (1X2)"
        }


        /*
         * Kettős esély
         */

        lower == "double chance" -> {

            "Kettős esély"
        }


        /*
         * Mindkét csapat gólt szerez
         */

        lower == "both teams to score" ||
            lower == "btts" -> {

            "Mindkét csapat szerez gólt"
        }


        /*
         * Gólok
         *
         * Példa:
         * Total Goals 2.5
         *
         * → Gólok száma 2.5
         */

        lower.startsWith("total goals") -> {

            "Gólok száma" +
                raw.removePrefixIgnoreCase(
                    "Total Goals"
                )
        }


        /*
         * Szögletek
         */

        lower.startsWith("total corners") -> {

            "Szögletek száma" +
                raw.removePrefixIgnoreCase(
                    "Total Corners"
                )
        }


        /*
         * Lapok
         */

        lower.startsWith("total cards") -> {

            "Lapok száma" +
                raw.removePrefixIgnoreCase(
                    "Total Cards"
                )
        }


        /*
         * Lövések
         */

        lower.startsWith("total shots") -> {

            "Lövések száma" +
                raw.removePrefixIgnoreCase(
                    "Total Shots"
                )
        }


        /*
         * Ázsiai hendikep
         */

        lower.startsWith("asian handicap") -> {

            "Ázsiai hendikep" +
                raw.removePrefixIgnoreCase(
                    "Asian Handicap"
                )
        }


        /*
         * Ázsiai gólhatár
         */

        lower.startsWith("asian total") -> {

            "Ázsiai gólhatár" +
                raw.removePrefixIgnoreCase(
                    "Asian Total"
                )
        }


        /*
         * Pontos eredmény
         */

        lower.startsWith("correct score") -> {

            "Pontos eredmény" +
                raw.removePrefixIgnoreCase(
                    "Correct Score"
                )
        }


        /*
         * 1. félidő
         */

        lower.startsWith("half time result") ||
            lower.startsWith("halftime result") ||
            lower == "1st half result" -> {

            "1. félidő eredménye" +
                raw
                    .removePrefixIgnoreCase(
                        "Half Time Result"
                    )
                    .removePrefixIgnoreCase(
                        "Halftime Result"
                    )
                    .removePrefixIgnoreCase(
                        "1st Half Result"
                    )
        }


        /*
         * 2. félidő
         */

        lower.startsWith("second half result") ||
            lower == "2nd half result" -> {

            "2. félidő eredménye" +
                raw
                    .removePrefixIgnoreCase(
                        "Second Half Result"
                    )
                    .removePrefixIgnoreCase(
                        "2nd Half Result"
                    )
        }


        /*
         * Döntetlennél visszajár
         */

        lower == "draw no bet" -> {

            "Döntetlennél visszajár"
        }


        /*
         * Csapat összes gólja
         */

        lower.startsWith("team total") -> {

            "Csapat góljai" +
                raw.removePrefixIgnoreCase(
                    "Team Total"
                )
        }


        /*
         * Hazai csapat összes gólja
         */

        lower.startsWith("home team total") -> {

            "Hazai csapat góljai" +
                raw.removePrefixIgnoreCase(
                    "Home Team Total"
                )
        }


        /*
         * Vendég csapat összes gólja
         */

        lower.startsWith("away team total") -> {

            "Vendég csapat góljai" +
                raw.removePrefixIgnoreCase(
                    "Away Team Total"
                )
        }


        /*
         * Első gólszerző csapat
         */

        lower.contains("first team to score") ||
            lower.contains("first goal") -> {

            "Ki szerzi az első gólt?"
        }


        /*
         * Utolsó gólszerző
         */

        lower.contains("last team to score") ||
            lower.contains("last goal") -> {

            "Ki szerzi az utolsó gólt?"
        }


        /*
         * Győzelmi különbség
         */

        lower.contains("winning margin") -> {

            "Győzelmi különbség" +
                raw.removePrefixIgnoreCase(
                    "Winning Margin"
                )
        }


        /*
         * Félidő / végeredmény
         */

        lower.contains("half time / full time") ||
            lower.contains("ht/ft") -> {

            "Félidő / végeredmény"
        }


        /*
         * Ismételt Correct Score felismerés,
         * ha az API más formátumot küld.
         */

        lower.contains("correct score") -> {

            "Pontos eredmény" +
                raw.removePrefixIgnoreCase(
                    "Correct Score"
                )
        }


        /*
         * Ismételt BTTS felismerés,
         * ha hosszabb piacnevet küld az API.
         */

        lower.contains("both teams") &&
            lower.contains("score") -> {

            "Mindkét csapat szerez gólt"
        }


        /*
         * Egyéb gólpiac.
         */

        lower.contains("over") &&
            lower.contains("under") &&
            lower.contains("goal") -> {

            "Gólok száma" +
                raw.removePrefixIgnoreCase(
                    "Goals"
                )
        }


        /*
         * ISMERETLEN PIAC
         *
         * Nagyon fontos:
         * NEM dobjuk el.
         *
         * Az API által küldött eredeti nevet
         * jelenítjük meg.
         */

        else -> {

            raw
        }
    }
}


/* -------------------------------------------------------------------------- */
/* KIMENETELEK TOVÁBBI MAGYARÍTÁSA                                            */
/* -------------------------------------------------------------------------- */

private fun translateSelection(
    selection: String
): String {

    val raw =
        selection.trim()

    return when (
        raw.lowercase(Locale.ROOT)
    ) {

        "home" -> {

            "Hazai"
        }

        "away" -> {

            "Vendég"
        }

        "draw" -> {

            "Döntetlen"
        }

        "yes" -> {

            "Igen"
        }

        "no" -> {

            "Nem"
        }

        "over" -> {

            "Több"
        }

        "under" -> {

            "Kevesebb"
        }

        else -> {

            raw
        }
    }
}


/* -------------------------------------------------------------------------- */
/* SEGÉDFÜGGVÉNY                                                              */
/* -------------------------------------------------------------------------- */

private fun String.removePrefixIgnoreCase(
    prefix: String
): String {

    return if (
        startsWith(
            prefix,
            ignoreCase = true
        )
    ) {

        substring(
            prefix.length
        )

    } else {

        this
    }
}
