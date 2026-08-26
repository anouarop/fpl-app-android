package com.shellanddeploy.fpllive.data.api

import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal thread-safe in-memory TTL cache. Used on top of the OkHttp disk cache so that
 * frequently-hit, rapidly-changing data (live stats) is bounded to a short cadence and
 * shared across the app without hammering the FPL API.
 */
class TtlCache {

    private data class Entry(val expiresAt: Long, val value: Any)

    private val map = ConcurrentHashMap<String, Entry>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = map[key] ?: return null
        return if (System.currentTimeMillis() < entry.expiresAt) entry.value as T
        else {
            map.remove(key)
            null
        }
    }

    fun put(key: String, value: Any, ttlMillis: Long) {
        map[key] = Entry(System.currentTimeMillis() + ttlMillis, value)
    }

    fun clear() = map.clear()
}
