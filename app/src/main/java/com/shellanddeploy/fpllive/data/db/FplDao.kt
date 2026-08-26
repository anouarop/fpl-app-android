package com.shellanddeploy.fpllive.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>)

    @Query("DELETE FROM players")
    suspend fun clear()

    @Query("SELECT * FROM players ORDER BY totalPoints DESC")
    fun observeAll(): Flow<List<PlayerEntity>>

    @Query("SELECT * FROM players")
    suspend fun getAll(): List<PlayerEntity>
}

@Dao
interface TeamDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(teams: List<TeamEntity>)

    @Query("DELETE FROM teams")
    suspend fun clear()

    @Query("SELECT * FROM teams")
    fun observeAll(): Flow<List<TeamEntity>>

    @Query("SELECT * FROM teams")
    suspend fun getAll(): List<TeamEntity>
}

@Dao
interface PositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(positions: List<PositionEntity>)

    @Query("DELETE FROM positions")
    suspend fun clear()

    @Query("SELECT * FROM positions ORDER BY id")
    fun observeAll(): Flow<List<PositionEntity>>

    @Query("SELECT * FROM positions ORDER BY id")
    suspend fun getAll(): List<PositionEntity>
}

@Dao
interface GameweekDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(gameweeks: List<GameweekEntity>)

    @Query("DELETE FROM gameweeks")
    suspend fun clear()

    @Query("SELECT * FROM gameweeks ORDER BY id")
    fun observeAll(): Flow<List<GameweekEntity>>

    @Query("SELECT * FROM gameweeks ORDER BY id")
    suspend fun getAll(): List<GameweekEntity>
}

@Dao
interface FixtureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(fixtures: List<FixtureEntity>)

    @Query("DELETE FROM fixtures WHERE event = :eventId")
    suspend fun clearForEvent(eventId: Int)

    @Query("DELETE FROM fixtures")
    suspend fun clearAll()

    @Query("SELECT * FROM fixtures WHERE event = :eventId ORDER BY kickoffTime")
    fun observeForEvent(eventId: Int): Flow<List<FixtureEntity>>

    @Query("SELECT * FROM fixtures WHERE event = :eventId ORDER BY kickoffTime")
    suspend fun getForEvent(eventId: Int): List<FixtureEntity>
}

@Dao
interface EntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: EntryEntity)

    @Query("SELECT * FROM entries WHERE id = :id")
    fun observeById(id: Int): Flow<EntryEntity?>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Int): EntryEntity?

    @Query("DELETE FROM entries")
    suspend fun clearAll()
}

@Dao
interface GameweekScoreDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<GameweekScoreEntity>)

    @Query("DELETE FROM gameweek_scores WHERE entryId = :entryId")
    suspend fun clearForEntry(entryId: Int)

    @Query("SELECT * FROM gameweek_scores WHERE entryId = :entryId ORDER BY event")
    fun observeForEntry(entryId: Int): Flow<List<GameweekScoreEntity>>

    @Query("SELECT * FROM gameweek_scores WHERE entryId = :entryId ORDER BY event")
    suspend fun getForEntry(entryId: Int): List<GameweekScoreEntity>

    @Query("DELETE FROM gameweek_scores")
    suspend fun clearAll()
}

@Dao
interface CacheMetaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: CacheMetaEntity)

    @Query("SELECT updatedAt FROM cache_meta WHERE `key` = :key")
    suspend fun get(key: String): Long?

    @Query("DELETE FROM cache_meta")
    suspend fun clear()
}
