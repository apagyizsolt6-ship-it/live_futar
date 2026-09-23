package com.livefutar.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/**
 * Egyszerű pull-to-refresh konténer NestedScroll-lal.
 * Nem igényel újabb Material3 BOM-ot.
 *
 * Használat:
 * ```
 * PullRefreshBox(
 *     isRefreshing = isRefreshing,
 *     onRefresh = { ... }
 * ) {
 *     LazyColumn(...) { ... }
 * }
 * ```
 */
@Composable
fun PullRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val triggerPx = with(density) { 72.dp.toPx() }
    var pullOffset by remember { mutableStateOf(0f) }

    val connection = remember(isRefreshing, enabled, triggerPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!enabled || isRefreshing) return Offset.Zero
                // Felhúzáskor csökkentjük a pull offsetet
                if (source == NestedScrollSource.Drag && available.y < 0f && pullOffset > 0f) {
                    val consumed = available.y.coerceAtLeast(-pullOffset)
                    pullOffset += consumed
                    return Offset(0f, consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (!enabled || isRefreshing) return Offset.Zero
                // Lefelé húzás a lista tetején → pull
                if (source == NestedScrollSource.Drag && available.y > 0f) {
                    val add = available.y * 0.55f
                    pullOffset = (pullOffset + add).coerceAtMost(triggerPx * 1.4f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (!enabled || isRefreshing) {
                    pullOffset = 0f
                    return Velocity.Zero
                }
                if (pullOffset >= triggerPx) {
                    onRefresh()
                }
                pullOffset = 0f
                return Velocity.Zero
            }
        }
    }

    // Ha a frissítés befejeződött, reset
    if (!isRefreshing && pullOffset > 0f && pullOffset < 1f) {
        pullOffset = 0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(connection)
    ) {
        content()

        val showBar = isRefreshing || pullOffset > 8f
        if (showBar) {
            val progress = when {
                isRefreshing -> null // indeterminate
                else -> (pullOffset / triggerPx).coerceIn(0f, 1f)
            }
            if (progress == null) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
