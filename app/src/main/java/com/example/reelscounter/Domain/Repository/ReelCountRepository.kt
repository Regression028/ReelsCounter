package com.example.reelscounter.domain.repository

import com.example.reelscounter.domain.model.ReelCount
import kotlinx.coroutines.flow.Flow

/**
 * What the app needs from storage, described without saying how it's
 * implemented. The Data layer will provide a Room-backed implementation
 * of this interface later — nothing here knows Room exists.
 */
interface ReelCountRepository {

    /** Saves one detected reel/short event. */
    suspend fun insert(reelCount: ReelCount)

    /** All-time count, live-updating as new events are inserted. */
    fun getTotalCount(): Flow<Int>

    /** Count for a single calendar day, live-updating. */
    fun getCountForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int>

    /** Count for a single calendar day, filtered to one platform, live-updating. */
    fun getCountForDayAndPlatform(
        startOfDayMillis: Long,
        endOfDayMillis: Long,
        platform: String
    ): Flow<Int>

    /** All recorded events, most recent first — useful for Statistics later. */
    fun getAllCounts(): Flow<List<ReelCount>>
}