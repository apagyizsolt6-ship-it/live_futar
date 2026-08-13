package com.livefutar.app.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.livefutar.app.model.HighlightModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    highlight: HighlightModel,
    onBackClick: () -> Unit
) {
    // Előnyben az embedUrl (iframe-be szánt, natívan beágyazható lejátszó),
    // ha az nincs, visszaesünk a sima url-re.
    val playbackUrl = highlight.embedUrl ?: highlight.url

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        highlight.title ?: "Videó lejátszása",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (playbackUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Text("Ehhez a klipphez jelenleg nem érhető el lejátszható link.")
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                ) {
                    EmbeddedVideoWebView(url = playbackUrl)
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbeddedVideoWebView(url: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.mediaPlaybackRequiresUserGesture = false
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadUrl(url)
            }
        },
        update = { webView ->
            webView.loadUrl(url)
        }
    )
}
