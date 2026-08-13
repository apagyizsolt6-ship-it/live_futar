package com.livefutar.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livefutar.app.data.ApiKeyManager
import com.livefutar.app.data.FootballApiService
import com.livefutar.app.model.MatchEventModel
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.theme.AccentGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    match: MatchModel,
    onBackClick: () -> Unit,
    onStandingsClick: (MatchModel) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val apiService = remember { FootballApiService.create() }

    var events by remember(match.id) { mutableStateOf<List<MatchEventModel>>(emptyList()) }
    var h2hMatches by remember(match.id) { mutableStateOf<List<MatchModel>>(emptyList()) }
    var isLoadingExtras by remember(match.id) { mutableStateOf(true) }

    LaunchedEffect(match.id) {
        isLoadingExtras = true
        val apiKey = ApiKeyManager.getApiKey(context)
        if (apiKey.isNotBlank()) {
            try {
                withContext(Dispatchers.IO) {
                    events = try {
                        apiService.getMatchEvents(apiKey, match.id).sortedBy { it.minuteSortKey }
                    } catch (e: Exception) {
                        emptyList()
                    }
                    val homeId = match.homeTeam?.id
                    val awayId = match.awayTeam?.id
                    h2hMatches = if (homeId != null && awayId != null) {
                        try {
                            apiService.getHeadToHead(apiKey, homeId, awayId)
                                .filter { it.id != match.id }
                                .take(5)
                        } catch (e: Exception) {
                            emptyList()
                        }
                    } else {
                        emptyList()
                    }
                }
            } finally {
                isLoadingExtras = false
            }
        } else {
            isLoadingExtras = false
        }
    }

    val titleText = match.leagueDisplayName

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        titleText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                },
                actions = {
                    if (match.league?.id != null && match.league.season != null) {
                        Text(
                            text = "Tabella",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .clickable { onStandingsClick(match) }
                        )
                    }
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
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (match.isLive) {
                            Modifier.border(
                                1.5.dp,
                                AccentGreen.copy(alpha = 0.45f),
                                RoundedCornerShape(20.dp)
                            )
                        } else Modifier
                    ),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (match.isLive) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (match.isLive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AccentGreen)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ÉLŐ",
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                letterSpacing = 1.sp
                            )
                            match.liveMinuteLabel?.let {
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = it,
                                    color = AccentGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TeamColumn(
                            logoUrl = match.homeTeam?.logo,
                            name = match.homeTeam?.name ?: "Hazai",
                            modifier = Modifier.weight(1f)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${match.homeScoreDisplay}  :  ${match.awayScoreDisplay}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (match.isLive) AccentGreen else MaterialTheme.colorScheme.onSurface
                            )
                            if (match.isNotStarted) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = match.kickoffTime,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        TeamColumn(
                            logoUrl = match.awayTeam?.logo,
                            name = match.awayTeam?.name ?: "Vendég",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (match.isLive) AccentGreen.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = match.statusLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (match.isLive) AccentGreen else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isLoadingExtras) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            } else {
                if (events.isNotEmpty()) {
                    SectionTitle(title = "Események")
                    EventsTimeline(events = events, homeTeamId = match.homeTeam?.id)
                }

                if (h2hMatches.isNotEmpty()) {
                    SectionTitle(title = "Korábbi találkozók")
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            h2hMatches.forEach { h2h -> H2HRow(h2h) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun EventsTimeline(events: List<MatchEventModel>, homeTeamId: Long?) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            events.forEach { event ->
                val isHomeEvent = event.team?.id == homeTeamId
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isHomeEvent) Arrangement.Start else Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isHomeEvent) {
                        EventContent(event)
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                        EventContent(event, alignEnd = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventContent(event: MatchEventModel, alignEnd: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (alignEnd) {
            EventTexts(event, alignEnd = true)
            Text(text = event.icon, fontSize = 16.sp)
        } else {
            Text(text = event.icon, fontSize = 16.sp)
            EventTexts(event, alignEnd = false)
        }
    }
}

@Composable
private fun EventTexts(event: MatchEventModel, alignEnd: Boolean) {
    Column(horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start) {
        Text(
            text = "${event.time ?: ""}' ${event.typeLabel}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!event.player.isNullOrBlank()) {
            Text(
                text = event.player,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun H2HRow(match: MatchModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${match.homeTeam?.name ?: "-"} – ${match.awayTeam?.name ?: "-"}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1
        )
        Text(
            text = "${match.homeScoreDisplay} : ${match.awayScoreDisplay}",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TeamColumn(logoUrl: String?, name: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
        Text(
            text = name,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
