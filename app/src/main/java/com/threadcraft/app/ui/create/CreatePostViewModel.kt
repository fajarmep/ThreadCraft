package com.threadcraft.app.ui.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threadcraft.app.ThreadCraftApp
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.data.model.PostHistory
import com.threadcraft.app.data.repository.AppRepository
import com.threadcraft.app.worker.PostScheduleWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
            } catch (t: Throwable) {
                android.util.Log.e("CreatePostViewModel", "Generate content error", t)
                val detailLog = buildString {
                    if (t is java.net.SocketTimeoutException) {
                        appendLine("Keterangan: Timeout (>180s). Server AI membutuhkan waktu terlalu lama untuk menyelesaikan respons.")
                    }
                    appendLine("Pesan: ${t.localizedMessage ?: t.message ?: "Terjadi kesalahan"}")
                    appendLine("Tipe: ${t.javaClass.simpleName}")
                    val cause = t.cause
                    if (cause != null) {
                        appendLine("Penyebab: ${cause.localizedMessage ?: cause.message}")
                    }
                    val stack = android.util.Log.getStackTraceString(t)
                    if (stack.isNotBlank()) {
                        appendLine("\n--- Detail Log ---")
                        appendLine(stack.take(1200))
                    }
                }
                _state.value = _state.value.copy(
                    isGenerating = false,
                    error = detailLog
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
                val threadIds = repo.postToThreads(s.generatedParts, s.mediaUris)
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
            } catch (t: Throwable) {
                android.util.Log.e("CreatePostViewModel", "Post to threads error", t)
                val detailLog = buildString {
                    val msg = t.localizedMessage ?: t.message ?: "Terjadi kesalahan"
                    appendLine("Pesan: $msg")
                    appendLine("Tipe: ${t.javaClass.simpleName}")
                    if (msg.contains("threads_manage_replies", ignoreCase = true) || (msg.contains("code 10") && msg.contains("permission", ignoreCase = true))) {
                        appendLine("\nSaran: Utas bersambung (reply chain) membutuhkan izin 'threads_manage_replies'. Silakan Logout lalu Login Ulang Threads di tab Pengaturan, dan pastikan fitur 'threads_manage_replies' aktif di Meta Developer Dashboard.")
                    } else if (msg.contains("190", ignoreCase = true) || msg.contains("token", ignoreCase = true) || msg.contains("session", ignoreCase = true)) {
                        appendLine("\nSaran: Sesi atau Token login Threads kemungkinan kadaluarsa/tidak valid. Silakan Logout lalu Login Ulang di menu Pengaturan.")
                    }
                    val cause = t.cause
                    if (cause != null) {
                        appendLine("Penyebab: ${cause.localizedMessage ?: cause.message}")
                    }
                    val stack = android.util.Log.getStackTraceString(t)
                    if (stack.isNotBlank()) {
                        appendLine("\n--- Detail Log ---")
                        appendLine(stack.take(1200))
                    }
                }
                repo.savePost(
                    PostHistory(
                        productTitle = s.productTitle,
                        affiliateLink = s.affiliateLink,
                        storyStyle = s.storyStyle,
                        parts = s.generatedParts,
                        status = "FAILED",
                        errorMessage = t.message
                    )
                )
                _state.value = _state.value.copy(
                    isPosting = false,
                    error = detailLog
                )
            }
        }
    }

    fun schedulePost(timeMillis: Long) {
        val s = _state.value
        if (s.generatedParts.isEmpty()) return

        val appContext = ThreadCraftApp.instance
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 1. Copy any attached media Uris to app private internal storage
                val savedMediaPaths = mutableListOf<String>()
                s.mediaUris.forEachIndexed { index, uri ->
                    try {
                        val input = appContext.contentResolver.openInputStream(uri)
                        if (input != null) {
                            val type = appContext.contentResolver.getType(uri) ?: ""
                            val isVid = type.startsWith("video", ignoreCase = true) || uri.toString().endsWith(".mp4", ignoreCase = true)
                            val ext = if (isVid) "mp4" else "jpg"
                            val destFile = File(appContext.filesDir, "sched_${System.currentTimeMillis()}_$index.$ext")
                            destFile.outputStream().use { out -> input.copyTo(out) }
                            savedMediaPaths.add(destFile.absolutePath)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("CreatePostViewModel", "Gagal menyalin media lokal: ${e.message}")
                    }
                }

                // 2. Save post to database
                val postId = repo.savePost(
                    PostHistory(
                        productTitle = s.productTitle,
                        affiliateLink = s.affiliateLink,
                        storyStyle = s.storyStyle,
                        parts = s.generatedParts,
                        status = "SCHEDULED",
                        scheduledAt = timeMillis,
                        mediaPaths = savedMediaPaths
                    )
                )

                // 3. Register WorkManager & AlarmManager to wake up and post
                PostScheduleWorker.schedulePostWork(appContext, postId, timeMillis)

                withContext(Dispatchers.Main) {
                    _state.value = s.copy(
                        showScheduler = false,
                        scheduleSuccess = true
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = s.copy(error = "Gagal jadwalkan: ${e.message}")
                }
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
