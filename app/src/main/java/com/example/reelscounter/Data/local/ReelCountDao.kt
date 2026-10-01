package com.example.reelscounter.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * The actual SQL Room will run. Room generates the implementation of
 * this interface for you at compile time — you never write the body.
 */
@Dao
interface ReelCountDao {

    @Insert
    suspend fun insert(entity: ReelCountEntity)

    @Query("SELECT COUNT(*) FROM reel_counts")
    fun getTotalCount(): Flow<Int>

    @Query(
        "SELECT COUNT(*) FROM reel_counts " +
                "WHERE timestamp >= :startOfDayMillis AND timestamp < :endOfDayMillis"
    )
    fun getCountForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int>

    @Query("SELECT * FROM reel_counts ORDER BY timestamp DESC")
    fun getAllCounts(): Flow<List<ReelCountEntity>>

    @Query(
        "SELECT COUNT(*) FROM reel_counts " +
                "WHERE platform = :platform " +
                "AND timestamp >= :startOfDayMillis AND timestamp < :endOfDayMillis"
    )
    fun getCountForDayAndPlatform(
        startOfDayMillis: Long,
        endOfDayMillis: Long,
        platform: String
    ): Flow<Int>

    /**
     * Every platform's count for one range in a single query, instead of
     * one COUNT(*) per platform — this is what Today/Last-7-days use.
     * The timestamp (and platform+timestamp) index keeps this fast as
     * the table grows.
     */
    @Query(
        "SELECT platform, COUNT(*) as count FROM reel_counts " +
                "WHERE timestamp >= :startOfDayMillis AND timestamp < :endOfDayMillis " +
                "GROUP BY platform"
    )
    fun getPlatformCounts(startOfDayMillis: Long, endOfDayMillis: Long): Flow<List<PlatformCountRow>>

    /** Wipes every recorded event. Used by Settings > Reset data. */
    @Query("DELETE FROM reel_counts")
    suspend fun deleteAll()
}