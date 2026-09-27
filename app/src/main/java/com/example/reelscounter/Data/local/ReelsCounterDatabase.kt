package com.example.reelscounter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The actual SQLite database. version = 1 since this is the first
 * schema — you'll bump this and add a migration if you ever change
 * ReelCountEntity's shape later.
 */
@Database(
    entities = [ReelCountEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ReelsCounterDatabase : RoomDatabase() {
    abstract fun reelCountDao(): ReelCountDao
}