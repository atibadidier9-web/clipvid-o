package com.example.videograbber

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.regex.Pattern

class TikTokExtractor : VideoExtractor {

    private val client = OkHttpClient.Builder().followRedirects(true).build()

    override fun matches(url: String): Boolean =
        PlatformDetector.detect(url) == Platform.TIKTOK

    override suspend fun extract(url: String): ExtractedVideo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
            )
            .build()

        client.newCall(request).execute().use { response ->
            val html = response.body?.string()
                ?: throw IllegalStateException("Réponse vide de TikTok")

            val scriptPattern = Pattern.compile(
                "<script id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\"[^>]*>(.*?)</script>",
                Pattern.DOTALL
            )
            val matcher = scriptPattern.matcher(html)
            if (!matcher.find()) {
                throw IllegalStateException(
                    "Structure de page TikTok non reconnue (probable changement côté TikTok)"
                )
            }

            val json = JSONObject(matcher.group(1)!!)
            val itemStruct = json
                .getJSONObject("__DEFAULT_SCOPE__")
                .getJSONObject("webapp.video-detail")
                .getJSONObject("itemInfo")
                .getJSONObject("itemStruct")

            val video = itemStruct.getJSONObject("video")
            val directUrl = video.optString("downloadAddr").ifBlank {
                video.getString("playAddr")
            }
            val id = itemStruct.optString("id", System.currentTimeMillis().toString())

            ExtractedVideo(directUrl = directUrl, suggestedName = "tiktok_$id.mp4")
        }
    }
}
