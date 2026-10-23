package com.threadcraft.app

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.threadcraft.app.data.local.AppDatabase

class ThreadCraftApp : Application(), ImageLoaderFactory {

    val database: AppDatabase
        get() = AppDatabase.getDatabase(this)

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stack = android.util.Log.getStackTraceString(throwable)
                android.util.Log.e("ThreadCraftApp", "FATAL CRASH: $stack")
                val crashPrefs = getSharedPreferences("crash_dump", android.content.Context.MODE_PRIVATE)
                crashPrefs.edit().putString("last_crash", stack).commit()
            } catch (_: Throwable) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            com.threadcraft.app.worker.PostScheduleWorker.enqueuePeriodicWork(this)
            com.threadcraft.app.worker.PostScheduleWorker.enqueueImmediateWork(this)
        } catch (t: Throwable) {
            Log.e("ThreadCraftApp", "Failed to enqueue worker", t)
        }
    }

    companion object {
        lateinit var instance: ThreadCraftApp
            private set
    }
}
