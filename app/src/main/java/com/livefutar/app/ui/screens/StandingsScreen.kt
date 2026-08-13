package com.livefutar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.livefutar.app.data.ApiKeyManager
import com.livefutar.app.data.FootballApiService
import com.livefutar.app.model.LeagueModel
import com.livefutar.app.model.StandingRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StandingsScreen(
    league: LeagueModel,
    onBackClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val apiService = remember { FootballApiService.create() }

    var rows by remember(league.id) { mutableStateOf<List<StandingRow>>(emptyList()) }
    var isLoading by remember(league.id) { mutableStateOf(true) }
    var errorMessage by remember(league.id) { mutableStateOf<String?>(null) }

    LaunchedEffect(league.id) {
        isLoading = true
        errorMessage = null
        val apiKey = ApiKeyManager.getApiKey(context)
        val season = league.season
        if (apiKey.isBlank() || season == null) {
            errorMessage = "A tabella ehhez a bajnoksághoz jelenleg nem elérhető."
            isLoading = false
            return@LaunchedEffect
        }
        try {
            val response = apiService.getStandings(apiKey, league.id, season)
            rows = response.groups?.firstOrNull()?.standings ?: emptyList()
        } catch (e: Exception) {
            errorMessage = "Nem sikerült betölteni a tabellát."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(league.name ?: "Tabella", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Text("←", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = errorMessage ?: "", color = Color.Gray)
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    StandingsHeaderRow()
                    LazyColumn {
                        items(rows) { row -> StandingRowView(row) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StandingsHeaderRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "#", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(24.dp))
        Text(text = "Csapat", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(text = "M", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(28.dp))
        Text(text = "GK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(36.dp))
        Text(text = "P", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray, modifier = Modifier.width(28.dp))
    }
}

@Composable
private fun StandingRowView(row: StandingRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = row.position?.toString() ?: "-",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(24.dp)
        )
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!row.team?.logo.isNullOrBlank()) {
                AsyncImage(
                    model = row.team?.logo,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).clip(CircleShape),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = row.team?.name ?: "-",
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        Text(
            text = row.total?.games?.toString() ?: "-",
            fontSize = 13.sp,
            modifier = Modifier.width(28.dp)
        )
        Text(
            text = if (row.total != null) {
                val diff = row.total.goalDifference
                (if (diff > 0) "+$diff" else "$diff")
            } else "-",
            fontSize = 13.sp,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = row.points?.toString() ?: "-",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(28.dp)
        )
    }
}
