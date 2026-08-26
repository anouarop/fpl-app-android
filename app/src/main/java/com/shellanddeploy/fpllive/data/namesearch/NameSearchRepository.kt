package com.shellanddeploy.fpllive.data.namesearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** A manager/team found by the name-search service. */
data class ManagerMatch(
    val teamId: Int,
    val managerName: String,
    val teamName: String,
    val rank: Int? = null,
)

/**
 * Searches the FPL name → team-ID index. This is an optional feature: the public FPL API cannot
 * search by name, so it depends on a self-hosted companion service (see `backend/`).
 */
interface NameSearchRepository {
    val isConfigured: Boolean

    /** Returns matching managers (empty if none, or if the service is unavailable). */
    suspend fun search(query: String): List<ManagerMatch>

    /** Registers a team so future name searches can find it. */
    suspend fun register(teamId: Int, managerName: String, teamName: String)
}

@Serializable
private data class SearchResponse(val results: List<ManagerMatchDto> = emptyList())

@Serializable
private data class ManagerMatchDto(
    val teamId: Int,
    val managerName: String,
    val teamName: String = "",
    val rank: Int? = null,
)

@Serializable
private data class RegisterRequest(
    val teamId: Int,
    val managerName: String,
    val teamName: String,
)

class HttpNameSearchRepository(private val baseUrl: String) : NameSearchRepository {

    override val isConfigured = baseUrl.isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun search(query: String): List<ManagerMatch> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val request = Request.Builder().url("$baseUrl/search?q=$encoded&limit=20").get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw java.io.IOException("empty body")
            json.decodeFromString<SearchResponse>(body).results
                .map { ManagerMatch(it.teamId, it.managerName, it.teamName, it.rank) }
        }
    }

    override suspend fun register(teamId: Int, managerName: String, teamName: String) {
        withContext(Dispatchers.IO) {
            val payload = json.encodeToString(RegisterRequest(teamId, managerName, teamName))
            val body = payload.toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url("$baseUrl/register").post(body).build()
            runCatching { client.newCall(request).execute().use { } }
        }
    }
}

object NoOpNameSearchRepository : NameSearchRepository {
    override val isConfigured = false
    override suspend fun search(query: String): List<ManagerMatch> = emptyList()
    override suspend fun register(teamId: Int, managerName: String, teamName: String) = Unit
}
