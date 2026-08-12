package com.livefutar.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.livefutar.app.data.ApiKeyManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onApiKeySaved: () -> Unit = {}
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(ApiKeyManager.getApiKey(context)) }
    var savedMessage by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Beállítások", fontWeight = FontWeight.Bold) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Highlightly API Kulcs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    savedMessage = false
                },
                label = { Text("API Kulcs megadása") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Button(
                onClick = {
                    ApiKeyManager.saveApiKey(context, apiKey)
                    savedMessage = true
                    onApiKeySaved()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Mentés")
            }

            if (savedMessage) {
                Text(
                    text = "Kulcs sikeresen elmentve! Adatok újratöltése...",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
