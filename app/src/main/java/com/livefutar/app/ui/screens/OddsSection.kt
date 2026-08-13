package com.livefutar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import com.livefutar.app.ui.theme.AccentGreen
import java.util.Locale

/**
 * Odds fül:
 *
 * PIAC
 *   -> KIMENET
 *      -> LEGJOBB ODDS
 *         -> ÖSSZES ODDS
 *
 * Minden piac külön kártya.
 *
 * A bookmaker-ek nem ömlesztve jelennek meg.
 * Először minden kimenetből csak a legjobb odds látható.
 *
 * A kimenet jobb oldalán lévő + gombbal az összes bookmaker
 * oddsai megjeleníthetők.
 *
 * Az odds kiválasztása:
 * 1. meghívja a meglévő onAddToSlip callbacket
 * 2. bekerül a helyi virtuális Tippmix-szerű szelvénybe
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

    var loading by remember(match.id) {
        mutableStateOf(true)
    }

    var error by remember(match.id) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(match.id, match.isLive) {
        loading = true
        error = null
        odds = emptyList()

        val key = ApiKeyManager.getApiKey(context)

        if (key.isBlank()) {
            error = "Nincs beállítva API-kulcs."
            loading = false
            return@LaunchedEffect
        }

        try {
            val response = apiService.getOdds(
                apiKey = key,
                matchId = match.id,
                oddsType = if (match.isLive) {
                    "live"
                } else {
                    "prematch"
                },
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

                    val values = bookmaker.values
                        .orEmpty()
                        .filter {
                            val odd = it.odd

                            !it.value.isNullOrBlank() &&
                                odd != null &&
                                odd.isFinite() &&
                                odd > 0.0
                        }

                    if (values.isEmpty()) {
                        null
                    } else {
                        bookmaker.copy(
                            values = values
                        )
                    }
                }

            if (odds.isEmpty()) {
                error =
                    "Ehhez a mérkőzéshez jelenleg nincs elérhető odds."
            }
        } catch (_: Exception) {
            error = "Az oddsok most nem tölthetők be."
        } finally {
            loading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
    ) {
        when {
            loading -> LoadingOdds()

            odds.isEmpty() -> EmptyOdds(
                message = error
            )

            else -> OddsList(
                odds = odds,
                match = match,
                onAddToSlip = onAddToSlip
            )
        }
    }
}

@Composable
private fun LoadingOdds() {
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

@Composable
private fun EmptyOdds(
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
                MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private data class RawOdd(
    val market: String,
    val bookmaker: String,
    val selection: String,
    val odd: Double
)

private data class Outcome(
    val selection: String,
    val label: String,
    val bestOdd: Double,
    val bestBookmaker: String,
    val all: List<RawOdd>
)

private data class MarketGroup(
    val key: String,
    val raw: String,
    val title: String,
    val outcomes: List<Outcome>
)

@Composable
private fun OddsList(
    odds: List<BookmakerOdd>,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {
    val markets = remember(
        odds,
        match.id
    ) {
        buildMarkets(
            odds = odds,
            match = match
        )
    }

    /*
     * null = minden piac alapból nyitva.
     */
    var openMarkets by remember(match.id) {
        mutableStateOf<Set<String>?>(null)
    }

    var openOutcomes by remember(match.id) {
        mutableStateOf<Set<String>>(emptySet())
    }

    /*
     * Helyi virtuális szelvény.
     */
    var slip by remember(match.id) {
        mutableStateOf<List<BetSlipSelection>>(emptyList())
    }

    var slipOpen by remember(match.id) {
        mutableStateOf(false)
    }

    fun add(selection: BetSlipSelection) {

        /*
         * A meglévő projekt szelvénykezelője is megkapja.
         */
        onAddToSlip(selection)

        /*
         * Ugyanazon meccs + ugyanazon piac esetén
         * az új tipp lecseréli az előzőt.
         */
        slip = slip
            .filterNot {
                it.matchId == selection.matchId &&
                    normalize(it.market) ==
                    normalize(selection.market)
            }
            .plus(selection)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement =
                Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(
                top = 8.dp,
                bottom =
                    if (slip.isEmpty()) {
                        20.dp
                    } else {
                        150.dp
                    }
            )
        ) {

            item {

                Text(
                    text =
                        "Minden elérhető piac • a legjobb odds elöl",
                    fontSize = 12.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(
                    modifier = Modifier.height(4.dp)
                )

                Text(
                    text =
                        "Koppints egy oddsszámra a virtuális szelvényhez adáshoz.",
                    fontSize = 12.sp,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(
                items = markets,
                key = {
                    it.key
                }
            ) { market ->

                val marketOpen =
                    openMarkets
                        ?.contains(market.key)
                        ?: true

                OddsMarketCard(
                    market = market,
                    open = marketOpen,
                    openOutcomes = openOutcomes,
                    selected = slip,
                    match = match,

                    onToggleMarket = {

                        val current =
                            openMarkets
                                ?: markets
                                    .map { it.key }
                                    .toSet()

                        openMarkets =
                            if (marketOpen) {
                                current - market.key
                            } else {
                                current + market.key
                            }
                    },

                    onToggleOutcome = { key ->

                        openOutcomes =
                            if (key in openOutcomes) {
                                openOutcomes - key
                            } else {
                                openOutcomes + key
                            }
                    },

                    onAdd = ::add
                )
            }
        }

        if (slip.isNotEmpty()) {

            VirtualSlip(
                selections = slip,
                open = slipOpen,

                onToggle = {
                    slipOpen = !slipOpen
                },

                onRemove = { item ->
                    slip =
                        slip.filterNot {
                            it == item
                        }
                },

                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        }
    }
}

@Composable
private fun OddsMarketCard(
    market: MarketGroup,
    open: Boolean,
    openOutcomes: Set<String>,
    selected: List<BetSlipSelection>,
    match: MatchModel,
    onToggleMarket: () -> Unit,
    onToggleOutcome: (String) -> Unit,
    onAdd: (BetSlipSelection) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            /*
             * Piac fejléc.
             */
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onToggleMarket
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = market.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow =
                            TextOverflow.Ellipsis
                    )

                    Text(
                        text =
                            "${market.outcomes.sumOf { it.all.size }} " +
                                "elérhető odds • " +
                                "${market.outcomes.size} kimenetel",
                        fontSize = 12.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text =
                        if (open) {
                            "▲"
                        } else {
                            "▼"
                        },
                    color =
                        MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (open) {

                Spacer(
                    modifier = Modifier.height(10.dp)
                )

                market.outcomes.forEach { outcome ->

                    val outcomeKey =
                        "${market.key}|${normalize(outcome.selection)}"

                    val outcomeOpen =
                        outcomeKey in openOutcomes

                    val isSelected =
                        selected.any {
                            it.matchId == match.id &&
                                normalize(it.market) ==
                                normalize(market.raw) &&
                                normalize(it.selection) ==
                                normalize(outcome.selection)
                        }

                    BestOutcome(
                        outcome = outcome,
                        selected = isSelected,
                        open = outcomeOpen,

                        onSelect = {

                            onAdd(
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
                                    market = market.raw,
                                    selection =
                                        outcome.selection,
                                    odd = outcome.bestOdd,
                                    bookmakerName =
                                        outcome.bestBookmaker
                                )
                            )
                        },

                        onToggle = {
                            onToggleOutcome(
                                outcomeKey
                            )
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    if (outcomeOpen) {

                        AllOdds(
                            rows = outcome.all,
                            best = outcome.bestOdd,
                            match = match,
                            market = market.raw,
                            onAdd = onAdd
                        )

                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )
                    }
                }

                Text(
                    text = "▲ Piac összecsukása",
                    modifier = Modifier
                        .clickable(
                            onClick = onToggleMarket
                        )
                        .padding(vertical = 4.dp),
                    color =
                        MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun BestOutcome(
    outcome: Outcome,
    selected: Boolean,
    open: Boolean,
    onSelect: () -> Unit,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                if (selected) {
                    AccentGreen.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.07f)
                }
            )
            .border(
                width =
                    if (selected) {
                        1.5.dp
                    } else {
                        1.dp
                    },
                color =
                    if (selected) {
                        AccentGreen
                    } else {
                        MaterialTheme.colorScheme.primary
                            .copy(alpha = 0.18f)
                    },
                shape = RoundedCornerShape(12.dp)
            )
            .padding(10.dp),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(
                    onClick = onSelect
                )
        ) {

            Text(
                text = outcome.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text = outcome.bestBookmaker,
                fontSize = 10.sp,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis
            )

            Text(
                text = "LEGJOBB ODDS",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGreen
            )
        }

        /*
         * Legjobb odds.
         */
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(10.dp)
                )
                .background(
                    MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.10f)
                )
                .border(
                    1.5.dp,
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(10.dp)
                )
                .clickable(
                    onClick = onSelect
                )
                .padding(
                    horizontal = 13.dp,
                    vertical = 8.dp
                ),
            contentAlignment = Alignment.Center
        ) {

            Text(
                text =
                    String.format(
                        Locale.US,
                        "%.2f",
                        outcome.bestOdd
                    ),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.primary
            )
        }

        Spacer(
            modifier = Modifier.width(6.dp)
        )

        /*
         * Összes bookmaker megjelenítése.
         */
        Text(
            text =
                if (open) {
                    "−"
                } else {
                    "+"
                },

            modifier = Modifier
                .clip(
                    RoundedCornerShape(8.dp)
                )
                .clickable(
                    onClick = onToggle
                )
                .padding(6.dp),

            color =
                MaterialTheme.colorScheme.primary,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AllOdds(
    rows: List<RawOdd>,
    best: Double,
    match: MatchModel,
    market: String,
    onAdd: (BetSlipSelection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(12.dp)
            )
            .background(
                MaterialTheme.colorScheme.background
                    .copy(alpha = 0.55f)
            )
            .padding(10.dp)
    ) {

        Text(
            text = "Összes elérhető odds",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        rows
            .sortedByDescending {
                it.odd
            }
            .forEach { row ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {

                            onAdd(
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
                                    market = market,
                                    selection =
                                        row.selection,
                                    odd = row.odd,
                                    bookmakerName =
                                        row.bookmaker
                                )
                            )
                        }
                        .padding(vertical = 5.dp),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = row.bookmaker,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        if (row.odd == best) {

                            Text(
                                text = "LEGJOBB",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentGreen
                            )
                        }
                    }

                    Text(
                        text =
                            String.format(
                                Locale.US,
                                "%.2f",
                                row.odd
                            ),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (row.odd == best) {
                                AccentGreen
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                    )
                }
            }
    }
}

