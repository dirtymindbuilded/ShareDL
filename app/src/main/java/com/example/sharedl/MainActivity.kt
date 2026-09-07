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

class MainActivity : AppCompatActivity() {
    private lateinit var urlInput: TextInputEditText
    private lateinit var statusText: TextView

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        urlInput = findViewById(R.id.urlInput)
        statusText = findViewById(R.id.statusText)
        val button: MaterialButton = findViewById(R.id.downloadButton)

        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        extractSharedUrl(intent)?.let {
            urlInput.setText(it)
            urlInput.setSelection(it.length)
            showQualityDialog(it, finishAfterStart = true)
        }

        button.setOnClickListener {
            val url = urlInput.text?.toString()?.trim().orEmpty()
            if (!isLikelyUrl(url)) {
                Toast.makeText(this, "Enter a valid http/https URL", Toast.LENGTH_SHORT).show()
            } else {
                showQualityDialog(url, finishAfterStart = false)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractSharedUrl(intent)?.let {
            urlInput.setText(it)
            showQualityDialog(it, finishAfterStart = true)
        }
    }

    private fun showQualityDialog(url: String, finishAfterStart: Boolean) {
        val choices = Quality.entries.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Download quality")
            .setItems(choices.map { it.label }.toTypedArray()) { _, which ->
                val selected = choices[which]
                DownloadService.start(this, url, selected)
                statusText.text = "Started: ${selected.label}"
                Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show()
                if (finishAfterStart) finish()
            }
            .setNegativeButton("Cancel") { _, _ -> if (finishAfterStart) finish() }
            .setOnCancelListener { if (finishAfterStart) finish() }
            .show()
    }

    private fun extractSharedUrl(intent: Intent): String? {
        if (intent.action != Intent.ACTION_SEND || intent.type != "text/plain") return null
        val text = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
        val url = Regex("https?://\\S+").find(text)?.value?.trimEnd('.', ',', ')', ']')
        return url?.takeIf(::isLikelyUrl)
    }

    private fun isLikelyUrl(value: String): Boolean =
        (value.startsWith("https://") || value.startsWith("http://")) && Patterns.WEB_URL.matcher(value).matches()
}
