package com.threadcraft.app

import android.app.Application
import androidx.room.Room
import com.threadcraft.app.data.local.AppDatabase

class ThreadCraftApp : Application() {

    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        try {
            database = Room.databaseBuilder(
                this,
                AppDatabase::class.java,
                "threadcraft.db"
            ).fallbackToDestructiveMigration().build()
        } catch (t: Throwable) {
            android.util.Log.e("ThreadCraftApp", "Error initializing Room DB", t)
        }

        try {
            com.threadcraft.app.worker.PostScheduleWorker.enqueuePeriodicWork(this)
        } catch (t: Throwable) {
            android.util.Log.e("ThreadCraftApp", "Error scheduling periodic worker", t)
        }
    }

    companion object {
        lateinit var instance: ThreadCraftApp
            private set
    }
}
