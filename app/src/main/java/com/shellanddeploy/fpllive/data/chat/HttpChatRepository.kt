package com.shellanddeploy.fpllive.data.chat

import com.shellanddeploy.fpllive.BuildConfig
import com.shellanddeploy.fpllive.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

interface ChatRepository {
    val isConfigured: Boolean
    suspend fun recent(): List<ChatMessage>
    suspend fun since(iso: String): List<ChatMessage>
    suspend fun send(teamId: Int, teamName: String, text: String)
}

private const val SELECT = "id,team_id,team_name,text,created_at"

@Serializable
private data class ChatMessageDto(
    val id: Long = 0,
    val team_id: Int = 0,
    val team_name: String = "",
    val text: String = "",
    val created_at: String = "",
)

class HttpChatRepository : ChatRepository {
    override val isConfigured =
        BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()

    private val base = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anon = BuildConfig.SUPABASE_ANON_KEY
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    private fun Request.Builder.withAuth() = this
        .addHeader("apikey", anon)
        .addHeader("Authorization", "Bearer $anon")

    override suspend fun recent(): List<ChatMessage> = withContext(Dispatchers.IO) {
        val url = "$base/rest/v1/chat_messages?select=$SELECT&order=created_at.desc&limit=100"
        val req = Request.Builder().url(url).get().withAuth().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            parse(resp.body?.string().orEmpty())
        }
    }

    override suspend fun since(iso: String): List<ChatMessage> = withContext(Dispatchers.IO) {
        val enc = URLEncoder.encode(iso, "UTF-8")
        val url =
            "$base/rest/v1/chat_messages?select=$SELECT&order=created_at.asc&created_at=gt.$enc&limit=100"
        val req = Request.Builder().url(url).get().withAuth().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            parse(resp.body?.string().orEmpty())
        }
    }

    override suspend fun send(teamId: Int, teamName: String, text: String) =
        withContext(Dispatchers.IO) {
            val dto = ChatMessageDto(team_id = teamId, team_name = teamName, text = text)
            val encoded = json.encodeToString(ListSerializer(ChatMessageDto.serializer()), listOf(dto))
            val body = encoded.toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("$base/rest/v1/chat_messages")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .withAuth()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) throw RuntimeException("HTTP ${resp.code}")
            }
        }

    private fun parse(s: String): List<ChatMessage> {
        if (s.isBlank()) return emptyList()
        return json.decodeFromString<List<ChatMessageDto>>(s).map {
            ChatMessage(it.id, it.team_id, it.team_name, it.text, it.created_at)
        }
    }
}
