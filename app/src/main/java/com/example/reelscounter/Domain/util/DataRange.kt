package com.example.reelscounter.domain.util

import com.example.reelscounter.domain.model.DayStats
import com.example.reelscounter.domain.model.Platform
import java.time.LocalDate
import java.time.ZoneId

/**
 * [start, end) millis for this calendar day, using the device's own
 * timezone — NOT UTC. Room stores plain UTC-epoch millis
 * (System.currentTimeMillis()), so "today" has to be computed in the
 * user's local zone and converted, or a user in e.g. IST would see
 * reels attributed to the wrong day around midnight.
 */
internal fun LocalDate.startOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    this.atStartOfDay(zone).toInstant().toEpochMilli()

internal fun LocalDate.endOfDayMillis(zone: ZoneId = ZoneId.systemDefault()): Long =
    this.plusDays(1).startOfDayMillis(zone)

/**
 * Turns a platform -> count map (as returned by the repository for one
 * day's range) into a [DayStats] for [date]. A platform with no events
 * that day is simply absent from the map, so it's treated as 0 rather
 * than looked up unsafely.
 */
internal fun Map<String, Int>.toDayStats(date: LocalDate): DayStats {
    val instagram = this[Platform.INSTAGRAM] ?: 0
    val youtube = this[Platform.YOUTUBE] ?: 0
    // Sum every platform present, not just the two known ones, so the
    // total never silently disagrees with what's actually in the map.
    val total = this.values.sum()
    return DayStats(
        date = date,
        total = total,
        instagramCount = instagram,
        youtubeCount = youtube
    )
}
