package com.threadcraft.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.ui.create.CreatePostViewModel
import com.threadcraft.app.ui.history.HistoryViewModel
import com.threadcraft.app.ui.settings.SettingsViewModel

class AppViewModelFactory(private val prefs: SecurePrefs) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(prefs) as T
            modelClass.isAssignableFrom(CreatePostViewModel::class.java) ->
                CreatePostViewModel(prefs) as T
            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(prefs) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