@Composable
private fun VirtualSlip(
    selections: List<BetSlipSelection>,
    open: Boolean,
    onToggle: () -> Unit,
    onRemove: (BetSlipSelection) -> Unit,
    modifier: Modifier
) {
    /*
     * Eredő odds.
     */
    val totalOdd =
        selections.fold(1.0) { total, item ->
            total * item.odd
        }

    /*
     * Első verzióban 1000 Ft alap tét.
     * Később ebből csinálunk szerkeszthető tétmezőt.
     */
    val stake = 1000.0

    val possibleWin =
        stake * totalOdd

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClick = onToggle
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "🎫 Virtuális szelvény (${selections.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text =
                            "Eredő odds: " +
                                String.format(
                                    Locale.US,
                                    "%.2f",
                                    totalOdd
                                ) +
                                " • Tét: 1 000 Ft",
                        fontSize = 11.sp,
                        color =
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text =
                        if (open) {
                            "▲"
                        } else {
                            "▼"
                        },
                    color =
                        MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (open) {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                selections.forEach { item ->

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = item.matchLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow =
                                    TextOverflow.Ellipsis
                            )

                            Text(
                                text =
                                    "${huMarket(item.market)} • " +
                                        item.label,
                                fontSize = 10.sp,
                                color =
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow =
                                    TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text =
                                String.format(
                                    Locale.US,
                                    "%.2f",
                                    item.odd
                                ),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color =
                                MaterialTheme.colorScheme.primary
                        )

                        Spacer(
                            modifier = Modifier.width(6.dp)
                        )

                        Text(
                            text = "×",
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable {
                                    onRemove(item)
                                }
                                .padding(5.dp),
                            color =
                                MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ) {

                    Column {

                        Text(
                            text = "Eredő odds",
                            fontSize = 10.sp,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text =
                                String.format(
                                    Locale.US,
                                    "%.2f",
                                    totalOdd
                                ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }

                    Column(
                        horizontalAlignment =
                            Alignment.End
                    ) {

                        Text(
                            text =
                                "Lehetséges nyeremény",
                            fontSize = 10.sp,
                            color =
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            text =
                                "${formatFt(possibleWin)} Ft",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGreen
                        )
                    }
                }

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Button(
                    onClick = onToggle,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Szelvény bezárása"
                    )
                }
            }
        }
    }
}

