package com.threadcraft.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.threadcraft.app.data.model.Converters
import com.threadcraft.app.data.model.PostHistory

@Database(entities = [PostHistory::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postHistoryDao(): PostHistoryDao
}
