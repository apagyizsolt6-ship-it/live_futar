package com.livefutar.app.model

/**
 * A "predictions" mező típusai (MatchPredictions, PredictionItem,
 * PredictionProbabilities) már léteznek az OddsModels.kt-ban - itt csak
 * a /matches/{id} válasz gyökér-objektumát adjuk hozzá, ami ezeket
 * tartalmazza.
 */
data class MatchDetailsResponse(
    val id: Long?,
    val predictions: MatchPredictions?
)
