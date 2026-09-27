package com.example.reelscounter.domain.usecase

import com.example.reelscounter.domain.model.ReelCount
import com.example.reelscounter.domain.repository.ReelCountRepository
import javax.inject.Inject

/**
 * Records one detected reel/short scroll. This is what
 * ReelDetectionService will call instead of just logging to Logcat.
 */
class IncrementCounterUseCase @Inject constructor(
    private val repository: ReelCountRepository
) {
    suspend operator fun invoke(platform: String) {
        repository.insert(
            ReelCount(
                platform = platform,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}