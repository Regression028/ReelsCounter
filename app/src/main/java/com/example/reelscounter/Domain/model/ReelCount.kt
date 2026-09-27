package com.example.reelscounter.domain.model

/**
 * A single detected reel/short scroll event.
 *
 * One row is created per detection (not one running total) so that
 * Statistics can later break counts down by day, by platform, etc.
 */
data class ReelCount(
    val id: Long = 0, // 0 means "not yet saved" — Room will assign a real id
    val platform: String, // e.g. "com.instagram.android" or "com.google.android.youtube"
    val timestamp: Long // System.currentTimeMillis() at time of detection
)