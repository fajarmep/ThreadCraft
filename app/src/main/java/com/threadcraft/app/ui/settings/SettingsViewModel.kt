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
    val redirectUri: String = "https://threadcraft.app/callback",
    val isThreadsConnected: Boolean = false,
    val showOAuthWebView: Boolean = false,
    val oauthUrl: String = "",
    val isSaving: Boolean = false,
    val isExchangingToken: Boolean = false,
    val isTestingAi: Boolean = false,
    val aiTestMessage: String? = null,
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
            redirectUri = prefs.redirectUri.ifBlank { "https://threadcraft.app/callback" },
            isThreadsConnected = prefs.isThreadsConnected
        )
    }

    fun updateAiBaseUrl(v: String) { _state.value = _state.value.copy(aiBaseUrl = v) }
    fun updateAiApiKey(v: String) { _state.value = _state.value.copy(aiApiKey = v) }
    fun updateAiModel(v: String) { _state.value = _state.value.copy(aiModel = v) }
    fun updateThreadsAppId(v: String) { _state.value = _state.value.copy(threadsAppId = v) }
    fun updateThreadsAppSecret(v: String) { _state.value = _state.value.copy(threadsAppSecret = v) }
    fun updateRedirectUri(v: String) { _state.value = _state.value.copy(redirectUri = v) }

    fun save() {
        val s = _state.value
        prefs.aiBaseUrl = s.aiBaseUrl.trim()
        prefs.aiApiKey = s.aiApiKey.trim()
        prefs.aiModel = s.aiModel.trim()
        prefs.threadsAppId = s.threadsAppId.trim()
        prefs.threadsAppSecret = s.threadsAppSecret.trim()
        prefs.redirectUri = s.redirectUri.trim().ifBlank { "https://threadcraft.app/callback" }
        prefs.isSetupComplete = true
        _state.value = s.copy(saveSuccess = true)
    }

    fun testAiConnection() {
        val s = _state.value
        val baseUrl = s.aiBaseUrl.trim()
        val apiKey = s.aiApiKey.trim()
        val model = s.aiModel.trim()

        if (baseUrl.isBlank() || apiKey.isBlank() || model.isBlank()) {
            _state.value = s.copy(error = "Lengkapi AI Base URL, API Key, dan Model terlebih dahulu")
            return
        }

        prefs.aiBaseUrl = baseUrl
        prefs.aiApiKey = apiKey
        prefs.aiModel = model

        _state.value = _state.value.copy(isTestingAi = true, aiTestMessage = null, error = null)
        viewModelScope.launch {
            try {
                val reply = repo.testAiConnection()
                _state.value = _state.value.copy(
                    isTestingAi = false,
                    aiTestMessage = "Koneksi Berhasil! Respon: \"$reply\""
                )
            } catch (t: Throwable) {
                android.util.Log.e("SettingsViewModel", "AI connection test error", t)
                _state.value = _state.value.copy(
                    isTestingAi = false,
                    error = "Test AI gagal: ${t.localizedMessage ?: t.message ?: "Koneksi gagal"}"
                )
            }
        }
    }

    fun clearSaveSuccess() {
        _state.value = _state.value.copy(saveSuccess = false)
    }

    fun clearAiTestMessage() {
        _state.value = _state.value.copy(aiTestMessage = null)
    }

    fun startOAuth() {
        val s = _state.value
        if (s.threadsAppId.isBlank()) {
            _state.value = s.copy(error = "Threads App ID harus diisi dulu")
            return
        }
        val targetRedirect = s.redirectUri.trim().ifBlank { "https://threadcraft.app/callback" }
        prefs.redirectUri = targetRedirect
        prefs.threadsAppId = s.threadsAppId.trim()
        prefs.threadsAppSecret = s.threadsAppSecret.trim()

        val url = Uri.parse("https://threads.net/oauth/authorize").buildUpon()
            .appendQueryParameter("client_id", s.threadsAppId.trim())
            .appendQueryParameter("redirect_uri", targetRedirect)
            .appendQueryParameter("scope", "threads_basic,threads_content_publish,threads_manage_replies")
            .appendQueryParameter("response_type", "code")
            .build()
            .toString()

        _state.value = s.copy(showOAuthWebView = true, oauthUrl = url)
    }

    fun dismissOAuth() {
        _state.value = _state.value.copy(showOAuthWebView = false)
    }

    fun handleOAuthCallback(urlOrCode: String) {
        val trimmed = urlOrCode.trim()
        val code = if (trimmed.contains("code=")) {
            val uri = Uri.parse(trimmed)
            uri.getQueryParameter("code") ?: trimmed.substringAfter("code=").substringBefore("&").substringBefore("#")
        } else {
            trimmed.substringBefore("#")
        }
        if (code.isBlank()) return
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
