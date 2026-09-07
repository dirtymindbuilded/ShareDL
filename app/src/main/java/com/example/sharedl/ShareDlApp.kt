package com.example.sharedl

import android.app.Application
import android.util.Log
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class ShareDlApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            YoutubeDL.getInstance().init(this)
            FFmpeg.getInstance().init(this)
        } catch (t: Throwable) {
            Log.e("ShareDL", "Downloader initialization failed", t)
        }
    }
}
