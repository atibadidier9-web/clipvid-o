package com.example.videograbber

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment

object DownloadHelper {

    fun enqueueDownload(context: Context, video: ExtractedVideo) {
        val request = DownloadManager.Request(Uri.parse(video.directUrl))
            .setTitle(video.suggestedName)
            .setDescription("Téléchargement en cours…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_MOVIES,
                video.suggestedName
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }
}
