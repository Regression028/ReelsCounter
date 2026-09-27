package com.example.reelscounter.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room's on-disk representation of a detected reel/short event.
 * One row per detection — this is what actually lives in the database
 * (as opposed to ReelCount in Domain, which is the plain Kotlin model
 * the rest of the app works with).
 */
@Entity(tableName = "reel_counts")
data class ReelCountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val platform: String,
    val timestamp: Long
)