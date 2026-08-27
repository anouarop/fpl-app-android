package com.shellanddeploy.fpllive.data.live

import com.shellanddeploy.fpllive.domain.model.LiveFeed
import com.shellanddeploy.fpllive.domain.model.LiveMatch
import com.shellanddeploy.fpllive.domain.model.LiveMatchEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Fetches the real-time live feed (goals/cards/assists + live scores) from the
 * companion live-events service (see `backend-live/`). Optional: falls back to a
 * no-op when no base URL is configured.
 */
interface LiveRepository {
    val isConfigured: Boolean

    /** Returns the current live feed (empty matches if unavailable). */
    suspend fun live(): LiveFeed
}

@Serializable
private data class LiveFeedDto(
    val event: Int = 0,
    val updated: Long = 0,
    val matches: List<LiveMatchDto> = emptyList(),
)

@Serializable
private data class LiveMatchDto(
    val id: Int = 0,
    val kickoff: String = "",
    val home: LiveTeamDto = LiveTeamDto(),
    val away: LiveTeamDto = LiveTeamDto(),
    val homeScore: Int = 0,
    val awayScore: Int = 0,
    val status: String = "",
    val live: Boolean = false,
    val events: List<LiveEventDto> = emptyList(),
)

@Serializable
private data class LiveTeamDto(
    val id: Int = 0,
    val name: String = "",
    val short: String = "",
)

@Serializable
private data class LiveEventDto(
    val minute: Int = 0,
    val type: String = "",
    val team: String = "",
    val player: LivePlayerDto? = null,
    val assist: LivePlayerDto? = null,
    val detail: String? = null,
)

@Serializable
private data class LivePlayerDto(
    val fplId: Int = 0,
    val name: String = "",
    val points: Int = 0,
)

class HttpLiveRepository(private val baseUrl: String) : LiveRepository {

    override val isConfigured = baseUrl.isNotBlank()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun live(): LiveFeed = withContext(Dispatchers.IO) {
        val request = Request.Builder().url("$baseUrl/live").get().build()
        client.newCall(request).execute().use { resp ->
            if (!resp.isSuccessful) throw java.io.IOException("HTTP ${resp.code}")
            val body = resp.body?.string() ?: throw java.io.IOException("empty body")
            json.decodeFromString<LiveFeedDto>(body).toDomain()
        }
    }
}

private fun LiveFeedDto.toDomain(): LiveFeed = LiveFeed(
    event = event,
    updated = updated,
    matches = matches.map { it.toDomain() },
)

private fun LiveMatchDto.toDomain(): LiveMatch = LiveMatch(
    id = id,
    kickoff = kickoff,
    homeShort = home.short,
    awayShort = away.short,
    homeScore = homeScore,
    awayScore = awayScore,
    status = status,
    live = live,
    events = events.mapNotNull { it.toDomain() },
)

private fun LiveEventDto.toDomain(): LiveMatchEvent? {
    val player = player ?: return null
    return LiveMatchEvent(
        minute = minute,
        type = type,
        isHome = team == "home",
        playerName = player.name,
        playerPoints = player.points,
        assistName = assist?.name,
        assistPoints = assist?.points,
        detail = detail,
    )
}

object NoOpLiveRepository : LiveRepository {
    override val isConfigured = false
    override suspend fun live(): LiveFeed = LiveFeed(0, 0, emptyList())
}
