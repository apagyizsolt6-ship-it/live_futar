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
 * Új, kompakt odds megjelenítés.
 *
 * FONTOS:
 * - Az API-ból érkező összes piac megmarad.
 * - Az összes bookmaker megmarad.
 * - Az összes odds megmarad.
 * - Alapból csak a legjobb oddsok láthatók.
 * - Piaconként ki-/becsukható a teljes lista.
 * - A részletes lista a bookmaker oddsokat mutatja.
 * - A legjobb odds mindenhol külön kiemelést kap.
 * - A piacnevek és alapvető kimenetelek magyarítva vannak.
 *
 * A szelvényhez adás funkció változatlanul működik:
 * az odds gombra koppintva BetSlipSelection készül.
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
                .mapNotNull { bookmaker ->

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

/* ============================================================
 * LOADING
 * ============================================================
 */

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
                modifier = Modifier.size(38.dp)
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
 * EMPTY STATE
 * ============================================================
 */

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

/* ============================================================
 * FŐ ODDS LISTA
 * ============================================================
 */

@Composable
private fun OddsList(
    odds: List<BookmakerOdd>,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {
    /*
     * Piacok csoportosítása.
     *
     * Minden piac külön kártya lesz.
     */
    val markets = remember(odds) {
        odds
            .groupBy { bookmaker ->
                bookmaker.market
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: "Egyéb"
            }
            .toList()
    }

    /*
     * Alapból az ELSŐ piac legyen nyitva.
     *
     * A többi zárt.
     */
    var expandedMarkets by remember(markets) {
        mutableStateOf(
            if (markets.isNotEmpty()) {
                setOf(markets.first().first)
            } else {
                emptySet()
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        top = 8.dp,
                        bottom = 2.dp
                    )
            ) {
                Text(
                    text =
                        "Minden elérhető piac • legjobb odds elöl",
                    fontSize = 12.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Koppints egy piacra a teljes oddslista megnyitásához.",
                    fontSize = 11.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(
            items = markets,
            key = { entry ->
                entry.first
            }
        ) { marketEntry ->

            val marketName = marketEntry.first
            val bookmakers = marketEntry.second

            val expanded =
                expandedMarkets.contains(marketName)

            OddsMarketCard(
                marketName = marketName,
                bookmakers = bookmakers,
                match = match,
                expanded = expanded,
                onToggle = {
                    expandedMarkets =
                        if (expanded) {
                            expandedMarkets - marketName
                        } else {
                            expandedMarkets + marketName
                        }
                },
                onAddToSlip = onAddToSlip
            )
        }

        item {
            Spacer(
                modifier = Modifier.height(24.dp)
            )
        }
    }
}

/* ============================================================
 * PIAC KÁRTYA
 * ============================================================
 */

@Composable
private fun OddsMarketCard(
    marketName: String,
    bookmakers: List<BookmakerOdd>,
    match: MatchModel,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAddToSlip: (BetSlipSelection) -> Unit
) {
    val marketGroups =
        remember(bookmakers) {
            buildMarketGroups(bookmakers)
        }

    val totalOdds =
        bookmakers
            .flatMap {
                it.values.orEmpty()
            }
            .count {
                val odd = it.odd

                !it.value.isNullOrBlank() &&
                    odd != null &&
                    odd.isFinite() &&
                    odd > 0.0
            }

    val selectionGroups =
        marketGroups
            .sortedByDescending {
                it.bestOdd
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
            modifier = Modifier.fillMaxWidth()
        ) {

            /*
             * FEJLÉC
             */
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onToggle()
                    }
                    .padding(16.dp)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = huMarket(marketName),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow =
                                TextOverflow.Ellipsis
                        )

                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )

                        Text(
                            text =
                                "$totalOdds elérhető odds • " +
                                    "${selectionGroups.size} kimenetel",
                            fontSize = 11.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    Text(
                        text =
                            if (expanded) {
                                "⌃"
                            } else {
                                "⌄"
                            },
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(
                    modifier = Modifier.height(12.dp)
                )

                /*
                 * ÖSSZECSUKVA:
                 *
                 * Csak a legjobb oddsok.
                 *
                 * Legfeljebb 3 kimenetelt mutatunk,
                 * hogy a kártya tényleg kompakt maradjon.
                 */
                if (!expanded) {

                    selectionGroups
                        .take(3)
                        .forEach { group ->

                            CompactBestSelectionRow(
                                group = group,
                                match = match,
                                marketName = marketName,
                                onAddToSlip =
                                    onAddToSlip
                            )

                            Spacer(
                                modifier =
                                    Modifier.height(7.dp)
                            )
                        }

                    if (selectionGroups.size > 3) {
                        Text(
                            text =
                                "+ ${selectionGroups.size - 3} további kimenetel",
                            fontSize = 11.sp,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text =
                            "⌄ Összes odds megjelenítése",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color =
                            MaterialTheme.colorScheme.primary
                    )
                }
            }

            /*
             * KINYITVA:
             *
             * Minden kimenetel + minden bookmaker.
             */
            if (expanded) {

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            MaterialTheme
                                .colorScheme
                                .outline
                                .copy(alpha = 0.12f)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = 16.dp
                        )
                ) {

                    Text(
                        text =
                            "Összes elérhető odds",
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant,
                        modifier =
                            Modifier.padding(
                                top = 12.dp,
                                bottom = 12.dp
                            )
                    )

                    selectionGroups.forEach { group ->

                        ExpandedSelectionGroup(
                            group = group,
                            marketName = marketName,
                            match = match,
                            onAddToSlip =
                                onAddToSlip
                        )

                        Spacer(
                            modifier =
                                Modifier.height(12.dp)
                        )
                    }

                    Text(
                        text =
                            "⌃ Összes odds elrejtése",
                        fontSize = 12.sp,
                        fontWeight =
                            FontWeight.SemiBold,
                        color =
                            MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToggle()
                                }
                                .padding(
                                    vertical = 8.dp
                                ),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/* ============================================================
 * KOMPAKT LEGJOBB ODDS
 * ============================================================
 */

@Composable
private fun CompactBestSelectionRow(
    group: MarketSelectionGroup,
    match: MatchModel,
    marketName: String,
    onAddToSlip: (BetSlipSelection) -> Unit
) {
    val best = group.bestEntry

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                MaterialTheme
                    .colorScheme
                    .primary
                    .copy(alpha = 0.06f)
            )
            .padding(
                horizontal = 10.dp,
                vertical = 8.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = group.displayName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text = best.bookmakerName,
                fontSize = 10.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text = "LEGJOBB ODDS",
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.primary
            )
        }

        BestOddsChip(
            odd = best.odd,
            onClick = {
                addToSlip(
                    match = match,
                    marketName = marketName,
                    selection = best.selection,
                    odd = best.odd,
                    bookmakerName =
                        best.bookmakerName,
                    onAddToSlip = onAddToSlip
                )
            }
        )
    }
}

/* ============================================================
 * KINYITOTT KIMENETEL
 * ============================================================
 */

@Composable
private fun ExpandedSelectionGroup(
    group: MarketSelectionGroup,
    marketName: String,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {

        /*
         * Kimenetel neve
         */
        Text(
            text = group.displayName,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier =
                Modifier.padding(
                    bottom = 7.dp
                )
        )

        /*
         * Bookmakerek:
         *
         * LEGJOBB ODDS FELÜL.
         */
        group.entries.forEachIndexed { index, entry ->

            ExpandedBookmakerRow(
                entry = entry,
                isBest = index == 0,
                marketName = marketName,
                match = match,
                onAddToSlip = onAddToSlip
            )

            if (index != group.entries.lastIndex) {
                Spacer(
                    modifier = Modifier.height(5.dp)
                )
            }
        }
    }
}

/* ============================================================
 * BOOKMAKER SOR
 * ============================================================
 */

@Composable
private fun ExpandedBookmakerRow(
    entry: MarketOddEntry,
    isBest: Boolean,
    marketName: String,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(10.dp)
            )
            .background(
                if (isBest) {
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.08f)
                } else {
                    MaterialTheme
                        .colorScheme
                        .surface
                }
            )
            .padding(
                horizontal = 10.dp,
                vertical = 7.dp
            ),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = entry.bookmakerName,
                fontSize = 12.sp,
                fontWeight =
                    if (isBest) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Medium
                    },
                color =
                    if (isBest) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurface
                    },
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            if (isBest) {
                Text(
                    text = "LEGJOBB ODDS",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }
        }

        OddsChip(
            odd = entry.odd,
            isBest = isBest,
            onClick = {
                addToSlip(
                    match = match,
                    marketName = marketName,
                    selection = entry.selection,
                    odd = entry.odd,
                    bookmakerName =
                        entry.bookmakerName,
                    onAddToSlip = onAddToSlip
                )
            }
        )
    }
}

