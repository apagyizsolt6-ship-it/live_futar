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
import com.livefutar.app.model.OddValue
import java.util.Locale

/**
 * OddsSection
 *
 * Odds fül teljes tartalma.
 *
 * Funkciók:
 * - prémium odds megjelenítés
 * - piacok csoportosítása
 * - bookmaker szerinti rendezés
 * - odds értékek szűrése
 * - magyar piaci elnevezések
 * - hazai / döntetlen / vendég sorrend az 1X2 piacon
 * - oddskártyák megnyitása / összecsukása
 * - odds kattintással szelvényhez adás
 * - API hiba kezelése
 *
 * Fontos:
 * A fájl nem használja az ExpandMore / ExpandLess Compose ikonokat,
 * így nincs szükség a material-icons-extended dependencyre.
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
            val type = if (match.isLive) {
                "live"
            } else {
                "prematch"
            }

            val response = apiService.getOdds(
                apiKey = apiKey,
                matchId = match.id,
                oddsType = type,
                limit = 5
            )

            odds = response.data
                .orEmpty()
                .firstOrNull {
                    it.matchId == match.id
                }
                ?.odds
                .orEmpty()
                .flatMap { bookmaker ->

                    val validValues = bookmaker.values
                        .orEmpty()
                        .filter { value ->

                            val odd = value.odd

                            !value.value.isNullOrBlank() &&
                                odd != null &&
                                odd.isFinite() &&
                                odd > 0.0
                        }

                    if (validValues.isEmpty()) {
                        emptyList()
                    } else {
                        listOf(
                            bookmaker.copy(
                                values = validValues
                            )
                        )
                    }
                }

            if (odds.isEmpty()) {
                errorMessage =
                    "Ehhez a mérkőzéshez jelenleg nincs elérhető odds."
            }

        } catch (_: Exception) {

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

/* ============================================================
   LOADING
   ============================================================ */

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

/* ============================================================
   EMPTY STATE
   ============================================================ */

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
                    text =
                        message
                            ?: "Az odds adat jelenleg nem érhető el.",

                    fontSize = 13.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/* ============================================================
   ODDS LIST
   ============================================================ */

@Composable
private fun OddsList(
    odds: List<BookmakerOdd>,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    val markets = odds
        .groupBy { bookmaker ->

            bookmaker.market
                ?.trim()
                .takeUnless {
                    it.isNullOrBlank()
                }
                ?: "Egyéb"
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
                    "Koppints egy oddsszámra a szelvényhez adáshoz",

                fontSize = 12.sp,

                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,

                modifier = Modifier.padding(
                    top = 8.dp,
                    bottom = 2.dp
                )
            )
        }

        items(
            items = markets,
            key = {
                it.first
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

/* ============================================================
   MARKET CARD
   ============================================================ */

@Composable
private fun OddsMarketCard(
    marketName: String,
    bookmakers: List<BookmakerOdd>,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    var expanded by remember(
        marketName,
        match.id
    ) {
        mutableStateOf(true)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
            },

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme
                    .colorScheme
                    .surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Text(
                    text = huMarket(marketName),

                    fontSize = 15.sp,

                    fontWeight =
                        FontWeight.Bold,

                    modifier = Modifier.weight(1f)
                )

                /*
                 * Nem használunk ExpandMore / ExpandLess ikont.
                 * Így nincs szükség material-icons-extended csomagra.
                 */
                Text(
                    text =
                        if (expanded) {
                            "⌃"
                        } else {
                            "⌄"
                        },

                    fontSize = 22.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            if (expanded) {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                bookmakers.forEach { bookmaker ->

                    OddsBookmakerBlock(
                        bookmaker = bookmaker,
                        marketName = marketName,
                        match = match,
                        onAddToSlip = onAddToSlip
                    )
                }

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text = "⌄ Összes odds megjelenítése",

                    fontSize = 12.sp,

                    fontWeight =
                        FontWeight.Bold,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        }
    }
}

/* ============================================================
   BOOKMAKER BLOCK
   ============================================================ */

@Composable
private fun OddsBookmakerBlock(
    bookmaker: BookmakerOdd,
    marketName: String,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    val bookmakerName =
        bookmaker.bookmakerName
            ?.trim()
            .takeUnless {
                it.isNullOrBlank()
            }
            ?: "Iroda"

    Text(
        text = bookmakerName,

        fontSize = 11.sp,

        fontWeight =
            FontWeight.SemiBold,

        color =
            MaterialTheme
                .colorScheme
                .onSurfaceVariant,

        modifier =
            Modifier.padding(
                bottom = 6.dp
            )
    )

    /*
     * 1X2 esetén:
     *
     * Hazai
     * Döntetlen
     * Vendég
     *
     * legyen a sorrend.
     */
    val values = bookmaker.values
        .orEmpty()
        .sortedWith(
            compareBy {
                selectionOrder(
                    selection = it.value.orEmpty(),
                    marketName = marketName
                )
            }
        )

    values.forEach { value ->

        OddsRow(
            value = value,
            marketName = marketName,
            bookmakerName = bookmakerName,
            match = match,
            onAddToSlip = onAddToSlip
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )
    }
}

/* ============================================================
   ODDS ROW
   ============================================================ */

@Composable
private fun OddsRow(
    value: OddValue,
    marketName: String,
    bookmakerName: String,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    val selection =
        value.value
            ?.trim()
            .orEmpty()

    val odd =
        value.odd
            ?: return

    if (
        selection.isBlank() ||
        !odd.isFinite() ||
        odd <= 0.0
    ) {
        return
    }

    val label = selectionLabel(
        selection = selection,
        match = match
    )

    Row(
        modifier = Modifier.fillMaxWidth(),

        horizontalArrangement =
            Arrangement.spacedBy(8.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = label,

                fontSize = 13.sp,

                fontWeight =
                    FontWeight.Medium,

                maxLines = 1,

                overflow =
                    TextOverflow.Ellipsis
            )

            /*
             * Az eredeti API értéket is megtartjuk
             * másodlagos sorban.
             */
            if (label != selection) {

                Text(
                    text = selection,

                    fontSize = 10.sp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,

                    maxLines = 1,

                    overflow =
                        TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(10.dp)
                )

                .background(
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.10f)
                )

                .border(
                    width = 1.dp,

                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                            .copy(alpha = 0.45f),

                    shape =
                        RoundedCornerShape(10.dp)
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
                                selection,

                            odd =
                                odd,

                            bookmakerName =
                                bookmakerName
                        )
                    }
                }

                .padding(
                    horizontal = 16.dp,
                    vertical = 9.dp
                ),

            contentAlignment =
                Alignment.Center
        ) {

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
}

/* ============================================================
   SELECTION LABEL
   ============================================================ */

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
        "1" ->
            match.homeTeam?.name
                ?: "Hazai"

        "away",
        "2" ->
            match.awayTeam?.name
                ?: "Vendég"

        "draw",
        "x" ->
            "Döntetlen"

        "over" ->
            "Több"

        "under" ->
            "Kevesebb"

        "yes" ->
            "Igen"

        "no" ->
            "Nem"

        "none" ->
            "Nincs"

        else ->
            selection
    }
}

