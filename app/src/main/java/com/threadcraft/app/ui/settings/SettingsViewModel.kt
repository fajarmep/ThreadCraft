package com.threadcraft.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsState(
    val aiBaseUrl: String = "",
    val aiApiKey: String = "",
    val aiModel: String = "",
    val threadsAppId: String = "",
    val threadsAppSecret: String = "",
    val threadsUsername: String = "",
    val isThreadsConnected: Boolean = false,
    val showOAuthWebView: Boolean = false,
    val oauthUrl: String = "",
    val isSaving: Boolean = false,
    val isExchangingToken: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

class SettingsViewModel(private val prefs: SecurePrefs) : ViewModel() {

    private val repo = AppRepository(prefs)
    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        _state.value = SettingsState(
            aiBaseUrl = prefs.aiBaseUrl,
            aiApiKey = prefs.aiApiKey,
            aiModel = prefs.aiModel,
            threadsAppId = prefs.threadsAppId,
            threadsAppSecret = prefs.threadsAppSecret,
            threadsUsername = prefs.threadsUsername,
            isThreadsConnected = prefs.isThreadsConnected
        )
    }

    fun updateAiBaseUrl(v: String) { _state.value = _state.value.copy(aiBaseUrl = v) }
    fun updateAiApiKey(v: String) { _state.value = _state.value.copy(aiApiKey = v) }
    fun updateAiModel(v: String) { _state.value = _state.value.copy(aiModel = v) }
    fun updateThreadsAppId(v: String) { _state.value = _state.value.copy(threadsAppId = v) }
    fun updateThreadsAppSecret(v: String) { _state.value = _state.value.copy(threadsAppSecret = v) }

    fun save() {
        val s = _state.value
        prefs.aiBaseUrl = s.aiBaseUrl
        prefs.aiApiKey = s.aiApiKey
        prefs.aiModel = s.aiModel
        prefs.threadsAppId = s.threadsAppId
        prefs.threadsAppSecret = s.threadsAppSecret
        prefs.isSetupComplete = true
        _state.value = s.copy(saveSuccess = true)
    }

    fun clearSaveSuccess() {
        _state.value = _state.value.copy(saveSuccess = false)
    }

    fun startOAuth() {
        val s = _state.value
        if (s.threadsAppId.isBlank()) {
            _state.value = s.copy(error = "Threads App ID harus diisi dulu")
            return
        }
        val redirectUri = "https://threadcraft.app/callback"
        val url = Uri.parse("https://threads.net/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", s.threadsAppId)
            .appendQueryParameter("redirect_uri", redirectUri)
            .appendQueryParameter("scope", "threads_basic,threads_content_publish")
            .appendQueryParameter("response_type", "code")
            .build()
            .toString()

        _state.value = s.copy(showOAuthWebView = true, oauthUrl = url)
    }

    fun dismissOAuth() {
        _state.value = _state.value.copy(showOAuthWebView = false)
    }

    fun handleOAuthCallback(url: String) {
        val uri = Uri.parse(url)
        val code = uri.getQueryParameter("code") ?: return
        _state.value = _state.value.copy(showOAuthWebView = false, isExchangingToken = true)

        viewModelScope.launch {
            try {
                val (username, _) = repo.exchangeToken(code)
                _state.value = _state.value.copy(
                    isExchangingToken = false,
                    isThreadsConnected = true,
                    threadsUsername = username
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isExchangingToken = false,
                    error = "Login gagal: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        prefs.clearThreadsAuth()
        _state.value = _state.value.copy(
            isThreadsConnected = false,
            threadsUsername = ""
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
