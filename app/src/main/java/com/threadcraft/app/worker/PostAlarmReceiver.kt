package com.threadcraft.app.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PostAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val postId = intent.getLongExtra("post_id", -1L)
        Log.d("PostAlarmReceiver", "Alarm received for postId=$postId, action=${intent.action}")
        // Immediately enqueue WorkManager to process the post
        PostScheduleWorker.enqueueImmediateWork(context, if (postId > 0) postId else null)
    }
}
