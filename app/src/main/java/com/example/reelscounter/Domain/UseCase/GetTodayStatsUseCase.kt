package com.example.reelscounter.domain.usecase

import com.example.reelscounter.domain.model.DayStats
import com.example.reelscounter.domain.repository.ReelCountRepository
import com.example.reelscounter.domain.util.endOfDayMillis
import com.example.reelscounter.domain.util.startOfDayMillis
import com.example.reelscounter.domain.util.toDayStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

/**
 * Today's total / Instagram / YouTube counts as one live [DayStats],
 * from a single grouped query rather than three separate COUNT(*) calls.
 * "Today" is today in the device's own timezone (see domain.util.DateRange).
 */
class GetTodayStatsUseCase @Inject constructor(
    private val repository: ReelCountRepository
) {
    operator fun invoke(): Flow<DayStats> {
        val today = LocalDate.now()
        return repository
            .getPlatformCounts(today.startOfDayMillis(), today.endOfDayMillis())
            .map { counts -> counts.toDayStats(today) }
    }
}
