package com.livefutar.app.model

import com.livefutar.app.util.HungarianNames

data class TeamModel(
    val id: Long,
    val name: String?,
    val logo: String?,
    val type: String?
) {
    /** Magyar megjelenítendő név (ha van fordítás). */
    val displayName: String
        get() = HungarianNames.team(name)
}
