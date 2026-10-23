package com.threadcraft.app.ui.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.threadcraft.app.data.model.PostHistory
import com.threadcraft.app.ui.theme.Accent
import com.threadcraft.app.ui.theme.ErrorRed
import com.threadcraft.app.ui.theme.Outline
import com.threadcraft.app.ui.theme.SuccessGreen
import com.threadcraft.app.ui.theme.SurfaceContainer
import com.threadcraft.app.ui.theme.SurfaceVariant
import com.threadcraft.app.ui.theme.WarningYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(12.dp))
                        Text("Riwayat Post")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (state.posts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Belum ada riwayat post",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }
                items(state.posts, key = { it.id }) { post ->
                    HistoryItem(
                        post = post,
                        isExpanded = state.expandedId == post.id,
                        onToggle = { viewModel.toggleExpanded(post.id) },
                        onDelete = { viewModel.deletePost(post.id) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun HistoryItem(
    post: PostHistory,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when (post.status) {
        "POSTED" -> SuccessGreen
        "FAILED" -> ErrorRed
        "SCHEDULED" -> WarningYellow
        "POSTING" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val statusIcon = when (post.status) {
        "POSTED" -> Icons.Default.CheckCircle
        "FAILED" -> Icons.Default.Error
        "SCHEDULED" -> Icons.Default.Schedule
        "POSTING" -> Icons.Default.Sync
        else -> Icons.Default.History
    }
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceVariant, RectangleShape)
            .border(1.dp, Outline, RectangleShape)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.productTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row {
                    Text(
                        post.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        dateFormat.format(Date(post.createdAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed, modifier = Modifier.size(18.dp))
            }
            Icon(
                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceContainer)
                    .padding(12.dp)
            ) {
                Text(
                    "Gaya: ${post.storyStyle}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Accent
                )
                Spacer(Modifier.height(8.dp))

                post.parts.forEach { part ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariant, RectangleShape)
                            .border(1.dp, Outline, RectangleShape)
                            .padding(10.dp)
                    ) {
                        Text(
                            part,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                if (post.errorMessage != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Error: ${post.errorMessage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ErrorRed
                    )
                }

                if (post.scheduledAt != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Dijadwalkan: ${dateFormat.format(Date(post.scheduledAt))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = WarningYellow
                    )
                }
            }
        }
    }
}
