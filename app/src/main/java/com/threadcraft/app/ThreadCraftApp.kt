package com.threadcraft.app

import android.app.Application
import android.util.Log
import com.threadcraft.app.data.local.AppDatabase

class ThreadCraftApp : Application() {

    val database: AppDatabase
        get() = AppDatabase.getDatabase(this)

    override fun onCreate() {
        super.onCreate()
        instance = this

        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("ThreadCraftApp", "Uncaught exception in thread ${thread.name}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            com.threadcraft.app.worker.PostScheduleWorker.enqueuePeriodicWork(this)
        } catch (t: Throwable) {
            Log.e("ThreadCraftApp", "Failed to enqueue periodic worker", t)
        }
    }

    companion object {
        lateinit var instance: ThreadCraftApp
            private set
    }
}
