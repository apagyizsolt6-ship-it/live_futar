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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
 * LIVE FUTÁR - OddsSection
 *
 * Verzió:
 *  - összevont odds piacok
 *  - nyitható / összecsukható piacok
 *  - 1X2 rendezett sorrend
 *  - Gólok száma egy közös blokkban
 *  - Szögletek egy közös blokkban
 *  - Pontos eredmény egy közös blokkban
 *  - BTTS
 *  - hendikep piacok
 *  - félidős piacok
 *  - több fogadóiroda támogatása
 *  - legjobb odds kiemelése
 *  - odds kattintás -> szelvény
 *
 * Az API-hívás csak akkor történik meg,
 * amikor az Odds fül ténylegesen megjelenik.
 */

private const val MAX_VISIBLE_VALUES = 6

private data class OddsMarketGroup(
    val key: String,
    val title: String,
    val values: List<OddsDisplayValue>
)

private data class OddsDisplayValue(
    val value: OddValue,
    val bookmakerName: String
)

private enum class MarketCategory {
    RESULT,
    DOUBLE_CHANCE,
    BTTS,
    TOTAL_GOALS,
    TEAM_TOTAL,
    CORRECT_SCORE,
    TOTAL_CORNERS,
    CORNERS_HANDICAP,
    HANDICAP,
    FIRST_HALF_RESULT,
    SECOND_HALF_RESULT,
    FIRST_HALF_GOALS,
    SECOND_HALF_GOALS,
    FIRST_TEAM_TO_SCORE,
    LAST_TEAM_TO_SCORE,
    OTHER
}

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

