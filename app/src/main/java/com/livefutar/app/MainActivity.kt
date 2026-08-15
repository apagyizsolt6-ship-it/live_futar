package com.livefutar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.livefutar.app.data.ApiKeyManager
import com.livefutar.app.data.FavoritesManager
import com.livefutar.app.data.FootballApiService
import com.livefutar.app.data.HalfTimeScoreCache
import com.livefutar.app.data.NotificationHelper
import com.livefutar.app.data.PreferencesManager
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.LeagueModel
import com.livefutar.app.model.MatchModel
import com.livefutar.app.ui.components.LiveFutarBottomBar
import com.livefutar.app.ui.screens.BetSlipScreen
import com.livefutar.app.ui.screens.HighlightsScreen
import com.livefutar.app.ui.screens.HomeScreen
import com.livefutar.app.ui.screens.LiveScreen
import com.livefutar.app.ui.screens.MatchDetailScreen
import com.livefutar.app.ui.screens.SettingsScreen
import com.livefutar.app.ui.screens.StandingsScreen
import com.livefutar.app.ui.screens.VideoPlayerScreen
import com.livefutar.app.ui.theme.LiveFutarTheme
import com.livefutar.app.util.DateUtils
import com.livefutar.app.widget.LiveFutarWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val AUTO_REFRESH_INTERVAL_MS = 60_000L
private const val MATCH_PAGE_SIZE = 100
private const val MAX_MATCH_PAGES = 20

/**
 * A teljes napi meccslista lekérése lapozással.
 *
 * A Highlightly /matches végpont egyszerre legfeljebb 100
 * mérkőzést ad vissza. A korábbi kód csak az első oldalt
 * kérte le, ezért sok élő mérkőzés kieshetett.
 */
suspend fun fetchAllMatches(
    apiService: FootballApiService,
    apiKey: String,
    date: String
): List<MatchModel> {

    val allMatches = mutableListOf<MatchModel>()
    var offset = 0
    var page = 0
    var totalCount: Int? = null

    while (page < MAX_MATCH_PAGES) {

        val response =
            apiService.getMatches(
                apiKey = apiKey,
                date = date,
                timezone = "Europe/Budapest",
                limit = MATCH_PAGE_SIZE,
                offset = offset
            )

        val pageMatches =
            response.data.orEmpty()

        if (totalCount == null) {
            totalCount =
                response.pagination?.totalCount
        }

        allMatches += pageMatches

        if (pageMatches.isEmpty()) {
            break
        }

        val nextOffset =
            offset + pageMatches.size

        val reachedTotal =
            totalCount != null &&
                nextOffset >= totalCount!!

        val lastPage =
            pageMatches.size < MATCH_PAGE_SIZE

        if (reachedTotal || lastPage) {
            break
        }

        offset = nextOffset
        page++
    }

    return allMatches
        .distinctBy { it.id }
}

