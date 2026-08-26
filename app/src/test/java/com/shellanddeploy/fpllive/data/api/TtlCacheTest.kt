package com.shellanddeploy.fpllive.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TtlCacheTest {

    @Test
    fun `returns value before expiry`() {
        val cache = TtlCache()
        cache.put("k", "v", 100_000L)
        assertEquals("v", cache.get<String>("k"))
    }

    @Test
    fun `returns null after expiry`() {
        val cache = TtlCache()
        cache.put("k", "v", -1L)
        assertNull(cache.get<String>("k"))
    }

    @Test
    fun `missing key returns null`() {
        assertNull(TtlCache().get<String>("nope"))
    }

    @Test
    fun `clear removes everything`() {
        val cache = TtlCache()
        cache.put("a", 1, 100_000L)
        cache.put("b", 2, 100_000L)
        cache.clear()
        assertNull(cache.get<Int>("a"))
        assertNull(cache.get<Int>("b"))
    }
}
