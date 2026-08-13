package com.livefutar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import com.livefutar.app.ui.theme.AccentGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveScreen(
    matches: List<MatchModel>,
    favoriteTeamIds: Set<Long>,
    onToggleTeamFavorite: (Long) -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onMatchClick: (MatchModel) -> Unit
) {
    val liveMatches = matches.filter { it.isLive }
    val grouped = liveMatches.groupBy { it.leagueDisplayName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Élő mérkőzések",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        if (liveMatches.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "(${liveMatches.size})",
                                fontSize = 14.sp,
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    Text(
                        text = if (isRefreshing) "…" else "↻",
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable(enabled = !isRefreshing) { onRefresh() }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
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
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentGreen
                )
            }

            if (liveMatches.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("⚽", fontSize = 48.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Jelenleg nincs élő mérkőzés",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = onRefresh) {
                            Text("Frissítés", color = AccentGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    grouped.forEach { (leagueName, leagueMatches) ->
                        item {
                            LiveLeagueHeader(
                                name = leagueName,
                                logo = leagueMatches.firstOrNull()?.league?.logo,
                                countryLogo = leagueMatches.firstOrNull()?.country?.logo,
                                count = leagueMatches.size
                            )
                        }
                        items(leagueMatches) { match ->
                            MatchCard(
                                match = match,
                                isHomeFavorite = match.homeTeam?.id != null &&
                                    favoriteTeamIds.contains(match.homeTeam.id),
                                isAwayFavorite = match.awayTeam?.id != null &&
                                    favoriteTeamIds.contains(match.awayTeam.id),
                                onToggleHomeFavorite = {
                                    match.homeTeam?.id?.let(onToggleTeamFavorite)
                                },
                                onToggleAwayFavorite = {
                                    match.awayTeam?.id?.let(onToggleTeamFavorite)
                                },
                                onClick = { onMatchClick(match) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveLeagueHeader(
    name: String,
    logo: String?,
    countryLogo: String?,
    count: Int
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
        if (!logo.isNullOrBlank()) {
            AsyncImage(
                model = logo,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AccentGreen
        )
    }
}
