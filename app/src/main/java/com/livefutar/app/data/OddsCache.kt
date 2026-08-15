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

    /** A "Full Time Result" (1X2) piac legjobb odds-ait szedi ki a válaszból. */
    fun extractBest1X2(response: OddsApiResponse): BestOdds? {
        val markets = response.data
            ?.flatMap { it.odds.orEmpty() }
            ?.filter { it.market?.contains("Full Time Result", ignoreCase = true) == true }
            .orEmpty()

        if (markets.isEmpty()) return null

        var bestHome: Double? = null
        var bestDraw: Double? = null
        var bestAway: Double? = null

        markets.forEach { bookmaker ->
            bookmaker.values.orEmpty().forEach { v ->
                val odd = v.odd ?: return@forEach
                when (v.value?.lowercase()) {
                    "home" -> if (bestHome == null || odd > bestHome!!) bestHome = odd
                    "draw" -> if (bestDraw == null || odd > bestDraw!!) bestDraw = odd
                    "away" -> if (bestAway == null || odd > bestAway!!) bestAway = odd
                }
            }
        }

        if (bestHome == null && bestDraw == null && bestAway == null) return null
        return BestOdds(bestHome, bestDraw, bestAway)
    }
}
