package com.example.videograbber

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var lastHandledText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkClipboardForLink()
    }

    override fun onDestroy() {
        (scope.coroutineContext[Job])?.cancel()
        super.onDestroy()
    }

    private fun checkClipboardForLink() {
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        if (clip == null || clip.itemCount == 0) {
            tvStatus.text = "En attente d'un lien copié…"
            return
        }
        val text = clip.getItemAt(0).text?.toString()
        if (text == null || text == lastHandledText) return

        val platform = PlatformDetector.detect(text)
        if (platform == Platform.UNKNOWN) {
            tvStatus.text = "En attente d'un lien copié…"
            return
        }

        lastHandledText = text
        tvStatus.text = "Lien détecté, extraction en cours…"
        val extractor = PlatformDetector.extractorFor(platform) ?: return

        scope.launch {
            try {
                val video = extractor.extract(text)
                DownloadHelper.enqueueDownload(this@MainActivity, video)
                tvStatus.text = "Téléchargement lancé : ${video.suggestedName}"
                Toast.makeText(this@MainActivity, "Téléchargement lancé", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                tvStatus.text = "Échec : ${e.message}"
                Toast.makeText(this@MainActivity, "Échec de l'extraction", Toast.LENGTH_LONG).show()
            }
        }
    }
}