private fun buildMarkets(
    odds: List<BookmakerOdd>,
    match: MatchModel
): List<MarketGroup> {

    val rows = buildList {

        odds.forEach { bookmaker ->

            val market =
                bookmaker.market
                    ?.trim()
                    .takeUnless {
                        it.isNullOrBlank()
                    }
                    ?: "Egyéb"

            val name =
                bookmaker.bookmakerName
                    ?.trim()
                    .takeUnless {
                        it.isNullOrBlank()
                    }
                    ?: "Bookmaker"

            bookmaker.values
                .orEmpty()
                .forEach { value ->

                    val originalSelection =
                        value.value
                            ?.trim()
                            .orEmpty()

                    val odd = value.odd

                    if (
                        originalSelection.isBlank() ||
                        odd == null ||
                        !odd.isFinite() ||
                        odd <= 0.0
                    ) {
                        return@forEach
                    }

                    /*
                     * A Correct Score API-k esetén előfordul,
                     * hogy a score a market mezőben van:
                     *
                     * Correct Score 1 : 0
                     *
                     * Ezt egyetlen közös Pontos eredmény piacba
                     * vonjuk össze.
                     */
                    val correctScore =
                        correctScoreFromMarket(market)

                    val selection =
                        correctScore
                            ?: originalSelection

                    val canonicalMarket =
                        if (correctScore != null) {
                            "Correct Score"
                        } else {
                            market
                        }

                    add(
                        RawOdd(
                            market = canonicalMarket,
                            bookmaker = name,
                            selection = selection,
                            odd = odd
                        )
                    )
                }
        }
    }

    return rows
        .groupBy {
            normalize(it.market)
        }
        .map { (key, marketRows) ->

            val raw =
                marketRows.first().market

            val outcomes =
                marketRows
                    .groupBy {
                        normalize(it.selection)
                    }
                    .map { (_, list) ->

                        val best =
                            list.maxByOrNull {
                                it.odd
                            } ?: list.first()

                        Outcome(
                            selection =
                                best.selection,
                            label =
                                selectionLabel(
                                    best.selection,
                                    match
                                ),
                            bestOdd =
                                best.odd,
                            bestBookmaker =
                                best.bookmaker,
                            all =
                                list.sortedByDescending {
                                    it.odd
                                }
                        )
                    }
                    .sortedWith(
                        outcomeComparator(raw)
                    )

            MarketGroup(
                key = key,
                raw = raw,
                title = huMarket(raw),
                outcomes = outcomes
            )
        }
        .sortedWith(
            compareBy<MarketGroup> {
                marketRank(it.raw)
            }.thenBy {
                it.title.lowercase(
                    Locale.ROOT
                )
            }
        )
}

