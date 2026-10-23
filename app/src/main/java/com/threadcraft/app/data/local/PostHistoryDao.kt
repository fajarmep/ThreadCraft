package com.threadcraft.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.threadcraft.app.data.model.PostHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PostHistoryDao {
    @Query("SELECT * FROM post_history ORDER BY createdAt DESC")
    fun getAll(): Flow<List<PostHistory>>

    @Insert
    suspend fun insert(post: PostHistory): Long

    @Update
    suspend fun update(post: PostHistory)

    @Query("SELECT * FROM post_history WHERE id = :id")
    suspend fun getById(id: Long): PostHistory?

    @Query("SELECT * FROM post_history WHERE status = 'SCHEDULED' AND scheduledAt <= :now")
    suspend fun getDueScheduled(now: Long): List<PostHistory>

    @Query("UPDATE post_history SET status = 'POSTING' WHERE id = :id AND status = 'SCHEDULED'")
    suspend fun claimForPosting(id: Long): Int

    @Query("DELETE FROM post_history WHERE id = :id")
    suspend fun deleteById(id: Long)
}