/* ============================================================
 * LEGJOBB ODDS CHIP
 * ============================================================
 */

@Composable
private fun BestOddsChip(
    odd: Double,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(10.dp)
            )
            .background(
                MaterialTheme
                    .colorScheme
                    .primary
                    .copy(alpha = 0.12f)
            )
            .border(
                width = 1.5.dp,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary,
                shape =
                    RoundedCornerShape(10.dp)
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 14.dp,
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
                    odd
                ),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color =
                MaterialTheme.colorScheme.primary
        )
    }
}

/* ============================================================
 * NORMÁL ODDS CHIP
 * ============================================================
 */

@Composable
private fun OddsChip(
    odd: Double,
    isBest: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(
                RoundedCornerShape(10.dp)
            )
            .background(
                if (isBest) {
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.12f)
                } else {
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(alpha = 0.06f)
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
                    MaterialTheme
                        .colorScheme
                        .primary
                        .copy(
                            alpha =
                                if (isBest) {
                                    0.9f
                                } else {
                                    0.45f
                                }
                        ),
                shape =
                    RoundedCornerShape(10.dp)
            )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 14.dp,
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
                    odd
                ),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color =
                MaterialTheme.colorScheme.primary
        )
    }
}

/* ============================================================
 * ADATSTRUKTÚRÁK
 * ============================================================
 */

