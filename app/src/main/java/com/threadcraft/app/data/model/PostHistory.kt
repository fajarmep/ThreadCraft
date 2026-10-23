package com.threadcraft.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "post_history")
@TypeConverters(Converters::class)
data class PostHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productTitle: String,
    val affiliateLink: String,
    val storyStyle: String,
    val parts: List<String>,
    val status: String, // DRAFT, POSTED, SCHEDULED, FAILED
    val threadIds: List<String> = emptyList(),
    val scheduledAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>?): String = gson.toJson(value ?: emptyList<String>())

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson(value, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
