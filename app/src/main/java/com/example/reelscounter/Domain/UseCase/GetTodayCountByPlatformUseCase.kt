package com.example.reelscounter.domain.usecase

import com.example.reelscounter.domain.repository.ReelCountRepository
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject

/**
 * Live count of reels/shorts detected today, filtered to one platform
 * (e.g. just Instagram, or just YouTube). Used to show Reels and Shorts
 * as separate numbers on Home instead of one combined total.
 */
class GetTodayCountByPlatformUseCase @Inject constructor(
    private val repository: ReelCountRepository
) {
    operator fun invoke(platform: String): Flow<Int> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis

        calendar.add(Calendar.DAY_OF_MONTH, 1)
        val endOfDay = calendar.timeInMillis

        return repository.getCountForDayAndPlatform(startOfDay, endOfDay, platform)
    }
}