private data class MarketOddEntry(
    val selection: String,
    val displayName: String,
    val bookmakerName: String,
    val odd: Double
)

private data class MarketSelectionGroup(
    val selection: String,
    val displayName: String,
    val entries: List<MarketOddEntry>
) {
    val bestEntry: MarketOddEntry
        get() = entries.first()

    val bestOdd: Double
        get() = bestEntry.odd
}

/* ============================================================
 * PIAC FELDOLGOZÁSA
 * ============================================================
 */

private fun buildMarketGroups(
    bookmakers: List<BookmakerOdd>
): List<MarketSelectionGroup> {

    val grouped =
        mutableMapOf<String, MutableList<MarketOddEntry>>()

    bookmakers.forEach { bookmaker ->

        val bookmakerName =
            bookmaker.bookmakerName
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }
                ?: "Ismeretlen iroda"

        bookmaker.values
            .orEmpty()
            .forEach { value ->

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
                    return@forEach
                }

                /*
                 * A groupinghez normalizált kulcs.
                 */
                val key =
                    normalizeSelectionKey(
                        selection
                    )

                val displayName =
                    selectionDisplayName(
                        selection
                    )

                val entry =
                    MarketOddEntry(
                        selection = selection,
                        displayName = displayName,
                        bookmakerName = bookmakerName,
                        odd = odd
                    )

                grouped
                    .getOrPut(key) {
                        mutableListOf()
                    }
                    .add(entry)
            }
    }

    return grouped
        .map { (_, entries) ->

            val sorted =
                entries.sortedByDescending {
                    it.odd
                }

            MarketSelectionGroup(
                selection =
                    sorted.first().selection,
                displayName =
                    sorted.first().displayName,
                entries = sorted
            )
        }
        .sortedByDescending {
            it.bestOdd
        }
}

/* ============================================================
 * SZELVÉNYHEZ ADÁS
 * ============================================================
 */

private fun addToSlip(
    match: MatchModel,
    marketName: String,
    selection: String,
    odd: Double,
    bookmakerName: String,
    onAddToSlip: (BetSlipSelection) -> Unit
) {
    onAddToSlip(
        BetSlipSelection(
            matchId = match.id,
            homeName =
                match.homeTeam?.name
                    ?: "Hazai",
            awayName =
                match.awayTeam?.name
                    ?: "Vendég",
            leagueName =
                match.leagueDisplayName,
            market = marketName,
            selection = selection,
            odd = odd,
            bookmakerName = bookmakerName
        )
    )
}

/* ============================================================
 * KIMENETEL MAGYARÍTÁSA
 * ============================================================
 */

private fun selectionDisplayName(
    selection: String
): String {

    return when (
        selection
            .trim()
            .lowercase(Locale.ROOT)
    ) {

        "home" ->
            "Hazai"

        "away" ->
            "Vendég"

        "draw" ->
            "Döntetlen"

        "1" ->
            "Hazai"

        "x" ->
            "Döntetlen"

        "2" ->
            "Vendég"

        "over" ->
            "Több"

        "under" ->
            "Kevesebb"

        "yes" ->
            "Igen"

        "no" ->
            "Nem"

        "odd" ->
            "Páratlan"

        "even" ->
            "Páros"

        else ->
            selection
    }
}

/* ============================================================
 * KIMENETEL NORMALIZÁLÁSA
 * ============================================================
 */

private fun normalizeSelectionKey(
    selection: String
): String {

    return when (
        selection
            .trim()
            .lowercase(Locale.ROOT)
    ) {

        "home",
        "1" ->
            "home"

        "away",
        "2" ->
            "away"

        "draw",
        "x" ->
            "draw"

        "over" ->
            "over"

        "under" ->
            "under"

        "yes" ->
            "yes"

        "no" ->
            "no"

        else ->
            selection
                .trim()
                .lowercase(Locale.ROOT)
    }
}

/* ============================================================
 * PIACNEVEK MAGYARÍTÁSA
 *
 * Több API-elnevezést is kezelünk.
 * Az ismeretlen piacot eredeti néven hagyjuk,
 * hogy SEMMILYEN piac ne vesszen el.
 * ============================================================
 */

