package com.livefutar.app.util

/**
 * Top bajnokságok prioritása a Home listában (előre rendezés).
 * Minél kisebb a szám, annál előrébb.
 */
object LeaguePriority {

    private val ranks = mapOf(
        // UEFA / világ
        "champions league" to 1,
        "bajnokok ligája" to 1,
        "europa league" to 2,
        "európa liga" to 2,
        "conference league" to 3,
        "konferencia liga" to 3,
        "nations league" to 4,
        "nemzetek ligája" to 4,
        "world cup" to 1,
        "világbajnokság" to 1,
        "european championship" to 1,
        "európa-bajnokság" to 1,
        // Top 5
        "premier league" to 10,
        "la liga" to 11,
        "laliga" to 11,
        "serie a" to 12,
        "bundesliga" to 13,
        "ligue 1" to 14,
        // Magyarország
        "nb i" to 15,
        "otp bank liga" to 15,
        "nb ii" to 25,
        "magyar kupa" to 20,
        // Egyéb erős
        "primeira liga" to 30,
        "eredivisie" to 31,
        "süper lig" to 32,
        "super lig" to 32,
        "championship" to 40,
        "mls" to 45
    )

    fun rank(leagueDisplayName: String?): Int {
        if (leagueDisplayName.isNullOrBlank()) return 500
        val lower = leagueDisplayName.lowercase()
        ranks.entries
            .filter { lower.contains(it.key) }
            .minByOrNull { it.value }
            ?.let { return it.value }
        return 400
    }
}
