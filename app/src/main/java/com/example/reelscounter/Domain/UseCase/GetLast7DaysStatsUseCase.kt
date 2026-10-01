package com.example.reelscounter.domain.usecase

import com.example.reelscounter.domain.model.DayStats
import com.example.reelscounter.domain.repository.ReelCountRepository
import com.example.reelscounter.domain.util.endOfDayMillis
import com.example.reelscounter.domain.util.startOfDayMillis
import com.example.reelscounter.domain.util.toDayStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * [DayStats] for each of the last 7 calendar days (device-local),
 * oldest first, today last. One query per day, same as the existing
 * GetWeeklyCountsUseCase, just with a platform breakdown added.
 */
class GetLast7DaysStatsUseCase @Inject constructor(
    private val repository: ReelCountRepository
) {
    operator fun invoke(): Flow<List<DayStats>> {
        val today = LocalDate.now()
        val days = (6 downTo 0).map { daysAgo -> today.minusDays(daysAgo.toLong()) }

        val dayFlows: List<Flow<DayStats>> = days.map { date ->
            repository
                .getPlatformCounts(date.startOfDayMillis(), date.endOfDayMillis())
                .map { counts -> counts.toDayStats(date) }
        }

        return combine(dayFlows) { it.toList() }
    }
}
