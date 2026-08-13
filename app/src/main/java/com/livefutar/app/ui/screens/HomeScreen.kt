package com.livefutar.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.MatchCard
import com.livefutar.app.ui.theme.AccentGold
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
    onMatchClick: (MatchModel) -> Unit,
    onStandingsClick: (MatchModel) -> Unit
) {
    val visibleMatches = if (showOnlyFavorites) {
        matches.filter { match ->
            (match.league?.id != null && favoriteLeagueIds.contains(match.league.id)) ||
                (match.homeTeam?.id != null && favoriteTeamIds.contains(match.homeTeam.id)) ||
                (match.awayTeam?.id != null && favoriteTeamIds.contains(match.awayTeam.id))
        }
    } else {
        matches
    }

    // Csoportosítás bajnokság szerint, a lista sorrendjét megtartva.
    val groupedByLeague = visibleMatches.groupBy { it.league?.name ?: "Egyéb mérkőzések" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LIVE FUTÁR ⚽ ÉLŐ", fontWeight = FontWeight.Bold) },
                actions = {
                    Text(
                        text = if (showOnlyFavorites) "★" else "☆",
                        fontSize = 20.sp,
                        color = if (showOnlyFavorites) AccentGold else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(end = 16.dp)
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
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                DateStrip(selectedDate = selectedDate, onDateSelected = onDateSelected)
            }

            item {
                Text(
                    text = DateUtils.fullDateLabel(selectedDate),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (visibleMatches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showOnlyFavorites) "Nincs kedvenc mérkőzés ezen a napon"
                            else "Nincsenek mérkőzések ezen a napon",
                            color = Color.Gray
                        )
                    }
                }
            } else {
                groupedByLeague.forEach { (leagueName, leagueMatches) ->
                    item {
                        val leagueId = leagueMatches.firstOrNull()?.league?.id
                        LeagueHeader(
                            leagueName = leagueName,
                            leagueLogo = leagueMatches.firstOrNull()?.league?.logo,
                            isFavorite = leagueId != null && favoriteLeagueIds.contains(leagueId),
                            onToggleFavorite = { leagueId?.let { onToggleLeagueFavorite(it) } },
                            onStandingsClick = { leagueMatches.firstOrNull()?.let(onStandingsClick) }
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
        }
    }
}

@Composable
private fun DateStrip(selectedDate: String, onDateSelected: (String) -> Unit) {
    val dates = remember(Unit) { DateUtils.dateStrip() }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        items(dates) { dateStr ->
            val isSelected = dateStr == selectedDate
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .clickable { onDateSelected(dateStr) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
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
    leagueName: String,
    leagueLogo: String?,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onStandingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            text = leagueName,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
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
            fontSize = 15.sp,
            color = if (isFavorite) AccentGold else Color.Gray,
            modifier = Modifier.clickable { onToggleFavorite() }
        )
    }
}
