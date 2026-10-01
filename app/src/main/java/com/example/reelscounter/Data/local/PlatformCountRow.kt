package com.example.reelscounter.data.local

/**
 * Room projection for "how many events per platform in this range" —
 * one GROUP BY query instead of one COUNT(*) query per platform.
 * Property names must match the query's column/alias names exactly
 * (platform, count) for Room to bind them automatically.
 */
data class PlatformCountRow(
    val platform: String,
    val count: Int
)
