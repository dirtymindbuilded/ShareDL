package com.example.sharedl

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

class DownloadService : Service() {
    private val executor = Executors.newCachedThreadPool()

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra(EXTRA_URL) ?: return START_NOT_STICKY
        val quality = runCatching { Quality.valueOf(intent.getStringExtra(EXTRA_QUALITY).orEmpty()) }
            .getOrElse { Quality.BEST }
        val notificationId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
        val processId = UUID.randomUUID().toString()

        startForeground(notificationId, notification("Preparing download…", 0, true))

        executor.execute {
            try {
                val outDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ShareDL"
                ).apply { mkdirs() }

                val request = YoutubeDLRequest(url).apply {
                    addOption("--no-playlist")
                    addOption("--newline")
                    addOption("--restrict-filenames")
                    addOption("-f", quality.format)
                    addOption("-o", File(outDir, "%(title)s.%(ext)s").absolutePath)
                    if (quality.audioOnly) {
                        addOption("-x")
                        addOption("--audio-format", "mp3")
                        addOption("--audio-quality", "0")
                    } else {
                        addOption("--merge-output-format", "mp4")
                    }
                }

                YoutubeDL.getInstance().execute(request, { progress, eta ->
                    val p = progress.toInt().coerceIn(0, 100)
                    val etaText = if (eta > 0) " • ETA ${eta}s" else ""
                    notify(notificationId, notification("Downloading $p%$etaText", p, false))
                }, processId)

                notify(notificationId, notification("Saved to Download/ShareDL", 100, false, done = true))
            } catch (t: Throwable) {
                notify(notificationId, notification("Download failed: ${t.message ?: "unknown error"}", 0, false, done = true))
            } finally {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        executor.shutdown()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(text: String, progress: Int, indeterminate: Boolean, done: Boolean = false) =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(if (done) "ShareDL" else "ShareDL download")
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(!done)
            .setAutoCancel(done)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .apply { if (!done) setProgress(100, progress, indeterminate) }
            .build()

    private fun notify(id: Int, notification: android.app.Notification) {
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    companion object {
        private const val CHANNEL_ID = "downloads"
        private const val EXTRA_URL = "url"
        private const val EXTRA_QUALITY = "quality"

        fun start(context: Context, url: String, quality: Quality) {
            val intent = Intent(context, DownloadService::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_QUALITY, quality.name)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
