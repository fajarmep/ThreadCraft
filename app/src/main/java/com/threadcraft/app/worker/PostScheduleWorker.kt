package com.threadcraft.app.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.data.repository.AppRepository
import java.util.concurrent.TimeUnit

class PostScheduleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = SecurePrefs(applicationContext)
        if (!prefs.isThreadsConnected) {
            return Result.success()
        }

        val repo = AppRepository(prefs)
        val now = System.currentTimeMillis()
        val duePosts = repo.getDueScheduled(now)

        for (post in duePosts) {
            try {
                val threadIds = repo.postToThreads(post.parts)
                repo.updatePost(
                    post.copy(
                        status = "POSTED",
                        threadIds = threadIds,
                        errorMessage = null
                    )
                )
            } catch (e: Exception) {
                repo.updatePost(
                    post.copy(
                        status = "FAILED",
                        errorMessage = e.message ?: "Unknown error"
                    )
                )
            }
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "threadcraft_post_scheduler"

        fun enqueuePeriodicWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<PostScheduleWorker>(15, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