private fun huMarket(
    market: String
): String {

    val key =
        market
            .trim()
            .lowercase(Locale.ROOT)

    return when {

        key == "full time result" ||
            key == "match result" ||
            key == "1x2" ->
            "Mérkőzés eredménye (1X2)"

        key == "double chance" ->
            "Kettős esély"

        key == "both teams to score" ||
            key == "btts" ->
            "Mindkét csapat szerez gólt"

        key == "total goals" ||
            key == "goals over/under" ||
            key == "over/under" ->
            "Gólok száma"

        key == "draw no bet" ->
            "Döntetlennél visszajár"

        key == "first half result" ->
            "1. félidő eredménye"

        key == "second half result" ->
            "2. félidő eredménye"

        key == "correct score" ->
            "Pontos eredmény"

        key == "team total" ->
            "Csapat góljai"

        key == "asian handicap" ||
            key == "asian handicap -0.5/+0.5" ->
            "Ázsiai hendikep"

        key == "handicap" ->
            "Hendikep"

        key == "total corners" ||
            key == "corners over/under" ->
            "Szögletek száma"

        key == "corner handicap" ->
            "Szöglet hendikep"

        key == "total cards" ||
            key == "cards over/under" ->
            "Lapok száma"

        key == "card handicap" ->
            "Lap hendikep"

        key == "total shots" ||
            key == "shots over/under" ->
            "Kapura lövések száma"

        key == "shots on target" ||
            key == "shots on target over/under" ->
            "Kaput eltaláló lövések"

        key == "total offsides" ||
            key == "offsides over/under" ->
            "Lesek száma"

        key == "first goal" ->
            "Első gólt szerző csapat"

        key == "last goal" ->
            "Utolsó gólt szerző csapat"

        key == "to score" ->
            "Gólt szerez"

        key == "win to nil" ->
            "Győzelem kapott gól nélkül"

        key == "clean sheet" ->
            "Kapott gól nélkül"

        key == "both teams to score & win" ->
            "Mindkét csapat gólt szerez és győzelem"

        key == "half time / full time" ->
            "Félidő / végeredmény"

        key == "half time result" ->
            "Félidő eredménye"

        key == "match goals" ->
            "Mérkőzés góljai"

        key == "winning margin" ->
            "Győzelmi különbség"

        key == "correct score first half" ->
            "1. félidő pontos eredménye"

        key == "correct score second half" ->
            "2. félidő pontos eredménye"

        key == "first half total goals" ->
            "1. félidő góljai"

        key == "second half total goals" ->
            "2. félidő góljai"

        key == "first half total corners" ->
            "1. félidő szögletei"

        key == "second half total corners" ->
            "2. félidő szögletei"

        key == "first half both teams to score" ->
            "1. félidő – mindkét csapat gólt szerez"

        key == "second half both teams to score" ->
            "2. félidő – mindkét csapat gólt szerez"

        key == "team total goals" ->
            "Csapat góljai"

        key == "home team total" ->
            "Hazai csapat góljai"

        key == "away team total" ->
            "Vendég csapat góljai"

        key == "home team total goals" ->
            "Hazai csapat góljai"

        key == "away team total goals" ->
            "Vendég csapat góljai"

        key == "player to score" ->
            "Játékos gólt szerez"

        key == "player assists" ->
            "Játékos gólpassza"

        key == "player shots" ->
            "Játékos lövései"

        key == "player shots on target" ->
            "Játékos kaput eltaláló lövései"

        key == "player cards" ->
            "Játékos lapjai"

        key == "player fouls" ->
            "Játékos szabálytalanságai"

        key == "player offsides" ->
            "Játékos leshelyzetei"

        key == "match corners" ->
            "Mérkőzés szögletei"

        key == "match cards" ->
            "Mérkőzés lapjai"

        key == "match offsides" ->
            "Mérkőzés leshelyzetei"

        key == "odd/even" ->
            "Páros / páratlan gólszám"

        key == "odd even" ->
            "Páros / páratlan gólszám"

        key == "winning team" ->
            "Győztes csapat"

        key == "to qualify" ->
            "Továbbjutás"

        key == "qualified" ->
            "Továbbjutó"

        key == "relegation" ->
            "Kiesés"

        key == "promotion" ->
            "Feljutás"

        key == "next goal" ->
            "Következő gól"

        key == "race to" ->
            "Ki éri el előbb?"

        key == "goalscorer" ->
            "Gólszerző"

        key == "anytime goalscorer" ->
            "Bármikor gólt szerez"

        key == "first goalscorer" ->
            "Első gólszerző"

        key == "last goalscorer" ->
            "Utolsó gólszerző"

        key == "to score 2+" ->
            "2 vagy több gólt szerez"

        key == "to score 3+" ->
            "3 vagy több gólt szerez"

        else ->
            market
    }
}
