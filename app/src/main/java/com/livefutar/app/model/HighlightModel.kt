package com.livefutar.app.model

data class HighlightModel(
    val id: Int,
    val title: String?,
    val url: String?,
    val embedUrl: String?,
    val imgUrl: String?,
    val category: String?,
    val source: String?
) {
    val categoryLabel: String
        get() = when (category) {
            "match-highlights" -> "Összefoglaló"
            "goal-clip" -> "Gólklip"
            "pre-match-content" -> "Meccs előtt"
            "post-match-content" -> "Meccs után"
            "press-conference" -> "Sajtótájékoztató"
            "behind-the-scenes" -> "Kulisszák mögött"
            "live-coverage" -> "Élő közvetítés"
            "discussion-analysis" -> "Elemzés"
            else -> "Videó"
        }
}
