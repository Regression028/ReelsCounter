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
}