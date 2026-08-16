package com.livefutar.app.model

data class MatchDetailsResponse(
    val id: Long?,
    val predictions: MatchPredictions?
)

data class MatchPredictions(
    val prematch: List<PredictionEntry>?,
    val live: List<PredictionEntry>?
)

data class PredictionEntry(
    val type: String?,
    val modelType: String?,
    val generatedAt: String?,
    val description: String?,
    val probabilities: PredictionProbabilities?
)

data class PredictionProbabilities(
    val home: String?,
    val draw: String?,
    val away: String?
)
