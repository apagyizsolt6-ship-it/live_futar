package com.livefutar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.ui.components.VideoPlayerCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HighlightsScreen(
    highlights: List<HighlightModel>,
    onHighlightClick: (HighlightModel) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Videós Kiemelések 🎥", fontWeight = FontWeight.Bold) },
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
            items(highlights) { highlight ->
                VideoPlayerCard(highlight = highlight, onClick = { onHighlightClick(highlight) })
            }
        }
    }
}
