package com.example.reelscounter.domain.usecase

import com.example.reelscounter.domain.repository.ReelCountRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject

/**
 * Live count of reels/shorts detected today. Home will collect this
 * Flow to show a number that updates in real time as new reels are
 * detected.
 */
class GetTodayCountUseCase @Inject constructor(
    private val repository: ReelCountRepository
) {
    operator fun invoke(): Flow<Int> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return repository.getCountForDay(startOfDay, endOfDay)
    }
}