package com.livefutar.app.data

import com.livefutar.app.model.OddsApiResponse

/** A legjobb (legmagasabb) elérhető odds a fő kimenetekre. */
data class BestOdds(
    val home: Double?,
    val draw: Double?,
    val away: Double?
)

/**
 * Az esélyeket (odds) csak egyszer kérjük le meccsenként, és memóriában
 * gyorsítótárazzuk - így nem hívjuk feleslegesen ugyanazt a végpontot
 * minden 60 másodperces háttérfrissítéskor vagy minden újrarajzoláskor.
 * App-újraindításkor törlődik.
 */
object OddsCache {
    private val cache = mutableMapOf<Long, BestOdds?>()
    private val loading = mutableSetOf<Long>()

    fun get(matchId: Long): BestOdds? = cache[matchId]

    fun has(matchId: Long): Boolean = cache.containsKey(matchId)

    fun isLoading(matchId: Long): Boolean = loading.contains(matchId)

    fun markLoading(matchId: Long) {
        loading.add(matchId)
    }

    fun set(matchId: Long, odds: BestOdds?) {
        cache[matchId] = odds
        loading.remove(matchId)
    }

    /** A fő 1X2 piac (bármilyen néven érkezzen) legjobb odds-ait szedi ki a válaszból. */
    fun extractBest1X2(response: OddsApiResponse): BestOdds? {
        val markets = response.data
            ?.flatMap { it.odds.orEmpty() }
            ?.filter { bookmaker ->
                val m = bookmaker.market?.trim()?.lowercase() ?: return@filter false
                m == "full time result" || m == "match result" || m == "1x2"
            }
            .orEmpty()

        if (markets.isEmpty()) return null

        var bestHome: Double? = null
        var bestDraw: Double? = null
        var bestAway: Double? = null

        markets.forEach { bookmaker ->
            bookmaker.values.orEmpty().forEach { v ->
                val odd = v.odd ?: return@forEach
                val outcome = v.value?.trim()?.lowercase()
                when (outcome) {
                    "home", "1" -> if (bestHome == null || odd > bestHome!!) bestHome = odd
                    "draw", "x" -> if (bestDraw == null || odd > bestDraw!!) bestDraw = odd
                    "away", "2" -> if (bestAway == null || odd > bestAway!!) bestAway = odd
                }
            }
        }

        if (bestHome == null && bestDraw == null && bestAway == null) return null
        return BestOdds(bestHome, bestDraw, bestAway)
    }
}
