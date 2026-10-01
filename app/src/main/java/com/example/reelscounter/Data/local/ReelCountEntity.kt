package com.example.reelscounter.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room's on-disk representation of a detected reel/short event.
 * One row per detection — this is what actually lives in the database
 * (as opposed to ReelCount in Domain, which is the plain Kotlin model
 * the rest of the app works with).
 *
 * Indices:
 *  - timestamp: every stats query filters by a timestamp range.
 *  - platform+timestamp: getPlatformCounts (today/last-7-days) filters by
 *    range and groups by platform in the same query.
 * Added in schema version 2 — see ReelsCounterDatabase.MIGRATION_1_2.
 */
@Entity(
    tableName = "reel_counts",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["platform", "timestamp"])
    ]
)
data class ReelCountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val platform: String,
    val timestamp: Long
)
