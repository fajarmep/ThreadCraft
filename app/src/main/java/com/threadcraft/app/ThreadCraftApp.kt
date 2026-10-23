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
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "threadcraft.db"
        ).fallbackToDestructiveMigration().build()
        com.threadcraft.app.worker.PostScheduleWorker.enqueuePeriodicWork(this)
    }

    companion object {
        lateinit var instance: ThreadCraftApp
            private set
    }
}