/* ============================================================
   SELECTION ORDER
   ============================================================ */

private fun selectionOrder(
    selection: String,
    marketName: String
): Int {

    val market =
        marketName
            .trim()
            .lowercase(Locale.ROOT)

    val value =
        selection
            .trim()
            .lowercase(Locale.ROOT)

    /*
     * Kifejezetten 1X2 / mérkőzés eredménye:
     *
     * 1 = Hazai
     * X = Döntetlen
     * 2 = Vendég
     */
    if (
        market == "1x2" ||
        market == "full time result" ||
        market == "match result" ||
        market.contains("match result")
    ) {

        return when (value) {

            "home",
            "1" -> 0

            "draw",
            "x" -> 1

            "away",
            "2" -> 2

            else -> 3
        }
    }

    /*
     * Over / Under piacoknál:
     *
     * Több
     * Kevesebb
     */
    if (
        market.contains("total") ||
        market.contains("over") ||
        market.contains("under")
    ) {

        return when (value) {

            "over" -> 0

            "under" -> 1

            else -> 2
        }
    }

    return 3
}

/* ============================================================
   HUNGARIAN MARKET NAMES
   ============================================================ */

private fun huMarket(
    market: String
): String {

    val normalized =
        market
            .trim()
            .lowercase(Locale.ROOT)

    return when {

        normalized == "full time result" ||
        normalized == "match result" ||
        normalized == "1x2" ->

            "Mérkőzés eredménye (1X2)"

        normalized == "double chance" ->

            "Kettős esély"

        normalized == "both teams to score" ||
        normalized == "btts" ->

            "Mindkét csapat szerez gólt"

        normalized == "total goals" ||
        normalized == "goals over/under" ||
        normalized == "over/under" ->

            "Összes gól"

        normalized == "total corners" ->

            "Összes szöglet"

        normalized == "corners over/under" ->

            "Szögletek száma"

        normalized == "total cards" ->

            "Összes lap"

        normalized == "cards over/under" ->

            "Lapok száma"

        normalized == "draw no bet" ->

            "Döntetlennél visszajár"

        normalized == "first half result" ->

            "1. félidő eredménye"

        normalized == "second half result" ->

            "2. félidő eredménye"

        normalized == "correct score" ->

            "Pontos eredmény"

        normalized == "team total" ->

            "Csapat góljai"

        normalized == "first team to score" ->

            "Első csapat, amely gólt szerez"

        normalized == "asian handicap" ->

            "Ázsiai hendikep"

        normalized == "handicap" ->

            "Hendikep"

        normalized == "match winner" ->

            "Mérkőzés győztese"

        normalized == "half time result" ->

            "Félidei eredmény"

        normalized == "total" ->

            "Összesen"

        normalized == "player props" ->

            "Játékos fogadások"

        normalized == "player to score" ->

            "Játékos gólt szerez"

        normalized == "clean sheet" ->

            "Kapott gól nélkül"

        normalized == "winning margin" ->

            "Győzelmi különbség"

        normalized == "to win either half" ->

            "Bármelyik félidő megnyerése"

        normalized == "to win both halves" ->

            "Mindkét félidő megnyerése"

        else ->

            market
                .replaceFirstChar {
                    if (it.isLowerCase()) {
                        it.titlecase(Locale.ROOT)
                    } else {
                        it.toString()
                    }
                }
    }
}
