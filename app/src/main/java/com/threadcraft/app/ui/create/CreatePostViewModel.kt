package com.threadcraft.app.ui.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.data.model.PostHistory
import com.threadcraft.app.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CreatePostState(
    val productTitle: String = "",
    val description: String = "",
    val affiliateLink: String = "",
    val mediaUris: List<Uri> = emptyList(),
    val storyStyle: String = "Cerpen Storytelling",
    val numParts: String = "Auto",
    val generatedParts: List<String> = emptyList(),
    val isGenerating: Boolean = false,
    val isPosting: Boolean = false,
    val showScheduler: Boolean = false,
    val scheduledTime: Long? = null,
    val error: String? = null,
    val postSuccess: Boolean = false,
    val scheduleSuccess: Boolean = false,
    val showStyleDropdown: Boolean = false,
    val showPartsDropdown: Boolean = false
)

class CreatePostViewModel(private val prefs: SecurePrefs) : ViewModel() {

    private val repo = AppRepository(prefs)
    private val _state = MutableStateFlow(CreatePostState())
    val state: StateFlow<CreatePostState> = _state.asStateFlow()

    val storyStyles = listOf(
        "Cerpen Storytelling",
        "Review Pengalaman",
        "Thread Tips",
        "Perbandingan Produk"
    )
    val partOptions = listOf("Auto", "3", "5", "7")

    fun updateProductTitle(v: String) { _state.value = _state.value.copy(productTitle = v) }
    fun updateDescription(v: String) { _state.value = _state.value.copy(description = v) }
    fun updateAffiliateLink(v: String) { _state.value = _state.value.copy(affiliateLink = v) }
    fun updateStoryStyle(v: String) { _state.value = _state.value.copy(storyStyle = v, showStyleDropdown = false) }
    fun updateNumParts(v: String) { _state.value = _state.value.copy(numParts = v, showPartsDropdown = false) }
    fun toggleStyleDropdown() { _state.value = _state.value.copy(showStyleDropdown = !_state.value.showStyleDropdown) }
    fun togglePartsDropdown() { _state.value = _state.value.copy(showPartsDropdown = !_state.value.showPartsDropdown) }

    fun addMedia(uris: List<Uri>) {
        _state.value = _state.value.copy(mediaUris = _state.value.mediaUris + uris)
    }

    fun removeMedia(uri: Uri) {
        _state.value = _state.value.copy(mediaUris = _state.value.mediaUris - uri)
    }

    fun updatePart(index: Int, text: String) {
        val parts = _state.value.generatedParts.toMutableList()
        if (index in parts.indices) {
            parts[index] = text
            _state.value = _state.value.copy(generatedParts = parts)
        }
    }

    fun generate() {
        val s = _state.value
        if (s.productTitle.isBlank()) {
            _state.value = s.copy(error = "Judul produk harus diisi")
            return
        }
        _state.value = s.copy(isGenerating = true, error = null, generatedParts = emptyList())

        viewModelScope.launch {
            try {
                val numParts = when (s.numParts) {
                    "Auto" -> null
                    else -> s.numParts.toIntOrNull()
                }
                val parts = repo.generateContent(
                    productTitle = s.productTitle,
                    description = s.description,
                    affiliateLink = s.affiliateLink,
                    style = s.storyStyle,
                    numParts = numParts
                )
                _state.value = _state.value.copy(isGenerating = false, generatedParts = parts)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isGenerating = false,
                    error = "Gagal generate: ${e.message}"
                )
            }
        }
    }

    fun postNow() {
        val s = _state.value
        if (s.generatedParts.isEmpty()) return
        if (!prefs.isThreadsConnected) {
            _state.value = s.copy(error = "Login ke Threads dulu di Settings")
            return
        }
        _state.value = s.copy(isPosting = true, error = null)

        viewModelScope.launch {
            try {
                val threadIds = repo.postToThreads(s.generatedParts)
                repo.savePost(
                    PostHistory(
                        productTitle = s.productTitle,
                        affiliateLink = s.affiliateLink,
                        storyStyle = s.storyStyle,
                        parts = s.generatedParts,
                        status = "POSTED",
                        threadIds = threadIds
                    )
                )
                _state.value = _state.value.copy(isPosting = false, postSuccess = true)
            } catch (e: Exception) {
                repo.savePost(
                    PostHistory(
                        productTitle = s.productTitle,
                        affiliateLink = s.affiliateLink,
                        storyStyle = s.storyStyle,
                        parts = s.generatedParts,
                        status = "FAILED",
                        errorMessage = e.message
                    )
                )
                _state.value = _state.value.copy(
                    isPosting = false,
                    error = "Gagal posting: ${e.message}"
                )
            }
        }
    }

    fun schedulePost(timeMillis: Long) {
        val s = _state.value
        if (s.generatedParts.isEmpty()) return

        viewModelScope.launch {
            try {
                repo.savePost(
                    PostHistory(
                        productTitle = s.productTitle,
                        affiliateLink = s.affiliateLink,
                        storyStyle = s.storyStyle,
                        parts = s.generatedParts,
                        status = "SCHEDULED",
                        scheduledAt = timeMillis
                    )
                )
                _state.value = s.copy(
                    showScheduler = false,
                    scheduleSuccess = true
                )
            } catch (e: Exception) {
                _state.value = s.copy(error = "Gagal jadwalkan: ${e.message}")
            }
        }
    }

    fun toggleScheduler() {
        _state.value = _state.value.copy(showScheduler = !_state.value.showScheduler)
    }

    fun clearSuccess() {
        _state.value = _state.value.copy(postSuccess = false, scheduleSuccess = false)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun resetForm() {
        _state.value = CreatePostState()
    }
}
