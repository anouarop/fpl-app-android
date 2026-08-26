package com.shellanddeploy.fpllive.data.api

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shellanddeploy.fpllive.data.db.CacheMetaEntity
import com.shellanddeploy.fpllive.data.db.FplDatabase
import com.shellanddeploy.fpllive.domain.model.Bootstrap
import com.shellanddeploy.fpllive.domain.model.Entry
import com.shellanddeploy.fpllive.testutil.FakeFplApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FplRepositoryImplTest {

    private lateinit var db: FplDatabase
    private lateinit var api: FakeFplApi

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FplDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        api = FakeFplApi()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `bootstrap fetches once then serves from cache`() = runBlocking {
        val repo = FplRepositoryImpl(api, db)

        val first = repo.bootstrap()
        assertTrue(first is FetchResult.Success)
        assertFalse((first as FetchResult.Success<Bootstrap>).stale)
        assertEquals(1, api.bootstrapCalls)

        val second = repo.bootstrap()
        assertTrue(second is FetchResult.Success)
        assertFalse((second as FetchResult.Success<Bootstrap>).stale)
        assertEquals(1, api.bootstrapCalls)
    }

    @Test
    fun `bootstrap observe emits persisted data`() = runBlocking {
        val repo = FplRepositoryImpl(api, db)
        repo.bootstrap()
        val observed = repo.observeBootstrap().first()
        assertEquals(1, observed?.players?.size)
        assertEquals("Saka", observed?.players?.first()?.webName)
    }

    @Test
    fun `bootstrap falls back to stale room cache on network failure`() = runBlocking {
        val repo = FplRepositoryImpl(api, db)
        repo.bootstrap()

        db.cacheMetaDao().upsert(CacheMetaEntity("bootstrap", System.currentTimeMillis() - 100_000_000L))
        api.failBootstrap = true

        val fresh = FplRepositoryImpl(api, db)
        val result = fresh.bootstrap()
        assertTrue(result is FetchResult.Success)
        assertTrue((result as FetchResult.Success<Bootstrap>).stale)
        assertEquals("Saka", result.data.players.first().webName)
    }

    @Test
    fun `entry persists and is cached`() = runBlocking {
        val repo = FplRepositoryImpl(api, db)
        val result = repo.entry(123)
        assertTrue(result is FetchResult.Success)
        assertEquals("Sample FC", (result as FetchResult.Success<Entry>).data.name)
        assertEquals("Sample FC", db.entryDao().getById(123)?.name)
    }

    @Test
    fun `clearCache empties room and memory`() = runBlocking {
        val repo = FplRepositoryImpl(api, db)
        repo.bootstrap()
        repo.clearCache()
        assertEquals(null, repo.observeBootstrap().first())
        assertEquals(0, db.playerDao().getAll().size)
    }
}
