package com.livefutar.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CacheDao {
    @Query("SELECT * FROM match_cache WHERE cacheKey = :key LIMIT 1")
    suspend fun get(key: String): MatchCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: MatchCacheEntity)

    @Query("DELETE FROM match_cache WHERE cacheKey = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM match_cache WHERE updatedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}
