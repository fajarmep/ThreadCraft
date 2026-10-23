package com.threadcraft.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.threadcraft.app.ThreadCraftApp
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.data.model.PostHistory
import com.threadcraft.app.data.repository.AppRepository
import com.threadcraft.app.worker.PostScheduleWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HistoryState(
    val posts: List<PostHistory> = emptyList(),
    val expandedId: Long? = null
)

class HistoryViewModel(prefs: SecurePrefs) : ViewModel() {

    private val repo = AppRepository(prefs)
    private val _state = MutableStateFlow(HistoryState())
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        try {
            PostScheduleWorker.enqueueImmediateWork(ThreadCraftApp.instance)
        } catch (_: Throwable) {}
        viewModelScope.launch {
            repo.getAllPosts().collect { posts ->
                _state.value = _state.value.copy(posts = posts)
            }
        }
    }

    fun toggleExpanded(id: Long) {
        _state.value = _state.value.copy(
            expandedId = if (_state.value.expandedId == id) null else id
        )
    }

    fun deletePost(id: Long) {
        viewModelScope.launch {
            repo.deletePost(id)
        }
    }
}
