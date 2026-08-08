package com.example.videograbber

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup

class InstagramExtractor : VideoExtractor {

    private val client = OkHttpClient.Builder().followRedirects(true).build()

    override fun matches(url: String): Boolean =
        PlatformDetector.detect(url) == Platform.INSTAGRAM

    override suspend fun extract(url: String): ExtractedVideo = withContext(Dispatchers.IO) {
        val shortcode = extractShortcode(url)
            ?: throw IllegalStateException("Impossible d'identifier l'ID du post Instagram")

        val embedUrl = "https://www.instagram.com/p/$shortcode/embed/"
        val request = Request.Builder()
            .url(embedUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
            )
            .build()

        client.newCall(request).execute().use { response ->
            val html = response.body?.string()
                ?: throw IllegalStateException("Réponse vide d'Instagram")

            val doc = Jsoup.parse(html)
            val videoTag = doc.select("video[src]").firstOrNull()
                ?: throw IllegalStateException(
                    "Post sans vidéo détectée, ou compte privé / structure changée"
                )

            val directUrl = videoTag.attr("src")
            ExtractedVideo(directUrl = directUrl, suggestedName = "instagram_$shortcode.mp4")
        }
    }

    private fun extractShortcode(url: String): String? {
        val regex = Regex("instagram\\.com/(?:p|reel|tv)/([A-Za-z0-9_-]+)")
        return regex.find(url)?.groupValues?.get(1)
    }
}
