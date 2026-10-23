package com.threadcraft.app.data.local

import android.content.Context
import android.content.SharedPreferences

class SecurePrefs(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        "threadcraft_app_prefs",
        Context.MODE_PRIVATE
    )

    var aiBaseUrl: String
        get() = prefs.getString(KEY_AI_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AI_BASE_URL, value).apply()

    var aiApiKey: String
        get() = prefs.getString(KEY_AI_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AI_API_KEY, value).apply()

    var aiModel: String
        get() = prefs.getString(KEY_AI_MODEL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_AI_MODEL, value).apply()

    var threadsAppId: String
        get() = prefs.getString(KEY_THREADS_APP_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_THREADS_APP_ID, value).apply()

    var threadsAppSecret: String
        get() = prefs.getString(KEY_THREADS_APP_SECRET, "") ?: ""
        set(value) = prefs.edit().putString(KEY_THREADS_APP_SECRET, value).apply()

    var threadsAccessToken: String
        get() = prefs.getString(KEY_THREADS_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_THREADS_TOKEN, value).apply()

    var threadsUserId: String
        get() = prefs.getString(KEY_THREADS_USER_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_THREADS_USER_ID, value).apply()

    var threadsUsername: String
        get() = prefs.getString(KEY_THREADS_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_THREADS_USERNAME, value).apply()

    var isSetupComplete: Boolean
        get() = prefs.getBoolean(KEY_SETUP_COMPLETE, false)
        set(value) = prefs.edit().putBoolean(KEY_SETUP_COMPLETE, value).apply()

    val isThreadsConnected: Boolean
        get() = threadsAccessToken.isNotBlank()

    fun clearThreadsAuth() {
        prefs.edit()
            .remove(KEY_THREADS_TOKEN)
            .remove(KEY_THREADS_USER_ID)
            .remove(KEY_THREADS_USERNAME)
            .apply()
    }

    companion object {
        private const val KEY_AI_BASE_URL = "ai_base_url"
        private const val KEY_AI_API_KEY = "ai_api_key"
        private const val KEY_AI_MODEL = "ai_model"
        private const val KEY_THREADS_APP_ID = "threads_app_id"
        private const val KEY_THREADS_APP_SECRET = "threads_app_secret"
        private const val KEY_THREADS_TOKEN = "threads_access_token"
        private const val KEY_THREADS_USER_ID = "threads_user_id"
        private const val KEY_THREADS_USERNAME = "threads_username"
        private const val KEY_SETUP_COMPLETE = "setup_complete"
    }
}
