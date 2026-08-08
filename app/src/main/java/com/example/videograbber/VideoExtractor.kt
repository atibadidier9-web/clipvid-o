package com.example.videograbber

data class ExtractedVideo(val directUrl: String, val suggestedName: String)

interface VideoExtractor {
    fun matches(url: String): Boolean
    suspend fun extract(url: String): ExtractedVideo
}

enum class Platform { TIKTOK, INSTAGRAM, FACEBOOK, UNKNOWN }

object PlatformDetector {
    fun detect(url: String): Platform {
        val u = url.lowercase()
        return when {
            u.contains("tiktok.com") -> Platform.TIKTOK
            u.contains("instagram.com") -> Platform.INSTAGRAM
            u.contains("facebook.com") || u.contains("fb.watch") -> Platform.FACEBOOK
            else -> Platform.UNKNOWN
        }
    }

    fun extractorFor(platform: Platform): VideoExtractor? = when (platform) {
        Platform.TIKTOK -> TikTokExtractor()
        Platform.INSTAGRAM -> InstagramExtractor()
        Platform.FACEBOOK -> FacebookExtractor()
        Platform.UNKNOWN -> null
    }
}