private fun correctScoreFromMarket(
    market: String
): String? {

    val prefix = "correct score"

    val lower =
        market
            .trim()
            .lowercase(Locale.ROOT)

    if (!lower.startsWith(prefix)) {
        return null
    }

    val suffix =
        market
            .trim()
            .substring(prefix.length)
            .trim()

    if (suffix.isBlank()) {
        return null
    }

    return Regex(
        "^(\\d+)\\s*[:\\-]\\s*(\\d+)$"
    )
        .find(suffix)
        ?.let {
            "${it.groupValues[1]}:${it.groupValues[2]}"
        }
}

private fun outcomeComparator(
    market: String
): Comparator<Outcome> =
    Comparator { a, b ->

        val rankA =
            selectionRank(
                market,
                a.selection
            )

        val rankB =
            selectionRank(
                market,
                b.selection
            )

        when {

            rankA != rankB ->
                rankA.compareTo(rankB)

            parseScore(a.selection) != null &&
                parseScore(b.selection) != null -> {

                val scoreA =
                    parseScore(a.selection)!!

                val scoreB =
                    parseScore(b.selection)!!

                val total =
                    (scoreA.first + scoreA.second)
                        .compareTo(
                            scoreB.first + scoreB.second
                        )

                if (total != 0) {
                    total
                } else {

                    val home =
                        scoreA.first.compareTo(
                            scoreB.first
                        )

                    if (home != 0) {
                        home
                    } else {
                        scoreA.second.compareTo(
                            scoreB.second
                        )
                    }
                }
            }

            else ->
                a.label
                    .lowercase(Locale.ROOT)
                    .compareTo(
                        b.label.lowercase(
                            Locale.ROOT
                        )
                    )
        }
    }

