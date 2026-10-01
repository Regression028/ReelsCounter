package com.example.reelscounter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The actual SQLite database.
 *
 * version = 2: added the timestamp and platform+timestamp indices for
 * Phase 2's stats queries (see MIGRATION_1_2 below). No columns or
 * tables changed, so no data is touched — only indices are created.
 */
@Database(
    entities = [ReelCountEntity::class],
    version = 2,
    exportSchema = false
)
abstract class ReelsCounterDatabase : RoomDatabase() {
    abstract fun reelCountDao(): ReelCountDao

    companion object {
        /**
         * v1 -> v2: adds the two indices ReelCountEntity now declares.
         * Names match Room's default index-naming convention
         * (index_<table>_<col1>_<col2>) exactly, so Room's schema
         * validation on open matches what this migration produces.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_reel_counts_timestamp " +
                            "ON reel_counts(timestamp)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_reel_counts_platform_timestamp " +
                            "ON reel_counts(platform, timestamp)"
                )
            }
        }
    }
}
