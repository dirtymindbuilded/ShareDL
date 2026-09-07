package com.example.sharedl

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.UpdateChannel
import com.yausername.youtubedl_android.YoutubeDL
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: TextInputEditText
    private lateinit var statusText: TextView
    private lateinit var downloadButton: MaterialButton

    private val executor = Executors.newSingleThreadExecutor()

    private var engineReady = false
    private var pendingSharedUrl: String? = null

    private val notificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        urlInput = findViewById(R.id.urlInput)
        statusText = findViewById(R.id.statusText)
        downloadButton = findViewById(R.id.downloadButton)

        requestNotificationPermission()

        downloadButton.isEnabled = false
        statusText.text = "Preparing download engine..."

        pendingSharedUrl = extractSharedUrl(intent)

        pendingSharedUrl?.let {
            urlInput.setText(it)
            urlInput.setSelection(it.length)
        }

        downloadButton.setOnClickListener {
            startDownloadFromInput()
        }

        initializeAndUpdateEngine()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        val sharedUrl = extractSharedUrl(intent) ?: return

        urlInput.setText(sharedUrl)
        urlInput.setSelection(sharedUrl.length)

        if (engineReady) {
            showQualityDialog(
                url = sharedUrl,
                finishAfterStart = true
            )
        } else {
            pendingSharedUrl = sharedUrl
            statusText.text = "Preparing download engine..."
        }
    }

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private fun initializeAndUpdateEngine() {

        executor.execute {

            var errorMessage: String? = null

            try {

                YoutubeDL.getInstance().init(applicationContext)

                FFmpeg.getInstance().init(applicationContext)

                runOnUiThread {
                    statusText.text = "Updating yt-dlp..."
                }

                /*
                 * Download the latest stable yt-dlp version.
                 *
                 * This prevents the:
                 * "yt-dlp version is older than 90 days"
                 * error.
                 */
                YoutubeDL.getInstance().updateYoutubeDL(
                    applicationContext,
                    UpdateChannel.STABLE
                )

            } catch (e: Throwable) {

                errorMessage = e.message ?: "Unknown engine error"

            }

            runOnUiThread {

                if (isFinishing || isDestroyed) {
                    return@runOnUiThread
                }

                engineReady = true
                downloadButton.isEnabled = true

                if (errorMessage == null) {

                    statusText.text =
                        "Download engine ready"

                } else {

                    /*
                     * Do not completely block the app if the update
                     * server is temporarily unavailable.
                     *
                     * A previously updated local yt-dlp may still work.
                     */
                    statusText.text =
                        "Engine ready (update unavailable)"

                    Toast.makeText(
                        this,
                        "yt-dlp update failed: $errorMessage",
                        Toast.LENGTH_LONG
                    ).show()
                }

                pendingSharedUrl?.let { url ->

                    pendingSharedUrl = null

                    showQualityDialog(
                        url = url,
                        finishAfterStart = true
                    )
                }
            }
        }
    }

    private fun startDownloadFromInput() {

        if (!engineReady) {

            Toast.makeText(
                this,
                "Please wait for yt-dlp to finish updating",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val url =
            urlInput.text
                ?.toString()
                ?.trim()
                .orEmpty()

        if (!isLikelyUrl(url)) {

            Toast.makeText(
                this,
                "Enter a valid http/https URL",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        showQualityDialog(
            url = url,
            finishAfterStart = false
        )
    }

    private fun showQualityDialog(
        url: String,
        finishAfterStart: Boolean
    ) {

        val choices =
            Quality.entries.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Download quality")

            .setItems(
                choices.map {
                    it.label
                }.toTypedArray()
            ) { _, which ->

                val selected =
                    choices[which]

                DownloadService.start(
                    this,
                    url,
                    selected
                )

                statusText.text =
                    "Started: ${selected.label}"

                Toast.makeText(
                    this,
                    "Download started",
                    Toast.LENGTH_SHORT
                ).show()

                if (finishAfterStart) {
                    finish()
                }
            }

            .setNegativeButton(
                "Cancel"
            ) { _, _ ->

                if (finishAfterStart) {
                    finish()
                }
            }

            .setOnCancelListener {

                if (finishAfterStart) {
                    finish()
                }
            }

            .show()
    }

    private fun extractSharedUrl(
        intent: Intent
    ): String? {

        if (
            intent.action != Intent.ACTION_SEND ||
            intent.type != "text/plain"
        ) {
            return null
        }

        val text =
            intent.getStringExtra(
                Intent.EXTRA_TEXT
            )
                ?.trim()
                .orEmpty()

        val url =
            Regex("https?://\\S+")
                .find(text)
                ?.value
                ?.trimEnd(
                    '.',
                    ',',
                    ')',
                    ']'
                )

        return url?.takeIf(
            ::isLikelyUrl
        )
    }

    private fun isLikelyUrl(
        value: String
    ): Boolean {

        return (
            value.startsWith("https://") ||
                value.startsWith("http://")
            ) &&
            Patterns.WEB_URL
                .matcher(value)
                .matches()
    }

    override fun onDestroy() {

        executor.shutdown()

        super.onDestroy()
    }
}
