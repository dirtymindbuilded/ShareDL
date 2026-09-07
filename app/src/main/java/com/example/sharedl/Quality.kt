package com.example.sharedl

enum class Quality(val label: String, val format: String, val audioOnly: Boolean = false) {
    BEST("Best available", "bestvideo+bestaudio/best"),
    P1080("1080p", "bestvideo[height<=1080]+bestaudio/best[height<=1080]"),
    P720("720p", "bestvideo[height<=720]+bestaudio/best[height<=720]"),
    P480("480p", "bestvideo[height<=480]+bestaudio/best[height<=480]"),
    P360("360p", "bestvideo[height<=360]+bestaudio/best[height<=360]"),
    AUDIO("Audio only (MP3)", "bestaudio/best", true)
}
