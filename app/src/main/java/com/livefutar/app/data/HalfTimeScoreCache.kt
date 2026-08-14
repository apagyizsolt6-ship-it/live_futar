package com.livefutar.app.data

/**
 * Az API nem ad vissza külön félidei eredményt, ezért mi magunk "kapjuk el" -
 * amikor egy meccs a háttérfrissítés során "Half time" állapotba kerül,
 * elmentjük az akkori állást. Csak memóriában tárolt, app-újraindításkor törlődik,
 * és csak azokra a meccsekre működik, amik alatt az app fut/frissül.
 */
object HalfTimeScoreCache {
    private val cache = mutableMapOf<Long, String>()

    fun set(matchId: Long, score: String?) {
        if (!score.isNullOrBlank()) cache[matchId] = score
    }

    fun get(matchId: Long): String? = cache[matchId]
}
