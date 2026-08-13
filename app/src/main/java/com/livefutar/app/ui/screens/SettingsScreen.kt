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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.livefutar.app.data.ApiKeyManager
import com.livefutar.app.data.PreferencesManager
import com.livefutar.app.ui.theme.AccentBlue
import com.livefutar.app.ui.theme.AccentGold
import com.livefutar.app.ui.theme.AccentGreen
import com.livefutar.app.ui.theme.AccentOrange
import com.livefutar.app.ui.theme.AccentPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: String,
    accentKey: String,
    onThemeModeChanged: (String) -> Unit,
    onAccentChanged: (String) -> Unit,
    onApiKeySaved: () -> Unit = {}
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(ApiKeyManager.getApiKey(context)) }
    var savedMessage by remember { mutableStateOf(false) }
    var notifyFavorites by remember {
        mutableStateOf(PreferencesManager.getNotifyFavorites(context))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Beállítások", fontWeight = FontWeight.Bold, fontSize = 18.sp)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SectionLabel("API")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Highlightly API kulcs",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            savedMessage = false
                        },
                        label = { Text("API kulcs") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Button(
                        onClick = {
                            ApiKeyManager.saveApiKey(context, apiKey)
                            savedMessage = true
                            onApiKeySaved()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Mentés", fontWeight = FontWeight.Bold)
                    }
                    if (savedMessage) {
                        Text(
                            text = "✓ Kulcs elmentve – adatok újratöltése...",
                            color = AccentGreen,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            SectionLabel("Megjelenés")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Téma", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "system" to "Rendszer",
                            "light" to "Világos",
                            "dark" to "Sötét"
                        ).forEach { (key, label) ->
                            val selected = themeMode == key
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    PreferencesManager.setThemeMode(context, key)
                                    onThemeModeChanged(key)
                                },
                                label = {
                                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Accent szín", fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        listOf(
                            "blue" to AccentBlue,
                            "green" to AccentGreen,
                            "gold" to AccentGold,
                            "purple" to AccentPurple,
                            "orange" to AccentOrange
                        ).forEach { (key, color) ->
                            val selected = accentKey == key
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (selected) Modifier.border(3.dp, Color.White, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        PreferencesManager.setAccent(context, key)
                                        onAccentChanged(key)
                                    }
                            )
                        }
                    }
                }
            }

            SectionLabel("Értesítések")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Kedvenc meccsek értesítése",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Gól és kezdés (hamarosan)",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notifyFavorites,
                        onCheckedChange = {
                            notifyFavorites = it
                            PreferencesManager.setNotifyFavorites(context, it)
                        }
                    )
                }
            }

            SectionLabel("Névjegy")
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("LIVE FUTÁR ⚽", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Verzió 1.1 · Prémium élő eredmények",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.sp
    )
}