private fun selectionRank(
    market: String,
    selection: String
): Int {

    val s =
        selection
            .lowercase(Locale.ROOT)
            .trim()

    return when {

        s == "home" || s == "1" ->
            10

        s == "draw" || s == "x" ->
            20

        s == "away" || s == "2" ->
            30

        s == "yes" || s == "igen" ->
            40

        s == "no" || s == "nem" ->
            50

        s.contains("over") ||
            s.contains("több") ->
            60

        s.contains("under") ||
            s.contains("kevesebb") ->
            70

        s.contains("none") ||
            s.contains("nincs") ->
            80

        else ->
            100
    }
}

private fun marketRank(
    market: String
): Int {

    val s =
        market.lowercase(Locale.ROOT)

    return when {

        s.contains("full time result") ||
            s.contains("match result") ||
            s == "1x2" ->
            10

        s.contains("double chance") ->
            20

        s.contains("draw no bet") ->
            30

        s.contains("both teams to score") ||
            s.contains("btts") ->
            40

        s.contains("total goals") ||
            s.contains("over/under") ->
            50

        s.contains("asian handicap") ->
            60

        s.contains("first half result") ->
            70

        s.contains("second half result") ->
            80

        s.contains("team total") ->
            90

        s.contains("first team to score") ->
            100

        s.contains("correct score") ->
            200

        else ->
            500
    }
}

private fun selectionLabel(
    selection: String,
    match: MatchModel
): String {

    val s =
        selection.trim()

    return when {

        s.equals(
            "home",
            true
        ) || s == "1" ->
            "Hazai"

        s.equals(
            "draw",
            true
        ) || s.equals(
            "x",
            true
        ) ->
            "Döntetlen"

        s.equals(
            "away",
            true
        ) || s == "2" ->
            "Vendég"

        s.equals(
            "yes",
            true
        ) ->
            "Igen"

        s.equals(
            "no",
            true
        ) ->
            "Nem"

        s.equals(
            "over",
            true
        ) ->
            "Több"

        s.equals(
            "under",
            true
        ) ->
            "Kevesebb"

        s.equals(
            "none",
            true
        ) ->
            "Nincs"

        s.startsWith(
            "Over ",
            true
        ) ->
            "Több ${
                s.substringAfter(" ")
                    .replace('.', ',')
            }"

        s.startsWith(
            "Under ",
            true
        ) ->
            "Kevesebb ${
                s.substringAfter(" ")
                    .replace('.', ',')
            }"

        else ->
            s
    }
}

