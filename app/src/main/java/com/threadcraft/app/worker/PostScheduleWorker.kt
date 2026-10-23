package com.threadcraft.app.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.data.model.PostHistory
import com.threadcraft.app.data.repository.AppRepository
import java.io.File
import java.util.concurrent.TimeUnit

class PostScheduleWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val targetPostId = inputData.getLong("post_id", -1L)
        Log.d("PostScheduleWorker", "doWork started. targetPostId=$targetPostId")

        val prefs = SecurePrefs(applicationContext)
        if (!prefs.isThreadsConnected) {
            Log.w("PostScheduleWorker", "Threads not connected, cannot publish scheduled posts")
            return Result.failure()
        }

        val repo = AppRepository(prefs, applicationContext)
        val now = System.currentTimeMillis()

        val postsToProcess = mutableListOf<PostHistory>()
        if (targetPostId > 0) {
            val p = repo.getPost(targetPostId)
            if (p != null && p.status == "SCHEDULED") {
                postsToProcess.add(p)
            }
        }

        val duePosts = repo.getDueScheduled(now)
        for (dp in duePosts) {
            if (postsToProcess.none { it.id == dp.id }) {
                postsToProcess.add(dp)
            }
        }

        Log.d("PostScheduleWorker", "Total scheduled posts to publish: ${postsToProcess.size}")

        var hasFailure = false
        for (post in postsToProcess) {
            try {
                val mediaUris = post.mediaPaths.mapNotNull { path ->
                    val file = File(path)
                    if (file.exists()) Uri.fromFile(file) else null
                }

                val threadIds = repo.postToThreads(post.parts, mediaUris)

                // Clean up cached images after successful post
                post.mediaPaths.forEach { path ->
                    try { File(path).delete() } catch (_: Throwable) {}
                }

                repo.updatePost(
                    post.copy(
                        status = "POSTED",
                        threadIds = threadIds,
                        errorMessage = null
                    )
                )
                Log.d("PostScheduleWorker", "Post id=${post.id} published successfully.")
            } catch (e: Exception) {
                Log.e("PostScheduleWorker", "Post id=${post.id} failed to publish", e)
                hasFailure = true
                repo.updatePost(
                    post.copy(
                        status = "FAILED",
                        errorMessage = e.message ?: "Gagal posting ke Threads"
                    )
                )
            }
        }

        return if (hasFailure) Result.retry() else Result.success()
    }

    companion object {
        private const val WORK_PERIODIC_NAME = "threadcraft_post_scheduler_periodic"

        fun schedulePostWork(context: Context, postId: Long, targetTimeMillis: Long) {
            val now = System.currentTimeMillis()
            val delayMillis = (targetTimeMillis - now).coerceAtLeast(0)

            Log.d("PostScheduleWorker", "Scheduling work for postId=$postId with delay=${delayMillis}ms")

            // 1. OneTimeWorkRequest with initial delay and network requirement
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val workRequest = OneTimeWorkRequestBuilder<PostScheduleWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("post_id" to postId))
                .setConstraints(constraints)
                .addTag("scheduled_post_$postId")
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "scheduled_post_$postId",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )

            // 2. AlarmManager setExactAndAllowWhileIdle to wake up from Doze mode
            try {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager != null) {
                    val intent = Intent(context, PostAlarmReceiver::class.java).apply {
                        putExtra("post_id", postId)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        postId.toInt(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            targetTimeMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            targetTimeMillis,
                            pendingIntent
                        )
                    }
                    Log.d("PostScheduleWorker", "AlarmManager registered for postId=$postId at $targetTimeMillis")
                }
            } catch (e: Exception) {
                Log.w("PostScheduleWorker", "Failed to schedule exact alarm: ${e.message}")
            }
        }

        fun enqueueImmediateWork(context: Context, postId: Long? = null) {
            val workBuilder = OneTimeWorkRequestBuilder<PostScheduleWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
            if (postId != null && postId > 0) {
                workBuilder.setInputData(workDataOf("post_id" to postId))
            }
            val request = workBuilder.build()
            val name = if (postId != null && postId > 0) "immediate_post_$postId" else "immediate_check_due"
            WorkManager.getInstance(context).enqueueUniqueWork(
                name,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        fun enqueuePeriodicWork(context: Context) {
            val request = PeriodicWorkRequestBuilder<PostScheduleWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_PERIODIC_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
