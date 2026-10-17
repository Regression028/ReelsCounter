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
    entities = [ReelCountEntity::class, ReelSessionEntity::class],
    version = 3,
    exportSchema = false
)
abstract class ReelsCounterDatabase : RoomDatabase() {
    abstract fun reelCountDao(): ReelCountDao
    abstract fun reelSessionDao(): ReelSessionDao

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

        /**
         * v2 -> v3: adds the new reel_sessions table (time tracking) and
         * its platform+startTime index. Does not touch reel_counts at
         * all, so existing counts are kept exactly as they are.
         *
         * The CREATE TABLE statement is written by hand to match what
         * Room generates from ReelSessionEntity (column order, nullability,
         * backticked identifiers) so Room's schema validation on open
         * accepts it. This couldn't be verified against a real device from
         * here — if the app fails to open after this update with a
         * schema-mismatch error, this is the statement to fix; no data is
         * at risk, since a failed migration rolls back and just leaves the
         * database at v2 until it's corrected.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `reel_sessions` (" +
                            "`id` INTEGER NOT NULL, " +
                            "`platform` TEXT NOT NULL, " +
                            "`startTime` INTEGER NOT NULL, " +
                            "`endTime` INTEGER, " +
                            "`durationMs` INTEGER, " +
                            "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_reel_sessions_platform_startTime` " +
                            "ON `reel_sessions` (`platform`, `startTime`)"
                )
            }
        }
    }
}
