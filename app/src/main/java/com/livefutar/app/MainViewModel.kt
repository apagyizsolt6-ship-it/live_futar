package com.livefutar.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.livefutar.app.data.ApiKeyManager
import com.livefutar.app.data.FavoritesManager
import com.livefutar.app.data.FootballApiService
import com.livefutar.app.data.HalfTimeScoreCache
import com.livefutar.app.data.MatchesCache
import com.livefutar.app.data.NotificationHelper
import com.livefutar.app.data.PreferencesManager
import com.livefutar.app.model.HighlightModel
import com.livefutar.app.model.MatchModel
import com.livefutar.app.util.DateUtils
import com.livefutar.app.widget.LiveFutarWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val AUTO_REFRESH_INTERVAL_MS = 60_000L
private const val MATCH_PAGE_SIZE = 100
private const val MAX_MATCH_PAGES = 20

data class MainUiState(
    val matches: List<MatchModel> = emptyList(),
    val todayMatches: List<MatchModel> = emptyList(),
    val highlights: List<HighlightModel> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    /** true = a megjelenített lista cache-ből jött (nincs friss hálózat) */
    val isOffline: Boolean = false,
    val selectedDate: String = DateUtils.today(),
    val favoriteTeamIds: Set<Long> = emptySet(),
    val favoriteLeagueIds: Set<Long> = emptySet(),
    val showOnlyFavorites: Boolean = false,
    val showOnlyLive: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val apiService = FootballApiService.create()

    private val _ui = MutableStateFlow(
        MainUiState(
            favoriteTeamIds = FavoritesManager.getFavoriteTeamIds(appContext),
            favoriteLeagueIds = FavoritesManager.getFavoriteLeagueIds(appContext)
        )
    )
    val ui: StateFlow<MainUiState> = _ui.asStateFlow()

    private var previousTodayMatchesById: Map<Long, MatchModel> = emptyMap()
    private var autoRefreshJob: Job? = null
    private var loadJob: Job? = null

    /** Utolsó preferToday – háttérfrissítéshez. */
    private var lastPreferToday: Boolean = false

    init {
        hydrateFromCache()
        startAutoRefresh()
    }

    private fun hydrateFromCache() {
        val today = DateUtils.today()
        val cachedToday = MatchesCache.loadToday(appContext, today)
        val cachedDate = MatchesCache.loadMatches(appContext, today)
        val cachedHl = MatchesCache.loadHighlights(appContext, today)
        if (cachedToday != null || cachedDate != null) {
            _ui.update {
                it.copy(
                    todayMatches = cachedToday ?: cachedDate ?: emptyList(),
                    matches = cachedDate ?: cachedToday ?: emptyList(),
                    highlights = cachedHl ?: emptyList(),
                    isLoading = false,
                    isOffline = true
                )
            }
            previousTodayMatchesById =
                (cachedToday ?: cachedDate ?: emptyList()).associateBy { it.id }
        }
    }

    fun setSelectedDate(date: String) {
        if (_ui.value.selectedDate == date) return
        _ui.update { it.copy(selectedDate = date) }
    }

    fun toggleShowOnlyFavorites() {
        _ui.update { it.copy(showOnlyFavorites = !it.showOnlyFavorites) }
    }

    fun toggleShowOnlyLive() {
        _ui.update { it.copy(showOnlyLive = !it.showOnlyLive) }
    }

    fun toggleTeamFavorite(teamId: Long) {
        FavoritesManager.toggleTeamFavorite(appContext, teamId)
        _ui.update {
            it.copy(favoriteTeamIds = FavoritesManager.getFavoriteTeamIds(appContext))
        }
    }

    fun toggleLeagueFavorite(leagueId: Long) {
        FavoritesManager.toggleLeagueFavorite(appContext, leagueId)
        _ui.update {
            it.copy(favoriteLeagueIds = FavoritesManager.getFavoriteLeagueIds(appContext))
        }
    }

    fun refresh(preferToday: Boolean = lastPreferToday) {
        load(isBackground = true, preferToday = preferToday)
    }

    fun load(isBackground: Boolean, preferToday: Boolean) {
        lastPreferToday = preferToday
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            fetchData(isBackground = isBackground, preferToday = preferToday)
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (isActive) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                fetchData(isBackground = true, preferToday = lastPreferToday)
            }
        }
    }

    private suspend fun fetchData(isBackground: Boolean, preferToday: Boolean) {
        if (isBackground) {
            _ui.update { it.copy(isRefreshing = true) }
        } else {
            _ui.update { it.copy(isLoading = true, errorMessage = null) }
        }

        val apiKey = ApiKeyManager.getApiKey(appContext)
        if (apiKey.isBlank()) {
            if (!isBackground) {
                _ui.update {
                    it.copy(
                        errorMessage = "Kérlek add meg az API kulcsot a Beállításokban!",
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            } else {
                _ui.update { it.copy(isRefreshing = false) }
            }
            return
        }

        val realToday = DateUtils.today()
        val selectedDate = _ui.value.selectedDate
        val requestedDate = if (preferToday) realToday else selectedDate

        try {
            val (newMatches, newHighlights) = withContext(Dispatchers.IO) {
                val matches = fetchAllMatches(apiService, apiKey, requestedDate)
                val highlights = apiService
                    .getHighlights(apiKey, requestedDate)
                    .data
                    ?: emptyList()
                matches to highlights
            }

            if (requestedDate == realToday) {
                checkFavoriteMatchEvents(newMatches)
                LiveFutarWidgetUpdater.updateWithMatches(appContext, newMatches)
                newMatches.forEach { match ->
                    if (match.state?.description == "Half time") {
                        HalfTimeScoreCache.set(
                            match.id,
                            match.state?.score?.current
                        )
                    }
                }
                previousTodayMatchesById = newMatches.associateBy { it.id }

                MatchesCache.saveToday(appContext, realToday, newMatches)
                MatchesCache.saveMatches(appContext, realToday, newMatches)
                MatchesCache.saveHighlights(appContext, realToday, newHighlights)

                _ui.update { state ->
                    state.copy(
                        todayMatches = newMatches,
                        matches = if (selectedDate == realToday) newMatches else state.matches,
                        highlights = if (selectedDate == realToday || preferToday) {
                            newHighlights
                        } else {
                            state.highlights
                        },
                        isOffline = false,
                        errorMessage = null,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            } else {
                MatchesCache.saveMatches(appContext, requestedDate, newMatches)
                MatchesCache.saveHighlights(appContext, requestedDate, newHighlights)

                _ui.update {
                    it.copy(
                        matches = newMatches,
                        highlights = newHighlights,
                        isOffline = false,
                        errorMessage = null,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            }
        } catch (e: Exception) {
            // Offline fallback
            val cachedMatches = MatchesCache.loadMatches(appContext, requestedDate)
            val cachedHl = MatchesCache.loadHighlights(appContext, requestedDate)
            val cachedToday = if (requestedDate == realToday) {
                MatchesCache.loadToday(appContext, realToday)
            } else {
                null
            }

            val fallbackMatches = cachedMatches ?: cachedToday
            if (fallbackMatches != null && !isBackground) {
                _ui.update { state ->
                    state.copy(
                        matches = if (requestedDate != realToday || selectedDate == realToday) {
                            fallbackMatches
                        } else {
                            state.matches
                        },
                        todayMatches = if (requestedDate == realToday) {
                            fallbackMatches
                        } else {
                            state.todayMatches
                        },
                        highlights = cachedHl ?: state.highlights,
                        isOffline = true,
                        errorMessage = null,
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            } else if (!isBackground) {
                _ui.update {
                    it.copy(
                        errorMessage = "Hiba történt az adatok betöltésekor: ${e.localizedMessage}",
                        isLoading = false,
                        isRefreshing = false
                    )
                }
            } else {
                _ui.update { it.copy(isRefreshing = false) }
            }
        }
    }

    private fun checkFavoriteMatchEvents(newMatches: List<MatchModel>) {
        if (!PreferencesManager.getNotifyFavorites(appContext)) return

        val previous = previousTodayMatchesById
        val favTeams = FavoritesManager.getFavoriteTeamIds(appContext)
        if (favTeams.isEmpty()) return

        newMatches.forEach { match ->
            val homeId = match.homeTeam?.id
            val awayId = match.awayTeam?.id
            val isFavoriteMatch =
                (homeId != null && homeId in favTeams) ||
                    (awayId != null && awayId in favTeams)
            if (!isFavoriteMatch) return@forEach

            val prev = previous[match.id] ?: return@forEach
            val homeName = match.homeTeam?.displayName ?: "Hazai"
            val awayName = match.awayTeam?.displayName ?: "Vendég"

            if (prev.isNotStarted && match.isLive) {
                NotificationHelper.notifyKickoff(
                    appContext,
                    "⚽ Elkezdődött: $homeName – $awayName",
                    match.leagueDisplayName,
                    match.id
                )
            }

            val prevHome = prev.homeScoreDisplay.toIntOrNull() ?: 0
            val prevAway = prev.awayScoreDisplay.toIntOrNull() ?: 0
            val newHome = match.homeScoreDisplay.toIntOrNull() ?: 0
            val newAway = match.awayScoreDisplay.toIntOrNull() ?: 0

            if (match.isLive && (newHome > prevHome || newAway > prevAway)) {
                NotificationHelper.notifyGoal(
                    appContext,
                    "⚽ Gól! $homeName $newHome - $newAway $awayName",
                    match.leagueDisplayName,
                    match.id
                )
            }

            if (prev.isLive && match.isFinished) {
                NotificationHelper.notifyFullTime(
                    appContext,
                    "🏁 Vége: $homeName $newHome - $newAway $awayName",
                    match.leagueDisplayName,
                    match.id
                )
            }
        }
    }
}

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
        val response = apiService.getMatches(
            apiKey = apiKey,
            date = date,
            timezone = "Europe/Budapest",
            limit = MATCH_PAGE_SIZE,
            offset = offset
        )
        val pageMatches = response.data.orEmpty()
        if (totalCount == null) {
            totalCount = response.pagination?.totalCount
        }
        allMatches += pageMatches
        if (pageMatches.isEmpty()) break
        val nextOffset = offset + pageMatches.size
        val reachedTotal = totalCount != null && nextOffset >= totalCount!!
        val lastPage = pageMatches.size < MATCH_PAGE_SIZE
        if (reachedTotal || lastPage) break
        offset = nextOffset
        page++
    }
    return allMatches.distinctBy { it.id }
}
