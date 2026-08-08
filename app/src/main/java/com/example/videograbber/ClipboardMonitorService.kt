package com.example.videograbber

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ClipboardMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var clipboardManager: ClipboardManager
    private var lastHandledText: String? = null

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        handleClipboardChange()
    }

    override fun onCreate() {
        super.onCreate()
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener(listener)
        startForeground(NOTIF_ID_SERVICE, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        clipboardManager.removePrimaryClipChangedListener(listener)
        (scope.coroutineContext[Job])?.cancel()
        super.onDestroy()
    }

    private fun handleClipboardChange() {
        val clip = clipboardManager.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).text?.toString() ?: return
        if (text == lastHandledText) return
        if (PlatformDetector.detect(text) == Platform.UNKNOWN) return

        lastHandledText = text
        processLink(text)
    }

    private fun processLink(url: String) {
        val platform = PlatformDetector.detect(url)
        val extractor = PlatformDetector.extractorFor(platform) ?: return

        scope.launch {
            try {
                val video = extractor.extract(url)
                DownloadHelper.enqueueDownload(this@ClipboardMonitorService, video)
                showResultNotification("Téléchargement lancé", video.suggestedName)
            } catch (e: Exception) {
                showResultNotification(
                    "Échec de l'extraction",
                    e.message ?: "Structure de page non reconnue"
                )
            }
        }
    }

    private fun buildForegroundNotification(): android.app.Notification {
        createChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("ClipVideo actif")
            .setContentText("Surveillance du presse-papiers en cours…")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setOngoing(true)
            .build()
    }

    private fun showResultNotification(title: String, message: String) {
        createChannel()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIF_ID_RESULT_COUNTER++, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID, "ClipVideo", NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "video_grabber_channel"
        private const val NOTIF_ID_SERVICE = 1
        private var NOTIF_ID_RESULT_COUNTER = 100
    }
}
