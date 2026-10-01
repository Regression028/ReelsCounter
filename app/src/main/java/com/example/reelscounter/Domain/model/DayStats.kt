package com.example.reelscounter.domain.model

import java.time.LocalDate

/**
 * Total Reels/Shorts recorded for one calendar day (in the device's own
 * timezone), split by platform. Used for both "today" and each day in
 * the last-7-days view — same shape, different date ranges.
 */
data class DayStats(
    val date: LocalDate,
    val total: Int,
    val instagramCount: Int,
    val youtubeCount: Int
)
