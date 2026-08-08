package com.example.videograbber

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.regex.Pattern

class FacebookExtractor : VideoExtractor {

    private val client = OkHttpClient.Builder().followRedirects(true).build()

    override fun matches(url: String): Boolean =
        PlatformDetector.detect(url) == Platform.FACEBOOK

    override suspend fun extract(url: String): ExtractedVideo = withContext(Dispatchers.IO) {
        val mobileUrl = toMobileUrl(url)
        val request = Request.Builder()
            .url(mobileUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"
            )
            .build()

        client.newCall(request).execute().use { response ->
            val html = response.body?.string()
                ?: throw IllegalStateException("Réponse vide de Facebook")

            val hdPattern = Pattern.compile("\"hd_src\":\"(https:[^\"]+)\"")
            val sdPattern = Pattern.compile("\"sd_src\":\"(https:[^\"]+)\"")

            val hdMatcher = hdPattern.matcher(html)
            val sdMatcher = sdPattern.matcher(html)

            val rawUrl = when {
                hdMatcher.find() -> hdMatcher.group(1)!!
                sdMatcher.find() -> sdMatcher.group(1)!!
                else -> throw IllegalStateException(
                    "Aucun lien vidéo trouvé (contenu privé, live, ou structure changée)"
                )
            }

            val directUrl = rawUrl.replace("\\/", "/")
            ExtractedVideo(
                directUrl = directUrl,
                suggestedName = "facebook_${System.currentTimeMillis()}.mp4"
            )
        }
    }

    private fun toMobileUrl(url: String): String =
        url.replace("www.facebook.com", "m.facebook.com")
            .replace("web.facebook.com", "m.facebook.com")
}
