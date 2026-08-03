package com.theveloper.pixelplay.data.youtube

import android.content.Context
import com.theveloper.pixelplay.data.database.MusicDao
import com.theveloper.pixelplay.data.database.SongEntity
import com.theveloper.pixelplay.data.database.SourceType
import com.theveloper.pixelplay.data.model.ArtistRef
import com.theveloper.pixelplay.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.absoluteValue

@Singleton
class YouTubeMusicRepository @Inject constructor(
    private val downloader: YouTubeDownloaderImpl,
    private val musicDao: MusicDao,
    private val okHttpClient: OkHttpClient,
    @ApplicationContext private val context: Context
) {
    private companion object {
        private const val YOUTUBE_SONG_ID_OFFSET = 7_000_000_000_000L
        private const val YOUTUBE_PARENT_DIRECTORY = "/Cloud/YouTube"
        private const val YOUTUBE_GENRE = "YouTube Music"

        // InnerTube API clients (ANDROID_SDKLESS, IOS, TVHTML5, WEB_REMIX, ANDROID_VR, WEB_EMBEDDED_PLAYER)
        private val INNERTUBE_CLIENTS = listOf(
            InnerTubeClient(
                name = "ANDROID_SDKLESS",
                apiKey = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w",
                endpoint = "https://www.youtube.com/youtubei/v1/player",
                userAgent = "com.google.android.youtube/20.10.38 (Linux; U; Android 11) gzip",
                clientNameHeader = "1",
                clientVersionHeader = "20.10.38",
                isEmbedded = false,
                clientBody = """
                    "clientName": "ANDROID",
                    "clientVersion": "20.10.38",
                    "platform": "MOBILE",
                    "osName": "Android",
                    "osVersion": "11",
                    "hl": "en",
                    "gl": "US"
                """.trimIndent()
            ),
            InnerTubeClient(
                name = "IOS",
                apiKey = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc",
                endpoint = "https://www.youtube.com/youtubei/v1/player",
                userAgent = "com.google.ios.youtube/20.10.4 (iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
                clientNameHeader = "5",
                clientVersionHeader = "20.10.4",
                isEmbedded = false,
                clientBody = """
                    "clientName": "IOS",
                    "clientVersion": "20.10.4",
                    "deviceMake": "Apple",
                    "deviceModel": "iPhone16,2",
                    "platform": "MOBILE",
                    "osName": "IOS",
                    "osVersion": "18.1.0.22B83",
                    "hl": "en",
                    "gl": "US"
                """.trimIndent()
            ),
            InnerTubeClient(
                name = "TVHTML5",
                apiKey = "AIzaSyAO_FJ2SlqU8Q4sYse5xHxD262mP-LWyVA",
                endpoint = "https://www.youtube.com/youtubei/v1/player",
                userAgent = "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/Version,gzip(gfe)",
                clientNameHeader = "7",
                clientVersionHeader = "7.20251105.10.00",
                isEmbedded = false,
                clientBody = """
                    "clientName": "TVHTML5",
                    "clientVersion": "7.20251105.10.00",
                    "platform": "DESKTOP",
                    "hl": "en",
                    "gl": "US"
                """.trimIndent()
            ),
            InnerTubeClient(
                name = "WEB_REMIX",
                apiKey = "AIzaSyAO_FJ2SlqU8Q4sYse5xHxD262mP-LWyVA",
                endpoint = "https://music.youtube.com/youtubei/v1/player",
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                clientNameHeader = "67",
                clientVersionHeader = "1.20260114.03.00",
                isEmbedded = false,
                clientBody = """
                    "clientName": "WEB_REMIX",
                    "clientVersion": "1.20260114.03.00",
                    "hl": "en",
                    "gl": "US"
                """.trimIndent()
            ),
            InnerTubeClient(
                name = "ANDROID_VR",
                apiKey = "AIzaSyAO_FJ2SlqU8Q4sYse5xHxD262mP-LWyVA",
                endpoint = "https://www.youtube.com/youtubei/v1/player",
                userAgent = "com.google.android.apps.youtube.vr.oculus/1.56.21 (Linux; U; Android 12)",
                clientNameHeader = "28",
                clientVersionHeader = "1.56.21",
                isEmbedded = false,
                clientBody = """
                    "clientName": "ANDROID_VR",
                    "clientVersion": "1.56.21",
                    "deviceMake": "Oculus",
                    "deviceModel": "Quest 3",
                    "platform": "MOBILE",
                    "hl": "en",
                    "gl": "US"
                """.trimIndent()
            ),
            InnerTubeClient(
                name = "WEB_EMBEDDED_PLAYER",
                apiKey = "AIzaSyAO_FJ2SlqU8Q4sYse5xHxD262mP-LWyVA",
                endpoint = "https://www.youtube.com/youtubei/v1/player",
                userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
                clientNameHeader = "56",
                clientVersionHeader = "1.20240101.01.00",
                isEmbedded = true,
                clientBody = """
                    "clientName": "WEB_EMBEDDED_PLAYER",
                    "clientVersion": "1.20240101.01.00",
                    "platform": "DESKTOP",
                    "hl": "en",
                    "gl": "US"
                """.trimIndent()
            )
        )
    }

    private data class InnerTubeClient(
        val name: String,
        val apiKey: String,
        val endpoint: String,
        val userAgent: String,
        val clientNameHeader: String,
        val clientVersionHeader: String,
        val isEmbedded: Boolean,
        val clientBody: String
    )

    @Volatile
    private var cachedVisitorData: String? = null

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    init {
        try {
            NewPipe.init(downloader)
            _isInitialized.value = true
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize NewPipeExtractor")
        }
    }

    /**
     * Search YouTube / YouTube Music for tracks matching [query].
     * Uses NewPipe extractor (reliable for search).
     */
    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val searchInfo = SearchInfo.getInfo(
                ServiceList.YouTube,
                ServiceList.YouTube.searchQHFactory.fromQuery(query)
            )

            val songs = mutableListOf<Song>()
            for (item in searchInfo.relatedItems) {
                if (item is StreamInfoItem) {
                    val song = mapStreamInfoToSong(item)
                    songs.add(song)
                }
            }
            songs
        } catch (e: Exception) {
            Timber.e(e, "YouTube search failed for query: $query")
            emptyList()
        }
    }

    /**
     * Resolve the direct audio stream URL for a given YouTube [videoId].
     *
     * Strategy: InnerTube API first (reliable, no signature decryption needed),
     * then NewPipe extractor as fallback.
     */
    suspend fun getAudioStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        android.util.Log.d("YouTubePlayback", "=== STEP 1: getAudioStreamUrl called for videoId: $videoId ===")
        // Primary: Direct InnerTube API — this is what InnerTune, ViMusic, etc. use
        val innerTubeResult = getAudioStreamViaInnerTube(videoId)
        if (innerTubeResult.isSuccess) {
            val url = innerTubeResult.getOrThrow()
            android.util.Log.d("YouTubePlayback", "=== SUCCESS: InnerTube extracted URL for $videoId -> $url ===")
            return@withContext innerTubeResult
        }
        android.util.Log.w("YouTubePlayback", "InnerTube extraction failed for $videoId: ${innerTubeResult.exceptionOrNull()?.message}")

        // 2. Fallback to Cobalt API instances (api.cobalt.tools)
        android.util.Log.d("YouTubePlayback", "Trying Cobalt API fallback for videoId: $videoId")
        val cobaltResult = getAudioStreamViaCobalt(videoId)
        if (cobaltResult.isSuccess) {
            val url = cobaltResult.getOrThrow()
            android.util.Log.d("YouTubePlayback", "=== SUCCESS: Cobalt extracted URL for $videoId -> $url ===")
            return@withContext cobaltResult
        }
        android.util.Log.w("YouTubePlayback", "Cobalt extraction failed for $videoId: ${cobaltResult.exceptionOrNull()?.message}")

        // 3. Fallback: NewPipe extractor
        if (_isInitialized.value) {
            android.util.Log.d("YouTubePlayback", "=== STEP 1b: Trying NewPipe fallback for videoId: $videoId ===")
            val newPipeResult = getAudioStreamViaNewPipe(videoId)
            if (newPipeResult.isSuccess) {
                val url = newPipeResult.getOrThrow()
                android.util.Log.d("YouTubePlayback", "=== SUCCESS: NewPipe extracted URL for $videoId -> $url ===")
                return@withContext newPipeResult
            }
            android.util.Log.w("YouTubePlayback", "NewPipe extraction failed for $videoId: ${newPipeResult.exceptionOrNull()?.message}")

            // 3. Fallback to Piped API instances
            android.util.Log.d("YouTubePlayback", "Trying Piped API fallback for videoId: $videoId")
            val pipedResult = getAudioStreamViaPiped(videoId)
            if (pipedResult.isSuccess) {
                val url = pipedResult.getOrThrow()
                android.util.Log.d("YouTubePlayback", "=== SUCCESS: Piped extracted URL for $videoId -> $url ===")
                return@withContext pipedResult
            }
            android.util.Log.w("YouTubePlayback", "Piped extraction failed for $videoId: ${pipedResult.exceptionOrNull()?.message}")

            // 4. Fallback to Invidious API instances
            android.util.Log.d("YouTubePlayback", "Trying Invidious API fallback for videoId: $videoId")
            val invidiousResult = getAudioStreamViaInvidious(videoId)
            if (invidiousResult.isSuccess) {
                val url = invidiousResult.getOrThrow()
                android.util.Log.d("YouTubePlayback", "=== SUCCESS: Invidious extracted URL for $videoId -> $url ===")
                return@withContext invidiousResult
            }
            android.util.Log.w("YouTubePlayback", "Invidious extraction failed for $videoId: ${invidiousResult.exceptionOrNull()?.message}")
        }

        val err = innerTubeResult.exceptionOrNull()
            ?: IllegalStateException("All extraction methods (InnerTube, NewPipe, Piped, Invidious) failed for videoId: $videoId")
        android.util.Log.e("YouTubePlayback", "=== FAILURE: All stream extraction methods failed for videoId: $videoId ===", err)
        Result.failure(err)
    }

    /**
     * Cobalt API fallback — high-availability media extractor API.
     */
    private fun getAudioStreamViaCobalt(videoId: String): Result<String> {
        val cobaltInstances = listOf(
            "https://api.cobalt.tools/",
            "https://cobalt.api.sc7.io/"
        )
        val bodyJson = """
            {
                "url": "https://www.youtube.com/watch?v=$videoId"
            }
        """.trimIndent()

        for (apiUrl in cobaltInstances) {
            try {
                android.util.Log.d("YouTubePlayback", "Trying Cobalt API instance: $apiUrl for videoId: $videoId")
                val request = Request.Builder()
                    .url(apiUrl)
                    .post(bodyJson.toRequestBody("application/json".toMediaType()))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "PixelPlayer/1.0")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    android.util.Log.w("YouTubePlayback", "Cobalt instance $apiUrl HTTP ${response.code}")
                    continue
                }
                val responseBody = response.body.string()
                val json = JSONObject(responseBody)
                val streamUrl = json.optString("url", "").ifBlank { json.optString("picker", "") }
                if (streamUrl.isNotBlank()) {
                    android.util.Log.d("YouTubePlayback", "Cobalt API SUCCESS via $apiUrl: ${streamUrl.take(80)}...")
                    return Result.success(streamUrl)
                }
            } catch (e: Exception) {
                android.util.Log.w("YouTubePlayback", "Cobalt instance $apiUrl exception: ${e.message}")
            }
        }
        return Result.failure(IllegalStateException("All Cobalt instances failed for $videoId"))
    }

    /**
     * Invidious API fallback — queries reliable public Invidious instances for audio streams.
     */
    private fun getAudioStreamViaInvidious(videoId: String): Result<String> {
        val invidiousInstances = listOf(
            "https://invidious.drgns.space/api/v1/videos/$videoId",
            "https://inv.us.projectsegfau.lt/api/v1/videos/$videoId",
            "https://invidious.privacydev.net/api/v1/videos/$videoId"
        )
        for (apiUrl in invidiousInstances) {
            try {
                android.util.Log.d("YouTubePlayback", "Trying Invidious API instance: $apiUrl")
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    android.util.Log.w("YouTubePlayback", "Invidious instance $apiUrl HTTP ${response.code}")
                    continue
                }
                val responseBody = response.body.string()
                val json = JSONObject(responseBody)
                val adaptiveFormats = json.optJSONArray("adaptiveFormats") ?: continue

                var bestUrl: String? = null
                var maxBitrate = -1
                for (i in 0 until adaptiveFormats.length()) {
                    val format = adaptiveFormats.getJSONObject(i)
                    val mimeType = format.optString("type", "")
                    if (!mimeType.startsWith("audio/")) continue
                    val url = format.optString("url", "")
                    if (url.isBlank()) continue
                    val bitrate = format.optInt("bitrate", 0)
                    val isOpus = mimeType.contains("opus", ignoreCase = true)
                    val score = if (isOpus) bitrate + 100_000 else bitrate

                    if (score > maxBitrate) {
                        maxBitrate = score
                        bestUrl = url
                    }
                }
                if (bestUrl != null) {
                    android.util.Log.d("YouTubePlayback", "Invidious API SUCCESS via $apiUrl: selected stream url=${bestUrl.take(80)}...")
                    return Result.success(bestUrl)
                }
            } catch (e: Exception) {
                android.util.Log.w("YouTubePlayback", "Invidious instance $apiUrl exception: ${e.message}")
            }
        }
        return Result.failure(IllegalStateException("All Invidious instances failed for $videoId"))
    }

    /**
     * Piped API fallback — queries reliable public Piped instances for audio streams.
     */
    private fun getAudioStreamViaPiped(videoId: String): Result<String> {
        val pipedInstances = listOf(
            "https://pipedapi.drgns.space/streams/$videoId",
            "https://pipedapi.colossal.systems/streams/$videoId",
            "https://pipedapi.adminforge.de/streams/$videoId"
        )
        for (apiUrl in pipedInstances) {
            try {
                android.util.Log.d("YouTubePlayback", "Trying Piped API instance: $apiUrl")
                val request = Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .build()
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    android.util.Log.w("YouTubePlayback", "Piped instance $apiUrl HTTP ${response.code}")
                    continue
                }
                val responseBody = response.body.string()
                val json = JSONObject(responseBody)
                val audioStreams = json.optJSONArray("audioStreams") ?: continue

                var bestUrl: String? = null
                var maxBitrate = -1
                for (i in 0 until audioStreams.length()) {
                    val stream = audioStreams.getJSONObject(i)
                    val streamUrl = stream.optString("url", "")
                    if (streamUrl.isBlank()) continue
                    val bitrate = stream.optInt("bitrate", 0)
                    val mimeType = stream.optString("mimeType", "")
                    val isOpus = mimeType.contains("opus", ignoreCase = true)
                    val score = if (isOpus) bitrate + 100_000 else bitrate

                    if (score > maxBitrate) {
                        maxBitrate = score
                        bestUrl = streamUrl
                    }
                }
                if (bestUrl != null) {
                    android.util.Log.d("YouTubePlayback", "Piped API SUCCESS via $apiUrl: selected stream url=${bestUrl.take(80)}...")
                    return Result.success(bestUrl)
                }
            } catch (e: Exception) {
                android.util.Log.w("YouTubePlayback", "Piped instance $apiUrl exception: ${e.message}")
            }
        }
        return Result.failure(IllegalStateException("All Piped instances failed for $videoId"))
    }

    /**
     * Direct InnerTube API call — tries ANDROID_VR, WEB_REMIX, TVHTML5, ANDROID_MUSIC, IOS clients in order.
     * Returns the highest-quality audio stream URL.
     */
    private fun getAudioStreamViaInnerTube(videoId: String): Result<String> {
        for (client in INNERTUBE_CLIENTS) {
            android.util.Log.d("YouTubePlayback", "Trying InnerTube client: ${client.name} for videoId: $videoId")
            try {
                val result = callInnerTubePlayer(videoId, client)
                if (result.isSuccess) {
                    android.util.Log.d("YouTubePlayback", "InnerTube ${client.name} SUCCESS for $videoId")
                    return result
                } else {
                    android.util.Log.w("YouTubePlayback", "InnerTube ${client.name} returned failure: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.w("YouTubePlayback", "InnerTube ${client.name} exception for $videoId: ${e.message}")
            }
        }
        return Result.failure(IllegalStateException("All InnerTube clients failed for videoId: $videoId"))
    }

    private fun fetchVisitorData(): String? {
        cachedVisitorData?.let { return it }
        try {
            val body = """
                {
                    "context": {
                        "client": {
                            "clientName": "WEB",
                            "clientVersion": "2.20260114.00.00"
                        }
                    }
                }
            """.trimIndent()
            val request = Request.Builder()
                .url("https://www.youtube.com/youtubei/v1/visitor_id?prettyPrint=false&key=AIzaSyAO_FJ2SlqU8Q4sYse5xHxD262mP-LWyVA")
                .post(body.toRequestBody("application/json".toMediaType()))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("X-YouTube-Client-Name", "1")
                .header("X-YouTube-Client-Version", "2.20260114.00.00")
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body.string()
                val json = JSONObject(responseBody)
                val rawVisitorData = json.optJSONObject("responseContext")?.optString("visitorData")
                if (!rawVisitorData.isNullOrBlank()) {
                    val visitorData = java.net.URLDecoder.decode(rawVisitorData, "UTF-8")
                    cachedVisitorData = visitorData
                    android.util.Log.d("YouTubePlayback", "Fetched visitorData: $visitorData")
                    return visitorData
                }
            } else {
                android.util.Log.w("YouTubePlayback", "fetchVisitorData HTTP failure: ${response.code}")
            }
        } catch (e: Exception) {
            android.util.Log.w("YouTubePlayback", "fetchVisitorData exception: ${e.message}")
        }
        return null
    }

    private fun callInnerTubePlayer(videoId: String, client: InnerTubeClient): Result<String> {
        val thirdPartyPart = if (client.isEmbedded) {
            """, "thirdParty": { "embedUrl": "https://www.youtube.com/watch?v=$videoId" }"""
        } else ""

        val visitorData = fetchVisitorData()
        val visitorDataJson = if (!visitorData.isNullOrBlank()) {
            """, "visitorData": "$visitorData""""
        } else ""

        val body = """
            {
                "context": {
                    "client": {
                        ${client.clientBody}
                        $visitorDataJson
                    }
                },
                "videoId": "$videoId",
                "contentCheckOk": true,
                "racyCheckOk": true
                $thirdPartyPart
            }
        """.trimIndent()

        val requestBuilder = Request.Builder()
            .url("${client.endpoint}?prettyPrint=false&key=${client.apiKey}")
            .post(body.toRequestBody("application/json".toMediaType()))
            .header("User-Agent", client.userAgent)
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("X-YouTube-Client-Name", client.clientNameHeader)
            .header("X-YouTube-Client-Version", client.clientVersionHeader)

        if (!visitorData.isNullOrBlank()) {
            requestBuilder.header("X-Goog-Visitor-Id", visitorData)
        }

        if (client.name.startsWith("WEB") || client.isEmbedded) {
            requestBuilder.header("Origin", "https://music.youtube.com")
            requestBuilder.header("Referer", "https://music.youtube.com/")
        }

        val request = requestBuilder.build()

        android.util.Log.d("YouTubePlayback", "Posting to InnerTube ${client.name}: ${client.endpoint}")
        val response = okHttpClient.newCall(request).execute()
        val responseBody = response.body.string()

        if (!response.isSuccessful) {
            android.util.Log.w("YouTubePlayback", "InnerTube ${client.name} HTTP failure: ${response.code}, body: $responseBody")
            return Result.failure(IllegalStateException("InnerTube ${client.name} HTTP ${response.code}"))
        }

        val json = JSONObject(responseBody)

        // Check playability status
        val playabilityStatus = json.optJSONObject("playabilityStatus")
        val status = playabilityStatus?.optString("status", "")
        android.util.Log.d("YouTubePlayback", "InnerTube ${client.name} playability status: $status")
        if (status != "OK") {
            val reason = playabilityStatus?.optString("reason", "Unknown")
            android.util.Log.w("YouTubePlayback", "Video not playable (${client.name}): status=$status, reason=$reason")
            return Result.failure(IllegalStateException("Video not playable ($status): $reason"))
        }

        // Extract adaptive audio streams
        val streamingData = json.optJSONObject("streamingData")
        if (streamingData == null) {
            android.util.Log.w("YouTubePlayback", "No streamingData in response for ${client.name}")
            return Result.failure(IllegalStateException("No streamingData in response"))
        }

        val formatsList = mutableListOf<JSONObject>()
        streamingData.optJSONArray("adaptiveFormats")?.let { array ->
            for (i in 0 until array.length()) formatsList.add(array.getJSONObject(i))
        }
        streamingData.optJSONArray("formats")?.let { array ->
            for (i in 0 until array.length()) formatsList.add(array.getJSONObject(i))
        }

        if (formatsList.isEmpty()) {
            android.util.Log.w("YouTubePlayback", "No formats in response for ${client.name}")
            return Result.failure(IllegalStateException("No formats in response"))
        }

        android.util.Log.d("YouTubePlayback", "InnerTube ${client.name} returned ${formatsList.size} formats")

        // Find best audio stream candidate — prefer Opus, then highest bitrate, and verify non-403
        val candidateFormats = mutableListOf<Pair<Int, String>>()

        for (format in formatsList) {
            val mimeType = format.optString("mimeType", "")
            val isAudioOnly = mimeType.startsWith("audio/")
            val isCombinedVideo = mimeType.startsWith("video/")

            if (!isAudioOnly && !isCombinedVideo) continue

            // Direct URL is required; signatureCipher requires JS deciphering so skip un-deciphered raw url
            val url = format.optString("url", "")
            if (url.isBlank()) continue

            val bitrate = format.optInt("bitrate", 0)
            val isOpus = mimeType.contains("opus", ignoreCase = true)
            var score = if (isOpus) bitrate + 100_000 else bitrate
            if (isAudioOnly) score += 50_000 // Boost pure audio formats over combined video

            candidateFormats.add(score to url)
        }

        candidateFormats.sortByDescending { it.first }

        for ((score, url) in candidateFormats) {
            if (verifyStreamUrlNot403(url)) {
                android.util.Log.d("YouTubePlayback", "InnerTube ${client.name} selected verified stream: score=$score, url=${url.take(80)}...")
                return Result.success(url)
            } else {
                android.util.Log.w("YouTubePlayback", "InnerTube ${client.name} candidate returned 403 Forbidden, checking next format...")
            }
        }

        android.util.Log.w("YouTubePlayback", "No verified audio stream URL found in adaptiveFormats for ${client.name}")
        return Result.failure(IllegalStateException("No verified audio streams found via ${client.name}"))
    }

    private fun verifyStreamUrlNot403(streamUrl: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(streamUrl)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build()
            val response = okHttpClient.newCall(request).execute()
            val isOk = response.isSuccessful || response.code == 206
            response.close()
            isOk
        } catch (e: Exception) {
            false
        }
    }

    /**
     * NewPipe extractor fallback — used when InnerTube API fails.
     */
    @Suppress("DEPRECATION")
    private fun getAudioStreamViaNewPipe(videoId: String): Result<String> {
        return try {
            val url = "https://www.youtube.com/watch?v=$videoId"
            val extractor = ServiceList.YouTube.getStreamExtractor(url)
            extractor.fetchPage()

            val audioStreams = extractor.audioStreams
            if (audioStreams.isEmpty()) {
                return Result.failure(IllegalStateException("NewPipe: no audio streams for $videoId"))
            }

            val sortedStreams = audioStreams.sortedByDescending { stream ->
                val isOpus = stream.format?.name?.contains("opus", ignoreCase = true) == true
                val bitrate = stream.averageBitrate.takeIf { it > 0 } ?: 128
                if (isOpus) bitrate + 1000 else bitrate
            }

            for (stream in sortedStreams) {
                val streamUrl = stream.url
                if (!streamUrl.isNullOrBlank() && verifyStreamUrlNot403(streamUrl)) {
                    Timber.d("NewPipe verified stream for $videoId: format=${stream.format?.name}, bitrate=${stream.averageBitrate}")
                    return Result.success(streamUrl)
                }
            }

            Result.failure(IllegalStateException("NewPipe: no verified working audio streams for $videoId"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save a list of YouTube songs into Room database so they appear in local library/playlists.
     */
    suspend fun saveSongsToLibrary(songs: List<Song>) = withContext(Dispatchers.IO) {
        try {
            val entities = songs.map { song ->
                val songIdLong = generateSongIdLong(song.id)
                SongEntity(
                    id = songIdLong,
                    title = song.title,
                    artistId = generateArtistIdLong(song.artist),
                    artistName = song.artist,
                    albumId = generateAlbumIdLong(song.album),
                    albumName = song.album,
                    albumArtist = song.albumArtist ?: song.artist,
                    filePath = song.contentUriString,
                    parentDirectoryPath = YOUTUBE_PARENT_DIRECTORY,
                    contentUriString = song.contentUriString,
                    albumArtUriString = song.albumArtUriString,
                    duration = song.duration,
                    genre = YOUTUBE_GENRE,
                    trackNumber = 0,
                    year = 0,
                    dateAdded = System.currentTimeMillis(),
                    mimeType = "audio/opus",
                    sourceType = SourceType.YOUTUBE
                )
            }
            musicDao.insertSongs(entities)
        } catch (e: Exception) {
            Timber.e(e, "Failed to save YouTube songs to library")
        }
    }

    private fun mapStreamInfoToSong(item: StreamInfoItem): Song {
        val videoId = item.url.substringAfter("v=").substringBefore("&")
        val songIdLong = generateSongIdLong(videoId)
        val artistName = item.uploaderName ?: "Unknown Artist"
        val albumName = "YouTube Music"

        return Song(
            id = songIdLong.toString(),
            title = item.name ?: "Unknown Title",
            artist = artistName,
            artistId = generateArtistIdLong(artistName),
            artists = listOf(ArtistRef(id = generateArtistIdLong(artistName), name = artistName, isPrimary = true)),
            album = albumName,
            albumId = generateAlbumIdLong(albumName),
            albumArtist = artistName,
            path = "youtube://$videoId",
            contentUriString = "youtube://$videoId",
            albumArtUriString = item.thumbnails.firstOrNull()?.url,
            duration = item.duration * 1000L,
            genre = YOUTUBE_GENRE,
            mimeType = "audio/opus",
            bitrate = 160,
            sampleRate = 48000
        )
    }

    private fun generateSongIdLong(videoId: String): Long {
        return YOUTUBE_SONG_ID_OFFSET + (videoId.hashCode().toLong().absoluteValue % 1_000_000_000_000L)
    }

    private fun generateArtistIdLong(artistName: String): Long {
        return 7_100_000_000_000L + (artistName.hashCode().toLong().absoluteValue % 1_000_000_000_000L)
    }

    private fun generateAlbumIdLong(albumName: String): Long {
        return 7_200_000_000_000L + (albumName.hashCode().toLong().absoluteValue % 1_000_000_000_000L)
    }
}
