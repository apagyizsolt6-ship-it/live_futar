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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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
    val filteredByFavorites = if (showOnlyFavorites) {
        matches.filter { match ->
            (match.league?.id != null && favoriteLeagueIds.contains(match.league.id)) ||
                (match.homeTeam?.id != null && favoriteTeamIds.contains(match.homeTeam.id)) ||
                (match.awayTeam?.id != null && favoriteTeamIds.contains(match.awayTeam.id))
        }
    } else {
        matches
    }

    val visibleMatches = if (showOnlyLive) {
        filteredByFavorites.filter { it.isLive }
    } else {
        filteredByFavorites
    }

    val liveMatches = matches.filter { it.isLive }
    val groupedByLeague = visibleMatches.groupBy { it.leagueDisplayName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "LIVE FUTÁR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("⚽", fontSize = 16.sp)
                        if (liveMatches.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            LiveBadge(count = liveMatches.size)
                        }
                    }
                },
                actions = {
                    Text(
                        text = if (isRefreshing) "…" else "↻",
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clickable(enabled = !isRefreshing) { onRefresh() }
                    )
                    FilterChip(
                        selected = showOnlyLive,
                        onClick = onToggleShowOnlyLive,
                        label = {
                            Text(
                                if (showOnlyLive) "ÉLŐ" else "Élő",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentGreen.copy(alpha = 0.25f),
                            selectedLabelColor = AccentGreen,
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(
                        text = if (showOnlyFavorites) "★" else "☆",
                        fontSize = 22.sp,
                        color = if (showOnlyFavorites) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(end = 14.dp)
                            .clickable { onToggleShowOnlyFavorites() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            item {
                DateStrip(selectedDate = selectedDate, onDateSelected = onDateSelected)
            }

            item {
                Text(
                    text = DateUtils.fullDateLabel(selectedDate),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Élő szekció a lista tetején
            if (!showOnlyLive && liveMatches.isNotEmpty() && !showOnlyFavorites) {
                item {
                    LiveSectionHeader(count = liveMatches.size)
                }
                items(liveMatches.take(8)) { match ->
                    MatchCard(
                        match = match,
                        isHomeFavorite = match.homeTeam?.id != null && favoriteTeamIds.contains(match.homeTeam.id),
                        isAwayFavorite = match.awayTeam?.id != null && favoriteTeamIds.contains(match.awayTeam.id),
                        onToggleHomeFavorite = { match.homeTeam?.id?.let(onToggleTeamFavorite) },
                        onToggleAwayFavorite = { match.awayTeam?.id?.let(onToggleTeamFavorite) },
                        onClick = { onMatchClick(match) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Divider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (visibleMatches.isEmpty()) {
                item {
                    EmptyState(
                        showOnlyFavorites = showOnlyFavorites,
                        showOnlyLive = showOnlyLive
                    )
                }
            } else {
                groupedByLeague.forEach { (leagueDisplayName, leagueMatches) ->
                    item {
                        val first = leagueMatches.firstOrNull()
                        val leagueId = first?.league?.id
                        LeagueHeader(
                            leagueDisplayName = leagueDisplayName,
                            leagueLogo = first?.league?.logo,
                            countryLogo = first?.country?.logo,
                            isFavorite = leagueId != null && favoriteLeagueIds.contains(leagueId),
                            onToggleFavorite = { leagueId?.let { onToggleLeagueFavorite(it) } },
                            onStandingsClick = { first?.let(onStandingsClick) }
                        )
                    }
                    items(leagueMatches) { match ->
                        MatchCard(
                            match = match,
                            isHomeFavorite = match.homeTeam?.id != null && favoriteTeamIds.contains(match.homeTeam.id),
                            isAwayFavorite = match.awayTeam?.id != null && favoriteTeamIds.contains(match.awayTeam.id),
                            onToggleHomeFavorite = { match.homeTeam?.id?.let(onToggleTeamFavorite) },
                            onToggleAwayFavorite = { match.awayTeam?.id?.let(onToggleTeamFavorite) },
                            onClick = { onMatchClick(match) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LiveBadge(count: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AccentGreen.copy(alpha = 0.18f))
            .border(1.dp, AccentGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = "ÉLŐ $count",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGreen,
            letterSpacing = 0.4.sp
        )
    }
}

@Composable
private fun LiveSectionHeader(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(AccentGreen)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "ÉLŐ MOST",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGreen,
            letterSpacing = 0.8.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "($count)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(showOnlyFavorites: Boolean, showOnlyLive: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(40.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = when {
                    showOnlyLive -> "⚽"
                    showOnlyFavorites -> "★"
                    else -> "📅"
                },
                fontSize = 36.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when {
                    showOnlyLive -> "Jelenleg nincs élő mérkőzés"
                    showOnlyFavorites -> "Nincs kedvenc mérkőzés ezen a napon"
                    else -> "Nincsenek mérkőzések ezen a napon"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DateStrip(selectedDate: String, onDateSelected: (String) -> Unit) {
    val dates = remember(Unit) { DateUtils.dateStrip() }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        items(dates) { dateStr ->
            val isSelected = dateStr == selectedDate
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surface
                                )
                            )
                        }
                    )
                    .then(
                        if (!isSelected) {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(14.dp)
                            )
                        } else Modifier
                    )
                    .clickable { onDateSelected(dateStr) }
                    .padding(horizontal = 16.dp, vertical = 9.dp)
            ) {
                Text(
                    text = DateUtils.shortChipLabel(dateStr),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LeagueHeader(
    leagueDisplayName: String,
    leagueLogo: String?,
    countryLogo: String?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onStandingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
                    .size(18.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = leagueDisplayName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "Tabella",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onStandingsClick() }
                .padding(end = 12.dp)
        )
        Text(
            text = if (isFavorite) "★" else "☆",
            fontSize = 16.sp,
            color = if (isFavorite) AccentGold else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.clickable { onToggleFavorite() }
        )
    }
}
