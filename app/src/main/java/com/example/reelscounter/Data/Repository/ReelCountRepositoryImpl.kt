package com.example.reelscounter.data.repository

import com.example.reelscounter.data.local.ReelCountDao
import com.example.reelscounter.data.local.ReelCountEntity
import com.example.reelscounter.domain.model.ReelCount
import com.example.reelscounter.domain.repository.ReelCountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Fulfills the ReelCountRepository contract (defined in Domain, which
 * knows nothing about Room) using an actual Room DAO underneath.
 * This is the only place in the app that converts between the Domain
 * model (ReelCount) and the Room entity (ReelCountEntity).
 */
class ReelCountRepositoryImpl @Inject constructor(
    private val dao: ReelCountDao
) : ReelCountRepository {

    override suspend fun insert(reelCount: ReelCount) {
        dao.insert(
            ReelCountEntity(
                platform = reelCount.platform,
                timestamp = reelCount.timestamp
            )
        )
    }

    override fun getTotalCount(): Flow<Int> = dao.getTotalCount()

    override fun getCountForDay(startOfDayMillis: Long, endOfDayMillis: Long): Flow<Int> =
        dao.getCountForDay(startOfDayMillis, endOfDayMillis)

    override fun getCountForDayAndPlatform(
        startOfDayMillis: Long,
        endOfDayMillis: Long,
        platform: String
    ): Flow<Int> = dao.getCountForDayAndPlatform(startOfDayMillis, endOfDayMillis, platform)

    override fun getAllCounts(): Flow<List<ReelCount>> =
        dao.getAllCounts().map { entities ->
            entities.map { entity ->
                ReelCount(
                    id = entity.id,
                    platform = entity.platform,
                    timestamp = entity.timestamp
                )
            }
        }
}