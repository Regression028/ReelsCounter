package com.example.reelscounter.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * What the app needs from session (time-tracking) storage, described
 * without saying how it's implemented — mirrors ReelCountRepository's
 * shape and conventions, but for viewing-session durations instead of
 * individual swipe counts.
 */
interface ReelSessionRepository {

    /** Starts a new session and returns its id, to be passed to [endSession] later. */
    suspend fun startSession(platform: String, startTimeMs: Long): Long

    /** Closes a specific session with its final end time; duration is derived from start/end. */
    suspend fun endSession(sessionId: Long, endTimeMs: Long)

    /**
     * Closes any session left open by a crash/force-kill (normal use always
     * ends a session through [endSession]). Treated as zero duration, since
     * there's no reliable way to know how long it actually lasted. Call once
     * when the detection service (re)starts.
     */
    suspend fun closeOrphanSessions()

    /** Live total session duration (finished sessions only) for [startOfDayMillis, endOfDayMillis). */
    fun getTotalDuration(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Long>

    /**
     * Live per-platform session duration for the same range. A platform with
     * no finished sessions in the range is simply absent from the map.
     */
    fun getPlatformDurations(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Map<String, Long>>

    /** Deletes every recorded session. Used by Settings > Reset data. */
    suspend fun deleteAllSessions()
}
