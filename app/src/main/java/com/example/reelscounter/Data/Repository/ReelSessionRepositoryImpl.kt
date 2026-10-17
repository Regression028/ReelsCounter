package com.example.reelscounter.data.repository

import com.example.reelscounter.data.local.ReelSessionDao
import com.example.reelscounter.data.local.ReelSessionEntity
import com.example.reelscounter.domain.repository.ReelSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Fulfills the ReelSessionRepository contract using Room underneath —
 * same role as ReelCountRepositoryImpl, but for sessions.
 */
class ReelSessionRepositoryImpl @Inject constructor(
    private val dao: ReelSessionDao
) : ReelSessionRepository {

    override suspend fun startSession(platform: String, startTimeMs: Long): Long =
        dao.insert(ReelSessionEntity(platform = platform, startTime = startTimeMs))

    override suspend fun endSession(sessionId: Long, endTimeMs: Long) {
        // Duration is derived from the row's own startTime rather than
        // trusted from the caller, so it can never drift from what's
        // actually stored. If the session id is unknown (e.g. already
        // ended, or lost across a service restart), there's nothing to
        // update — this is a no-op rather than a crash.
        val startTime = dao.getStartTime(sessionId) ?: return
        val duration = (endTimeMs - startTime).coerceAtLeast(0)
        dao.endSession(sessionId, endTimeMs, duration)
    }

    override suspend fun closeOrphanSessions() {
        dao.closeOrphanSessions()
    }

    override fun getTotalDuration(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Long> =
        dao.getTotalDuration(startOfDayMillis, endOfDayMillis)

    override fun getPlatformDurations(
        startOfDayMillis: Long,
        endOfDayMillis: Long
    ): Flow<Map<String, Long>> =
        dao.getPlatformDurations(startOfDayMillis, endOfDayMillis).map { rows ->
            rows.associate { it.platform to it.totalMs }
        }

    override suspend fun deleteAllSessions() {
        dao.deleteAll()
    }
}
