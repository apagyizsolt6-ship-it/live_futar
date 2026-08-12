package com.livefutar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.MatchCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    matches: List<MatchModel>,
    onMatchClick: (MatchModel) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LIVE FUTÁR ⚽ ÉLŐ", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            item {
                Text(
                    text = "Maibb mérkőzések",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (matches.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Nincsenek elérhető meccsek", color = Color.Gray)
                    }
                }
            } else {
                items(matches) { match ->
                    MatchCard(match = match, onClick = { onMatchClick(match) })
                }
            }
        }
    }
}
