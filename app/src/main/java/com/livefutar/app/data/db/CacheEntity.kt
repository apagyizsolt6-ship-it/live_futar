package com.livefutar.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "match_cache")
data class MatchCacheEntity(
    @PrimaryKey val cacheKey: String,
    val json: String,
    val updatedAt: Long
)
