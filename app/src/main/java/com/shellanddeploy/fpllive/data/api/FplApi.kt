package com.shellanddeploy.fpllive.data.api

import android.content.Context
import com.shellanddeploy.fpllive.data.model.BootstrapDto
import com.shellanddeploy.fpllive.data.model.ElementSummaryDto
import com.shellanddeploy.fpllive.data.model.EntryDto
import com.shellanddeploy.fpllive.data.model.FixtureDto
import com.shellanddeploy.fpllive.data.model.HistoryDto
import com.shellanddeploy.fpllive.data.model.LiveEventDto
import com.shellanddeploy.fpllive.data.model.LeagueStandingsDto
import com.shellanddeploy.fpllive.data.model.PicksDto
import com.shellanddeploy.fpllive.data.model.TransferDto
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * All FPL endpoints. Public, unauthenticated — the FPL API only asks that requests
 * carry a normal browser User-Agent header (we set it in [FplClient]).
 */
interface FplApi {

    @GET("bootstrap-static/")
    suspend fun bootstrap(): BootstrapDto

    @GET("event/{id}/live")
    suspend fun eventLive(@Path("id") id: Int): LiveEventDto

    @GET("fixtures/")
    suspend fun fixtures(@Query("event") event: Int? = null): List<FixtureDto>

    @GET("entry/{id}/")
    suspend fun entry(@Path("id") id: Int): EntryDto

    @GET("entry/{id}/event/{event}/picks")
    suspend fun picks(@Path("id") id: Int, @Path("event") event: Int): PicksDto

    @GET("entry/{id}/history/")
    suspend fun history(@Path("id") id: Int): HistoryDto

    @GET("element-summary/{id}/")
    suspend fun elementSummary(@Path("id") id: Int): ElementSummaryDto

    @GET("entry/{id}/transfers/")
    suspend fun transfers(@Path("id") id: Int): List<TransferDto>

    @GET("leagues-classic/{id}/standings/")
    suspend fun leaguesClassicStandings(@Path("id") id: Int): LeagueStandingsDto

    companion object {
        const val BASE_URL = "https://fantasy.premierleague.com/api/"
    }
}

/**
 * Builds the single OkHttp + Retrofit stack used by the whole app.
 *
 * - Every request carries a normal browser User-Agent (FPL rejects requests without one).
 * - A 10 MB OkHttp disk cache serves bootstrap/fixtures/entry/element-summary for 10 minutes.
 * - The live endpoint (`/event/{id}/live`) is never disk-cached (no-store) so the app only
 *   ever shows fresh live data (the in-memory TTL layer governs its re-fetch cadence).
 */
object FplClient {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; SM-A057F) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36"

    private const val DISK_CACHE_SIZE = 10L * 1024 * 1024
    private const val CACHEABLE_MAX_AGE_SECONDS = 600

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        namingStrategy = JsonNamingStrategy.SnakeCase
    }

    fun create(context: Context): FplApi {
        val cacheDir = File(context.cacheDir, "fpl_http_cache")
        val cache = Cache(cacheDir, DISK_CACHE_SIZE)

        val client = OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val path = request.url.encodedPath
                val isLive = path.contains("/event/") && path.endsWith("/live")

                val forced = if (isLive) {
                    request.newBuilder().header("Cache-Control", "no-store").build()
                } else {
                    request.newBuilder()
                        .header("Cache-Control", "max-age=$CACHEABLE_MAX_AGE_SECONDS")
                        .build()
                }

                val response = chain.proceed(forced)
                if (isLive) {
                    response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "no-store")
                        .build()
                } else {
                    response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "public, max-age=$CACHEABLE_MAX_AGE_SECONDS")
                        .build()
                }
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(FplApi.BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(FplApi::class.java)
    }
}
