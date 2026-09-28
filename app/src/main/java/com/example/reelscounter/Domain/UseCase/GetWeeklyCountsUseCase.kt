package com.example.reelscounter.domain.usecase

import com.example.reelscounter.domain.repository.ReelCountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject

/**
 * The last 7 days of daily totals, oldest first, today last — powers a
 * simple weekly trend chart on Home. Reuses the same getCountForDay
 * query as GetTodayCountUseCase, just once per day in the window.
 */
class GetWeeklyCountsUseCase @Inject constructor(
    private val repository: ReelCountRepository
) {
    operator fun invoke(): Flow<List<Int>> {
        val dayFlows = (6 downTo 0).map { daysAgo ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, -daysAgo)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis

            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val endOfDay = calendar.timeInMillis

            repository.getCountForDay(startOfDay, endOfDay)
        }
        return combine(dayFlows) { counts -> counts.toList() }
    }
}