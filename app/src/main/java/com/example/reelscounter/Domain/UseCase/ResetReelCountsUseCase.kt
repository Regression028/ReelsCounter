package com.example.reelscounter.domain.usecase

import com.example.reelscounter.domain.repository.ReelCountRepository
import javax.inject.Inject

/**
 * Deletes every recorded Reel/Short event. Irreversible — the Settings
 * screen (Phase 4) is expected to confirm with the user before calling this.
 */
class ResetReelCountsUseCase @Inject constructor(
    private val repository: ReelCountRepository
) {
    suspend operator fun invoke() = repository.deleteAllCounts()
}
