package com.theveloper.pixelplay.data.youtube

import android.net.Uri
import com.theveloper.pixelplay.data.stream.CloudStreamProxy
import com.theveloper.pixelplay.data.stream.CloudStreamSecurity
import okhttp3.OkHttpClient
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeStreamProxy @Inject constructor(
    private val repository: YouTubeMusicRepository,
    okHttpClient: OkHttpClient
) : CloudStreamProxy<String>(okHttpClient) {

    override val allowedHostSuffixes = setOf(
        "youtube.com",
        "googlevideo.com",
        "ytimg.com",
        "youtube-nocookie.com"
    )
    override val cacheExpirationMs = 3L * 60 * 60 * 1000 // 3 hours
    override val proxyTag = "YouTubeStreamProxy"
    override val routePath = "/youtube/{videoId}"
    override val routeParamName = "videoId"
    override val uriScheme = "youtube"
    override val routePrefix = "/youtube"

    override fun parseRouteParam(value: String): String? =
        value.takeIf { it.isNotBlank() }

    override fun validateId(id: String): Boolean =
        CloudStreamSecurity.validateYouTubeVideoId(id)

    override fun formatIdForUrl(id: String): String = id

    override suspend fun resolveStreamUrl(id: String): String? =
        repository.getAudioStreamUrl(id).getOrNull()

    override fun getAdditionalHeaders(id: String): Map<String, String> = mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Referer" to "https://www.youtube.com/watch?v=$id"
    )

    override fun extractIdFromUri(uri: Uri): String? {
        val host = uri.host
        if (host != null && CloudStreamSecurity.validateYouTubeVideoId(host)) return host
        
        // Handle URIs like youtube:///videoId or youtube:videoId
        val pathId = uri.path?.removePrefix("/")
        if (pathId != null && CloudStreamSecurity.validateYouTubeVideoId(pathId)) return pathId
        
        val ssp = uri.schemeSpecificPart?.removePrefix("//")?.removePrefix("/")
        if (ssp != null && CloudStreamSecurity.validateYouTubeVideoId(ssp)) return ssp
        
        return null
    }

    fun resolveYouTubeUri(uriString: String): String? = resolveUri(uriString)

    suspend fun warmUpStreamUrl(uriString: String) {
        val uri = Uri.parse(uriString)
        if (uri.scheme != "youtube") return
        val videoId = extractIdFromUri(uri) ?: return
        if (!CloudStreamSecurity.validateYouTubeVideoId(videoId)) return
        try {
            getOrFetchStreamUrl(videoId)
        } catch (e: Exception) {
            Timber.w(e, "warmUpStreamUrl failed for YouTube videoId: $videoId")
        }
    }
}
