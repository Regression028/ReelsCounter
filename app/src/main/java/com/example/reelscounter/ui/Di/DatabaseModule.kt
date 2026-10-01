package com.example.reelscounter.ui.Di

import android.content.Context
import androidx.room.Room
import com.example.reelscounter.data.local.ReelCountDao
import com.example.reelscounter.data.local.ReelsCounterDatabase
import com.example.reelscounter.data.repository.ReelCountRepositoryImpl
import com.example.reelscounter.domain.repository.ReelCountRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Tells Hilt how to build the things the Domain/Data layers need:
 * - How to construct the Room database (there should only ever be one
 *   instance for the whole app — @Singleton ensures that).
 * - How to get a DAO out of that database.
 * - Which concrete class (ReelCountRepositoryImpl) to hand over whenever
 *   something asks for the ReelCountRepository interface.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ReelsCounterDatabase {
        return Room.databaseBuilder(
            context,
            ReelsCounterDatabase::class.java,
            "reels_counter.db"
        )
            .addMigrations(ReelsCounterDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideReelCountDao(database: ReelsCounterDatabase): ReelCountDao {
        return database.reelCountDao()
    }
}

/**
 * Separate module (a Kotlin `interface`, not `object`) specifically for
 * @Binds. Hilt requires @Binds to live in an abstract class/interface,
 * which is why this can't just be added to DatabaseModule above (that
 * one is an `object` with @Provides functions instead).
 */
@Module
@InstallIn(SingletonComponent::class)
interface RepositoryModule {

    @Binds
    fun bindReelCountRepository(
        impl: ReelCountRepositoryImpl
    ): ReelCountRepository
}