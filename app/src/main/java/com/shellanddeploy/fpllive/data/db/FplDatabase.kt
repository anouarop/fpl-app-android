package com.shellanddeploy.fpllive.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlayerEntity::class,
        TeamEntity::class,
        PositionEntity::class,
        GameweekEntity::class,
        FixtureEntity::class,
        EntryEntity::class,
        GameweekScoreEntity::class,
        CacheMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class FplDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
    abstract fun teamDao(): TeamDao
    abstract fun positionDao(): PositionDao
    abstract fun gameweekDao(): GameweekDao
    abstract fun fixtureDao(): FixtureDao
    abstract fun entryDao(): EntryDao
    abstract fun gameweekScoreDao(): GameweekScoreDao
    abstract fun cacheMetaDao(): CacheMetaDao

    companion object {
        fun build(context: Context): FplDatabase =
            Room.databaseBuilder(context, FplDatabase::class.java, "fpllive.db")
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