class MainActivity : ComponentActivity() {

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContent {

            val context =
                androidx.compose.ui.platform.LocalContext.current

            var themeMode by remember {
                mutableStateOf(
                    PreferencesManager
                        .getThemeMode(context)
                )
            }

            var accentKey by remember {
                mutableStateOf(
                    PreferencesManager
                        .getAccent(context)
                )
            }

            LiveFutarTheme(
                themeMode = themeMode,
                accentKey = accentKey
            ) {

                var currentScreen by remember {
                    mutableStateOf("home")
                }

                var selectedMatch by remember {
                    mutableStateOf<MatchModel?>(null)
                }

                var selectedHighlight by remember {
                    mutableStateOf<HighlightModel?>(null)
                }

                var standingsLeague by remember {
                    mutableStateOf<LeagueModel?>(null)
                }

                var showBetSlip by remember {
                    mutableStateOf(false)
                }

                /*
                 * A kiválasztott nap mérkőzései.
                 *
                 * Ezt használja a Home / Meccsek képernyő.
                 */
                var matches by remember {
                    mutableStateOf<List<MatchModel>>(
                        emptyList()
                    )
                }

                /*
                 * A MAI nap mérkőzései.
                 *
                 * Ezt kizárólag az Élő képernyő használja.
                 *
                 * Így ha a felhasználó például Holnapra vált,
                 * az Élő képernyő nem a holnapi listából próbál
                 * élő meccseket keresni.
                 */
                var todayMatches by remember {
                    mutableStateOf<List<MatchModel>>(
                        emptyList()
                    )
                }

                var highlights by remember {
                    mutableStateOf<List<HighlightModel>>(
                        emptyList()
                    )
                }

                var isLoading by remember {
                    mutableStateOf(true)
                }

                var isRefreshing by remember {
                    mutableStateOf(false)
                }

                var errorMessage by remember {
                    mutableStateOf<String?>(null)
                }

                var selectedDate by remember {
                    mutableStateOf(
                        DateUtils.today()
                    )
                }

                var reloadTrigger by remember {
                    mutableStateOf(0)
                }

                val apiService =
                    remember {
                        FootballApiService.create()
                    }

                var favoriteTeamIds by remember {
                    mutableStateOf(
                        FavoritesManager
                            .getFavoriteTeamIds(context)
                    )
                }

                var favoriteLeagueIds by remember {
                    mutableStateOf(
                        FavoritesManager
                            .getFavoriteLeagueIds(context)
                    )
                }

                var showOnlyFavorites by remember {
                    mutableStateOf(false)
                }

                var showOnlyLive by remember {
                    mutableStateOf(false)
                }

                /*
                 * Az előző MAI mérkőzés-pillanatkép.
                 *
                 * Ez külön van a kiválasztott nap listájától,
                 * mert a kedvenc-meccsek értesítéseinek mindig
                 * a mai élő állapotváltozást kell figyelniük.
                 */
                var previousTodayMatchesById by remember {
                    mutableStateOf<Map<Long, MatchModel>>(
                        emptyMap()
                    )
                }

                fun toggleTeamFavorite(
                    teamId: Long
                ) {

                    FavoritesManager
                        .toggleTeamFavorite(
                            context,
                            teamId
                        )

                    favoriteTeamIds =
                        FavoritesManager
                            .getFavoriteTeamIds(context)
                }

                fun toggleLeagueFavorite(
                    leagueId: Long
                ) {

                    FavoritesManager
                        .toggleLeagueFavorite(
                            context,
                            leagueId
                        )

                    favoriteLeagueIds =
                        FavoritesManager
                            .getFavoriteLeagueIds(context)
                }

                /*
                 * Kedvenc csapatok eseményeinek ellenőrzése.
                 *
                 * Mindig a MAI meccsekből dolgozik.
                 */
                fun checkFavoriteMatchEvents(
                    newMatches: List<MatchModel>
                ) {

                    if (
                        !PreferencesManager
                            .getNotifyFavorites(context)
                    ) {
                        return
                    }

                    val previous =
                        previousTodayMatchesById

                    val favTeams =
                        FavoritesManager
                            .getFavoriteTeamIds(context)

                    if (favTeams.isEmpty()) {
                        return
                    }

                    newMatches.forEach { match ->

                        val homeId =
                            match.homeTeam?.id

                        val awayId =
                            match.awayTeam?.id

                        val isFavoriteMatch =
                            (
                                homeId != null &&
                                    favTeams.contains(homeId)
                                ) ||
                                (
                                    awayId != null &&
                                        favTeams.contains(awayId)
                                )

                        if (!isFavoriteMatch) {
                            return@forEach
                        }

                        val prev =
                            previous[match.id]
                                ?: return@forEach

                        val homeName =
                            match.homeTeam?.name
                                ?: "Hazai"

                        val awayName =
                            match.awayTeam?.name
                                ?: "Vendég"

                        /*
                         * Meccs elkezdődött.
                         */
                        if (
                            prev.isNotStarted &&
                            match.isLive
                        ) {

                            NotificationHelper
                                .notifyKickoff(
                                    context,
                                    "⚽ Elkezdődött: $homeName – $awayName",
                                    match.leagueDisplayName
                                )
                        }

                        /*
                         * Gólszerzés.
                         */
                        val prevHome =
                            prev.homeScoreDisplay
                                .toIntOrNull()
                                ?: 0

                        val prevAway =
                            prev.awayScoreDisplay
                                .toIntOrNull()
                                ?: 0

                        val newHome =
                            match.homeScoreDisplay
                                .toIntOrNull()
                                ?: 0

                        val newAway =
                            match.awayScoreDisplay
                                .toIntOrNull()
                                ?: 0

                        if (
                            match.isLive &&
                            (
                                newHome > prevHome ||
                                    newAway > prevAway
                                )
                        ) {

                            NotificationHelper
                                .notifyGoal(
                                    context,
                                    "⚽ Gól! $homeName $newHome - $newAway $awayName",
                                    match.leagueDisplayName
                                )
                        }
                    }
                }

                /*
                 * Adatok lekérése.
                 *
                 * Fontos:
                 *
                 * - Home / Meccsek esetén a selectedDate-et kérjük.
                 * - Élő esetén mindig a MAI napot kérjük.
                 *
                 * Így nincs szükség külön élő API-végpontra,
                 * és nem kérjük le egyszerre a két napot.
                 */
                suspend fun fetchData(
                    isBackground: Boolean
                ) {

                    if (isBackground) {

                        isRefreshing = true

                    } else {

                        isLoading = true
                        errorMessage = null
                    }

                    val apiKey =
                        ApiKeyManager
                            .getApiKey(context)

                    if (apiKey.isBlank()) {

                        if (!isBackground) {

                            errorMessage =
                                "Kérlek add meg az API kulcsot a Beállításokban!"
                        }

                        isLoading = false
                        isRefreshing = false

                        return
                    }

                    /*
                     * A tényleges mai dátum.
                     *
                     * NEM selectedDate.
                     */
                    val realToday =
                        DateUtils.today()

                    /*
                     * Ha az Élő képernyő van megnyitva,
                     * akkor a MAI napot kérjük.
                     *
                     * Egyébként a kiválasztott napot.
                     */
                    val requestedDate =
                        if (
                            currentScreen == "live"
                        ) {
                            realToday
                        } else {
                            selectedDate
                        }

                    try {

                        withContext(Dispatchers.IO) {

                            /*
                             * Egyetlen /matches lekérés.
                             *
                             * Nem kérjük le egyszerre a kiválasztott
                             * napot ÉS a mai napot.
                             */
                            val newMatches =
                                fetchAllMatches(
                                    apiService = apiService,
                                    apiKey = apiKey,
                                    date = requestedDate
                                )

                            /*
                             * Highlights továbbra is ugyanahhoz
                             * a dátumhoz tartozik, amit éppen nézünk.
                             */
                            val newHighlights =
                                apiService
                                    .getHighlights(
                                        apiKey,
                                        requestedDate
                                    )
                                    .data
                                    ?: emptyList()

                            /*
                             * Ha a MAI napot kértük:
                             *
                             * - frissítjük todayMatches-t
                             * - ellenőrizzük a kedvenc eseményeket
                             * - frissítjük az előző pillanatképet
                             */
                            if (
                                requestedDate == realToday
                            ) {

                                checkFavoriteMatchEvents(
                                    newMatches
                                )

                                /*
                                 * A kezdőképernyő-widget is a mai nap
                                 * kedvenc meccseit mutatja - mivel ez az
                                 * adat úgyis lekérésre került, nincs
                                 * extra hálózati hívás.
                                 */
                                LiveFutarWidgetUpdater.updateWithMatches(
                                    context,
                                    newMatches
                                )

                                /*
                                 * Félidei eredmény cache.
                                 */
                                newMatches.forEach { match ->

                                    if (
                                        match.state
                                            ?.description ==
                                        "Half time"
                                    ) {

                                        HalfTimeScoreCache.set(
                                            match.id,
                                            match.state
                                                ?.score
                                                ?.current
                                        )
                                    }
                                }

                                todayMatches =
                                    newMatches

                                previousTodayMatchesById =
                                    newMatches
                                        .associateBy {
                                            it.id
                                        }

                                /*
                                 * Ha éppen a mai nap van kiválasztva,
                                 * akkor ugyanazt a listát a normál
                                 * Meccsek képernyő is használhatja.
                                 */
                                if (
                                    selectedDate == realToday
                                ) {

                                    matches =
                                        newMatches
                                }

                            } else {

                                /*
                                 * Nem a mai napot nézzük.
                                 *
                                 * A normál Meccsek listája frissül,
                                 * de a mai élő lista érintetlen marad.
                                 */
                                matches =
                                    newMatches
                            }

                            highlights =
                                newHighlights
                        }

                        errorMessage = null

                    } catch (e: Exception) {

                        if (!isBackground) {

                            errorMessage =
                                "Hiba történt az adatok betöltésekor: ${e.localizedMessage}"
                        }

                    } finally {

                        isLoading = false
                        isRefreshing = false
                    }
                }

                /*
                 * Első betöltés és dátumváltás.
                 */
                LaunchedEffect(
                    reloadTrigger,
                    selectedDate,
                    currentScreen
                ) {

                    fetchData(
                        isBackground = false
                    )
                }

                /*
                 * Automatikus frissítés.
                 *
                 * Továbbra is 60 másodperc.
                 *
                 * Nincs plusz folyamatos API-lekérés.
                 */
                LaunchedEffect(Unit) {

                    while (true) {

                        delay(
                            AUTO_REFRESH_INTERVAL_MS
                        )

                        fetchData(
                            isBackground = true
                        )
                    }
                }

                /*
                 * Az Élő számláló kizárólag a MAI listából készül.
                 */
                val liveCount =
                    todayMatches.count {
                        it.isLive
                    }

                val showBottomBar =
                    selectedMatch == null &&
                        selectedHighlight == null &&
                        standingsLeague == null &&
                        !showBetSlip

                Scaffold(

                    bottomBar = {

                        if (showBottomBar) {

                            LiveFutarBottomBar(

                                currentScreen =
                                    currentScreen,

                                liveCount =
                                    liveCount,

                                onScreenSelected = {
                                    screen ->

                                    currentScreen =
                                        screen
                                }
                            )
                        }
                    }

                ) { paddingValues ->

                    Column(

                        modifier =
                            Modifier
                                .padding(
                                    paddingValues
                                )
                                .fillMaxSize()

                    ) {

                        if (
                            isRefreshing &&
                            currentScreen != "live"
                        ) {

                            LinearProgressIndicator(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                            )
                        }

                        Box(
                            modifier =
                                Modifier.fillMaxSize()
                        ) {

                            when {

                                selectedHighlight != null -> {

                                    VideoPlayerScreen(

                                        highlight =
                                            selectedHighlight!!,

                                        onBackClick = {
                                            selectedHighlight =
                                                null
                                        }
                                    )
                                }

                                standingsLeague != null -> {

                                    StandingsScreen(

                                        league =
                                            standingsLeague!!,

                                        onBackClick = {
                                            standingsLeague =
                                                null
                                        }
                                    )
                                }

                                showBetSlip -> {

                                    BetSlipScreen(

                                        onBackClick = {
                                            showBetSlip =
                                                false
                                        }
                                    )
                                }

                                selectedMatch != null -> {

                                    MatchDetailScreen(

                                        match =
                                            selectedMatch!!,

                                        onBackClick = {
                                            selectedMatch =
                                                null
                                        },

                                        onOpenBetSlip = {
                                            showBetSlip =
                                                true
                                        },

                                        onStandingsClick = {
                                            match ->

                                            match.league?.let {

                                                standingsLeague =
                                                    it
                                            }
                                        }
                                    )
                                }

                                currentScreen == "settings" -> {

                                    SettingsScreen(

                                        themeMode =
                                            themeMode,

                                        accentKey =
                                            accentKey,

                                        onThemeModeChanged = {
                                            themeMode =
                                                it
                                        },

                                        onAccentChanged = {
                                            accentKey =
                                                it
                                        },

                                        onApiKeySaved = {
                                            reloadTrigger++
                                        }
                                    )
                                }

                                isLoading -> {

                                    Box(

                                        modifier =
                                            Modifier
                                                .fillMaxSize(),

                                        contentAlignment =
                                            Alignment.Center

                                    ) {

                                        CircularProgressIndicator()
                                    }
                                }

                                errorMessage != null &&
                                    currentScreen != "settings" -> {

                                    Column(

                                        modifier =
                                            Modifier
                                                .fillMaxSize()
                                                .padding(24.dp),

                                        horizontalAlignment =
                                            Alignment.CenterHorizontally,

                                        verticalArrangement =
                                            Arrangement.Center

                                    ) {

                                        Text(
                                            text =
                                                errorMessage!!
                                        )

                                        Spacer(
                                            modifier =
                                                Modifier
                                                    .padding(8.dp)
                                        )

                                        Button(
                                            onClick = {
                                                reloadTrigger++
                                            }
                                        ) {

                                            Text(
                                                "Újrapróbálkozás"
                                            )
                                        }

                                        Spacer(
                                            modifier =
                                                Modifier
                                                    .padding(4.dp)
                                        )

                                        Button(
                                            onClick = {
                                                currentScreen =
                                                    "settings"
                                            }
                                        ) {

                                            Text(
                                                "Beállítások"
                                            )
                                        }
                                    }
                                }

                                currentScreen == "live" -> {

                                    /*
                                     * FONTOS:
                                     *
                                     * Az Élő képernyő MOSTANTÓL
                                     * kizárólag a mai listát kapja.
                                     *
                                     * Nem a selectedDate listát.
                                     */
                                    LiveScreen(

                                        matches =
                                            todayMatches,

                                        favoriteTeamIds =
                                            favoriteTeamIds,

                                        onToggleTeamFavorite = {
                                            id ->

                                            toggleTeamFavorite(
                                                id
                                            )
                                        },

                                        isRefreshing =
                                            isRefreshing,

                                        onRefresh = {
                                            reloadTrigger++
                                        },

                                        onMatchClick = {
                                            match ->

                                            selectedMatch =
                                                match
                                        }
                                    )
                                }

                                currentScreen == "home" -> {

                                    HomeScreen(

                                        matches =
                                            matches,

                                        selectedDate =
                                            selectedDate,

                                        onDateSelected = {
                                            newDate ->

                                            selectedDate =
                                                newDate
                                        },

                                        favoriteTeamIds =
                                            favoriteTeamIds,

                                        favoriteLeagueIds =
                                            favoriteLeagueIds,

                                        onToggleTeamFavorite = {
                                            id ->

                                            toggleTeamFavorite(
                                                id
                                            )
                                        },

                                        onToggleLeagueFavorite = {
                                            id ->

                                            toggleLeagueFavorite(
                                                id
                                            )
                                        },

                                        showOnlyFavorites =
                                            showOnlyFavorites,

                                        onToggleShowOnlyFavorites = {

                                            showOnlyFavorites =
                                                !showOnlyFavorites
                                        },

                                        showOnlyLive =
                                            showOnlyLive,

                                        onToggleShowOnlyLive = {

                                            showOnlyLive =
                                                !showOnlyLive
                                        },

                                        isRefreshing =
                                            isRefreshing,

                                        onRefresh = {
                                            reloadTrigger++
                                        },

                                        onMatchClick = {
                                            match ->

                                            selectedMatch =
                                                match
                                        },

                                        onStandingsClick = {
                                            match ->

                                            match.league?.let {

                                                standingsLeague =
                                                    it
                                            }
                                        }
                                    )
                                }

                                currentScreen == "highlights" -> {

                                    HighlightsScreen(

                                        highlights =
                                            highlights,

                                        onHighlightClick = {
                                            highlight ->

                                            selectedHighlight =
                                                highlight
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