@Composable
private fun OddsList(
    odds: List<BookmakerOdd>,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    val groups = buildMarketGroups(
        odds = odds,
        match = match
    )

    val expandedMarkets =
        remember(match.id) {
            mutableStateMapOf<String, Boolean>().apply {

                groups.forEachIndexed { index, group ->

                    put(
                        group.key,
                        index == 0
                    )
                }
            }
        }

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
            items = groups,
            key = {
                it.key
            }
        ) { group ->

            val expanded =
                expandedMarkets[group.key] ?: false

            OddsMarketGroupCard(
                group = group,
                expanded = expanded,
                match = match,
                onToggle = {
                    expandedMarkets[group.key] =
                        !expanded
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

@Composable
private fun OddsMarketGroupCard(
    group: OddsMarketGroup,
    expanded: Boolean,
    match: MatchModel,
    onToggle: () -> Unit,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.surface
        )
    ) {

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onToggle()
                    }
                    .padding(
                        horizontal = 14.dp,
                        vertical = 13.dp
                    ),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = group.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text =
                            "${group.values.size} lehetőség",
                        fontSize = 11.sp,
                        color =
                            MaterialTheme
                                .colorScheme
                                .onSurfaceVariant
                    )
                }

                Icon(
                    imageVector =
                        if (expanded) {
                            Icons.Default.ExpandLess
                        } else {
                            Icons.Default.ExpandMore
                        },
                    contentDescription =
                        if (expanded) {
                            "Összecsukás"
                        } else {
                            "Megnyitás"
                        },
                    tint =
                        MaterialTheme
                            .colorScheme
                            .primary
                )
            }

            if (expanded) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 12.dp,
                            end = 12.dp,
                            bottom = 12.dp
                        )
                ) {

                    val visibleValues =
                        group.values
                            .take(MAX_VISIBLE_VALUES)

                    visibleValues.forEach { displayValue ->

                        OddsValueRow(
                            displayValue = displayValue,
                            marketName = group.title,
                            match = match,
                            onAddToSlip = onAddToSlip
                        )

                        Spacer(
                            modifier =
                                Modifier.height(6.dp)
                        )
                    }

                    if (group.values.size > MAX_VISIBLE_VALUES) {

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "+ ${group.values.size - MAX_VISIBLE_VALUES} további odds",
                            fontSize = 11.sp,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 4.dp,
                                    vertical = 4.dp
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OddsValueRow(
    displayValue: OddsDisplayValue,
    marketName: String,
    match: MatchModel,
    onAddToSlip: (BetSlipSelection) -> Unit
) {

    val value = displayValue.value

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

    val label =
        selectionLabel(
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
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text =
                    displayValue.bookmakerName,
                fontSize = 10.sp,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
                            bookmakerName =
                                displayValue.bookmakerName
                        )
                    )
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
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme
                        .colorScheme
                        .primary
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* MARKET GROUPING                                                           */
/* -------------------------------------------------------------------------- */

private fun buildMarketGroups(
    odds: List<BookmakerOdd>,
    match: MatchModel
): List<OddsMarketGroup> {

    val rawValues =
        odds.flatMap { bookmaker ->

            val bookmakerName =
                bookmaker.bookmakerName
                    ?.trim()
                    .takeUnless {
                        it.isNullOrBlank()
                    }
                    ?: "Fogadóiroda"

            bookmaker.values
                .orEmpty()
                .mapNotNull { value ->

                    val odd = value.odd

                    if (
                        value.value.isNullOrBlank() ||
                        odd == null ||
                        !odd.isFinite() ||
                        odd <= 0.0
                    ) {
                        null
                    } else {

                        OddsRawValue(
                            market =
                                bookmaker.market
                                    ?.trim()
                                    .orEmpty(),
                            value = value,
                            bookmakerName =
                                bookmakerName
                        )
                    }
                }
        }

    val grouped =
        rawValues.groupBy {
            marketKey(it.market)
        }

    val orderedKeys =
        listOf(
            MarketCategory.RESULT,
            MarketCategory.DOUBLE_CHANCE,
            MarketCategory.BTTS,
            MarketCategory.TOTAL_GOALS,
            MarketCategory.TEAM_TOTAL,
            MarketCategory.CORRECT_SCORE,
            MarketCategory.TOTAL_CORNERS,
            MarketCategory.CORNERS_HANDICAP,
            MarketCategory.HANDICAP,
            MarketCategory.FIRST_HALF_RESULT,
            MarketCategory.SECOND_HALF_RESULT,
            MarketCategory.FIRST_HALF_GOALS,
            MarketCategory.SECOND_HALF_GOALS,
            MarketCategory.FIRST_TEAM_TO_SCORE,
            MarketCategory.LAST_TEAM_TO_SCORE,
            MarketCategory.OTHER
        )

    return orderedKeys
        .flatMap { category ->

            val entries =
                grouped[category]
                    .orEmpty()

            if (entries.isEmpty()) {
                emptyList()
            } else {

                val displayValues =
                    prepareDisplayValues(
                        category = category,
                        values = entries,
                        match = match
                    )

                listOf(
                    OddsMarketGroup(
                        key =
                            category.name,
                        title =
                            marketCategoryTitle(
                                category
                            ),
                        values =
                            displayValues
                    )
                )
            }
        }
}

private data class OddsRawValue(
    val market: String,
    val value: OddValue,
    val bookmakerName: String
)

private fun prepareDisplayValues(
    category: MarketCategory,
    values: List<OddsRawValue>,
    match: MatchModel
): List<OddsDisplayValue> {

    val converted =
        values.map {
            OddsDisplayValue(
                value = it.value,
                bookmakerName = it.bookmakerName
            )
        }

    return when (category) {

        MarketCategory.RESULT -> {

            converted.sortedBy {
                resultOrder(
                    selection =
                        it.value.value
                            ?.trim()
                            .orEmpty(),
                    match = match
                )
            }
        }

        MarketCategory.TOTAL_GOALS,
        MarketCategory.FIRST_HALF_GOALS,
        MarketCategory.SECOND_HALF_GOALS -> {

            converted.sortedWith(
                compareBy(
                    {
                        extractNumber(
                            it.value.value
                                ?.trim()
                                .orEmpty()
                        )
                    },
                    {
                        overUnderOrder(
                            it.value.value
                                ?.trim()
                                .orEmpty()
                        )
                    }
                )
            )
        }

        MarketCategory.TOTAL_CORNERS,
        MarketCategory.CORNERS_HANDICAP -> {

            converted.sortedWith(
                compareBy(
                    {
                        extractNumber(
                            it.value.value
                                ?.trim()
                                .orEmpty()
                        )
                    },
                    {
                        overUnderOrder(
                            it.value.value
                                ?.trim()
                                .orEmpty()
                        )
                    }
                )
            )
        }

        MarketCategory.CORRECT_SCORE -> {

            converted.sortedWith(
                compareBy {
                    correctScoreOrder(
                        it.value.value
                            ?.trim()
                            .orEmpty()
                    )
                }
            )
        }

        else -> {

            converted.sortedByDescending {
                it.value.odd ?: 0.0
            }
        }
    }
}

/* -------------------------------------------------------------------------- */
/* MARKET DETECTION                                                           */
/* -------------------------------------------------------------------------- */

private fun marketKey(
    market: String
): MarketCategory {

    val normalized =
        market
            .trim()
            .lowercase(Locale.ROOT)

    return when {

        normalized.contains("correct score") ||
            normalized.contains("correctscore") ||
            normalized.contains("pontos eredmény") ->
            MarketCategory.CORRECT_SCORE

        normalized.contains("total corners") ||
            normalized.contains("corners over") ||
            normalized.contains("corner over") ||
            normalized.contains("corner") &&
            normalized.contains("over/under") ->
            MarketCategory.TOTAL_CORNERS

        normalized.contains("corner handicap") ||
            normalized.contains("corners handicap") ->
            MarketCategory.CORNERS_HANDICAP

        normalized.contains("first team to score") ->
            MarketCategory.FIRST_TEAM_TO_SCORE

        normalized.contains("last team to score") ->
            MarketCategory.LAST_TEAM_TO_SCORE

        normalized.contains("first half result") ->
            MarketCategory.FIRST_HALF_RESULT

        normalized.contains("second half result") ->
            MarketCategory.SECOND_HALF_RESULT

        normalized.contains("first half") &&
            (
                normalized.contains("goal") ||
                    normalized.contains("total")
                ) ->
            MarketCategory.FIRST_HALF_GOALS

        normalized.contains("second half") &&
            (
                normalized.contains("goal") ||
                    normalized.contains("total")
                ) ->
            MarketCategory.SECOND_HALF_GOALS

        normalized.contains("team total") ||
            normalized.contains("team goals") ->
            MarketCategory.TEAM_TOTAL

        normalized.contains("both teams to score") ||
            normalized.contains("btts") ->
            MarketCategory.BTTS

        normalized.contains("double chance") ->
            MarketCategory.DOUBLE_CHANCE

        normalized.contains("total goals") ||
            normalized.contains("goals over/under") ||
            normalized == "over/under" ||
            normalized.contains("goal over") ||
            normalized.contains("goal under") ->
            MarketCategory.TOTAL_GOALS

        normalized.contains("handicap") ||
            normalized.contains("asian handicap") ->
            MarketCategory.HANDICAP

        normalized.contains("full time result") ||
            normalized.contains("match result") ||
            normalized == "1x2" ||
            normalized == "result" ->
            MarketCategory.RESULT

        else ->
            MarketCategory.OTHER
    }
}

private fun marketCategoryTitle(
    category: MarketCategory
): String {

    return when (category) {

        MarketCategory.RESULT ->
            "Végeredmény (1X2)"

        MarketCategory.DOUBLE_CHANCE ->
            "Kettős esély"

        MarketCategory.BTTS ->
            "Mindkét csapat szerez gólt"

        MarketCategory.TOTAL_GOALS ->
            "Gólok száma"

        MarketCategory.TEAM_TOTAL ->
            "Csapat góljai"

        MarketCategory.CORRECT_SCORE ->
            "Pontos eredmény"

        MarketCategory.TOTAL_CORNERS ->
            "Szögletek száma"

        MarketCategory.CORNERS_HANDICAP ->
            "Szöglet hendikep"

        MarketCategory.HANDICAP ->
            "Hendikep"

        MarketCategory.FIRST_HALF_RESULT ->
            "1. félidő eredménye"

        MarketCategory.SECOND_HALF_RESULT ->
            "2. félidő eredménye"

        MarketCategory.FIRST_HALF_GOALS ->
            "1. félidő góljai"

        MarketCategory.SECOND_HALF_GOALS ->
            "2. félidő góljai"

        MarketCategory.FIRST_TEAM_TO_SCORE ->
            "Melyik csapat szerez először gólt?"

        MarketCategory.LAST_TEAM_TO_SCORE ->
            "Melyik csapat szerez utoljára gólt?"

        MarketCategory.OTHER ->
            "Egyéb piacok"
    }
}

/* -------------------------------------------------------------------------- */
/* SELECTION TRANSLATION                                                      */
/* -------------------------------------------------------------------------- */

private fun selectionLabel(
    selection: String,
    match: MatchModel
): String {

    val normalized =
        selection
            .trim()
            .lowercase(Locale.ROOT)

    return when {

        normalized == "home" ||
            normalized == "1" ->
            match.homeTeam?.name ?: "Hazai"

        normalized == "away" ||
            normalized == "2" ->
            match.awayTeam?.name ?: "Vendég"

        normalized == "draw" ||
            normalized == "x" ->
            "Döntetlen"

        normalized == "yes" ->
            "Igen"

        normalized == "no" ->
            "Nem"

        normalized.startsWith("over") -> {

            val number =
                selection
                    .replace(
                        Regex(
                            "(?i)over\\s*"
                        ),
                        ""
                    )
                    .trim()

            "Több mint $number"
        }

        normalized.startsWith("under") -> {

            val number =
                selection
                    .replace(
                        Regex(
                            "(?i)under\\s*"
                        ),
                        ""
                    )
                    .trim()

            "Kevesebb mint $number"
        }

        normalized.startsWith("o ") -> {

            val number =
                selection
                    .drop(2)
                    .trim()

            "Több mint $number"
        }

        normalized.startsWith("u ") -> {

            val number =
                selection
                    .drop(2)
                    .trim()

            "Kevesebb mint $number"
        }

        else ->
            selection
    }
}

/* -------------------------------------------------------------------------- */
/* SORTING                                                                    */
/* -------------------------------------------------------------------------- */

private fun resultOrder(
    selection: String,
    match: MatchModel
): Int {

    return when (
        selection
            .trim()
            .lowercase(Locale.ROOT)
    ) {

        "home",
        "1" -> 0

        "draw",
        "x" -> 1

        "away",
        "2" -> 2

        else -> 3
    }
}

private fun overUnderOrder(
    selection: String
): Int {

    val normalized =
        selection
            .trim()
            .lowercase(Locale.ROOT)

    return when {

        normalized.startsWith("over") ||
            normalized.startsWith("o ") ->
            0

        normalized.startsWith("under") ||
            normalized.startsWith("u ") ->
            1

        else ->
            2
    }
}

private fun extractNumber(
    value: String
): Double {

    val match =
        Regex(
            """(\d+(?:[.,]\d+)?)"""
        ).find(value)

    return match
        ?.groupValues
        ?.getOrNull(1)
        ?.replace(',', '.')
        ?.toDoubleOrNull()
        ?: Double.MAX_VALUE
}

private fun correctScoreOrder(
    value: String
): Double {

    val match =
        Regex(
            """(\d+)\s*[:\-]\s*(\d+)"""
        ).find(value)

    if (match == null) {
        return Double.MAX_VALUE
    }

    val home =
        match.groupValues
            .getOrNull(1)
            ?.toDoubleOrNull()
            ?: return Double.MAX_VALUE

    val away =
        match.groupValues
            .getOrNull(2)
            ?.toDoubleOrNull()
            ?: return Double.MAX_VALUE

    return home * 100.0 + away
}