private fun normalize(
    value: String
): String =
    value
        .trim()
        .lowercase(Locale.ROOT)
        .replace(
            Regex("\\s+"),
            " "
        )

private fun parseScore(
    value: String
): Pair<Int, Int>? {

    val match =
        Regex(
            "^(\\d+)\\s*[:\\-]\\s*(\\d+)$"
        ).find(
            value.trim()
        ) ?: return null

    val home =
        match.groupValues[1]
            .toIntOrNull()
            ?: return null

    val away =
        match.groupValues[2]
            .toIntOrNull()
            ?: return null

    return home to away
}

private fun huMarket(
    market: String
): String {

    val s =
        market
            .trim()
            .lowercase(Locale.ROOT)

    return when {

        s == "full time result" ||
            s == "match result" ||
            s == "1x2" ->
            "Végeredmény (1X2)"

        s == "double chance" ->
            "Kettős esély"

        s == "draw no bet" ->
            "Döntetlennél visszajár"

        s == "both teams to score" ||
            s == "btts" ->
            "Mindkét csapat szerez gólt"

        s.startsWith("total goals") ||
            s == "goals over/under" ||
            s == "over/under" ->
            "Gólok száma${
                suffixAfterIgnoreCase(
                    market,
                    "Total Goals"
                )
            }"

        s.startsWith("total corners") ->
            "Szögletek száma${
                suffixAfterIgnoreCase(
                    market,
                    "Total Corners"
                )
            }"

        s.startsWith("total cards") ->
            "Lapok száma${
                suffixAfterIgnoreCase(
                    market,
                    "Total Cards"
                )
            }"

        s == "first half result" ->
            "1. félidő eredménye"

        s == "second half result" ->
            "2. félidő eredménye"

        s.startsWith("correct score") ->
            "Pontos eredmény${
                suffixAfterIgnoreCase(
                    market,
                    "Correct Score"
                )
            }"

        s.startsWith("asian handicap") ->
            "Ázsiai hendikep${
                suffixAfterIgnoreCase(
                    market,
                    "Asian Handicap"
                )
            }"

        s == "team total" ->
            "Csapat góljai"

        s == "first team to score" ->
            "Első gólt szerző csapat"

        s == "last team to score" ->
            "Utolsó gólt szerző csapat"

        s == "half time/full time" ->
            "Félidő / végeredmény"

        s == "odd/even" ->
            "Páratlan / páros"

        s.startsWith("total shots") ->
            "Kapura lövések száma${
                suffixAfterIgnoreCase(
                    market,
                    "Total Shots"
                )
            }"

        s.startsWith("total fouls") ->
            "Szabálytalanságok száma${
                suffixAfterIgnoreCase(
                    market,
                    "Total Fouls"
                )
            }"

        s.startsWith("total offsides") ->
            "Lesek száma${
                suffixAfterIgnoreCase(
                    market,
                    "Total Offsides"
                )
            }"

        s == "to qualify" ->
            "Továbbjutás"

        s == "clean sheet" ->
            "Kapott gól nélkül"

        s == "win to nil" ->
            "Győzelem kapott gól nélkül"

        else ->
            market
    }
}

private fun suffixAfterIgnoreCase(
    value: String,
    prefix: String
): String {

    val lower =
        value.lowercase(Locale.ROOT)

    val prefixLower =
        prefix.lowercase(Locale.ROOT)

    if (!lower.startsWith(prefixLower)) {
        return ""
    }

    val suffix =
        value
            .substring(prefix.length)
            .trim()

    return if (suffix.isBlank()) {
        ""
    } else {
        " $suffix"
    }
}

private fun formatFt(
    value: Double
): String =
    String.format(
        Locale.US,
        "%,.0f",
        value
    ).replace(
        ',',
        ' '
    )
