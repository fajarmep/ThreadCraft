package com.threadcraft.app.ui.create

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.threadcraft.app.ui.theme.Accent
import com.threadcraft.app.ui.theme.OnAccent
import com.threadcraft.app.ui.theme.Outline
import com.threadcraft.app.ui.theme.SurfaceContainer
import com.threadcraft.app.ui.theme.SurfaceVariant
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(viewModel: CreatePostViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) viewModel.addMedia(uris)
    }

    LaunchedEffect(state.postSuccess) {
        if (state.postSuccess) {
            snackbarHostState.showSnackbar("Berhasil diposting ke Threads!")
            viewModel.clearSuccess()
            viewModel.resetForm()
        }
    }

    LaunchedEffect(state.scheduleSuccess) {
        if (state.scheduleSuccess) {
            snackbarHostState.showSnackbar("Berhasil dijadwalkan!")
            viewModel.clearSuccess()
            viewModel.resetForm()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(12.dp))
                        Text("Buat Post")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Product title
            FlatField(
                value = state.productTitle,
                onValueChange = { viewModel.updateProductTitle(it) },
                label = "Judul Produk",
                placeholder = "Nama produk yang dipromosikan"
            )
            Spacer(Modifier.height(12.dp))

            // Description
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Deskripsi Singkat") },
                placeholder = { Text("Jelaskan produk secara singkat...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RectangleShape,
                minLines = 3,
                maxLines = 5,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Outline,
                    cursorColor = Accent,
                    focusedLabelColor = Accent
                )
            )
            Spacer(Modifier.height(12.dp))

            // Affiliate link
            FlatField(
                value = state.affiliateLink,
                onValueChange = { viewModel.updateAffiliateLink(it) },
                label = "Link Affiliate",
                placeholder = "https://..."
            )
            Spacer(Modifier.height(16.dp))

            // Media picker
            Text(
                "Media",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Add media button
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(SurfaceVariant, RectangleShape)
                        .border(1.dp, Outline, RectangleShape)
                        .clickable { mediaPickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Add, contentDescription = "Add media", tint = Accent)
                        Text("Tambah", style = MaterialTheme.typography.labelSmall, color = Accent)
                    }
                }

                // Media preview thumbnails
                state.mediaUris.forEach { uri ->
                    Box(modifier = Modifier.size(80.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        IconButton(
                            onClick = { viewModel.removeMedia(uri) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(24.dp)
                                .background(Accent, RectangleShape)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Style dropdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    DropdownSelector(
                        label = "Gaya Cerita",
                        selected = state.storyStyle,
                        expanded = state.showStyleDropdown,
                        onToggle = { viewModel.toggleStyleDropdown() },
                        options = viewModel.storyStyles,
                        onSelect = { viewModel.updateStoryStyle(it) }
                    )
                }

                Box(modifier = Modifier.weight(0.6f)) {
                    DropdownSelector(
                        label = "Jumlah Bagian",
                        selected = state.numParts,
                        expanded = state.showPartsDropdown,
                        onToggle = { viewModel.togglePartsDropdown() },
                        options = viewModel.partOptions,
                        onSelect = { viewModel.updateNumParts(it) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Generate button
            Button(
                onClick = { viewModel.generate() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                enabled = !state.isGenerating
            ) {
                if (state.isGenerating) {
                    Text("Sedang Generate...", style = MaterialTheme.typography.labelLarge)
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate dengan AI", style = MaterialTheme.typography.labelLarge)
                }
            }

            if (state.generatedParts.isEmpty()) {
                state.error?.let { err ->
                    Spacer(Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariant, RectangleShape)
                            .border(1.dp, MaterialTheme.colorScheme.error, RectangleShape)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Detail Log Galat:",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Error Log", err)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Log disalin!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RectangleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Salin Log", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearError()
                                    },
                                    shape = RectangleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Hapus", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // Preview section
            if (state.generatedParts.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Preview Thread",
                    style = MaterialTheme.typography.titleMedium,
                    color = Accent
                )
                Spacer(Modifier.height(12.dp))

                state.generatedParts.forEachIndexed { index, part ->
                    var editableText by remember(part) { mutableStateOf(part) }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariant, RectangleShape)
                            .border(1.dp, Outline, RectangleShape)
                            .padding(1.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceContainer)
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Bagian ${index + 1} / ${state.generatedParts.size}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Accent
                                )
                            }
                            OutlinedTextField(
                                value = editableText,
                                onValueChange = {
                                    editableText = it
                                    viewModel.updatePart(index, it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RectangleShape,
                                minLines = 2,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Accent,
                                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                                    cursorColor = Accent,
                                    focusedContainerColor = SurfaceVariant,
                                    unfocusedContainerColor = SurfaceVariant
                                )
                            )
                            Text(
                                "${editableText.length}/500",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (editableText.length > 500)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(16.dp))

                // Post and schedule buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { viewModel.postNow() },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
                        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                        enabled = !state.isPosting
                    ) {
                        if (state.isPosting) {
                            Text("Memposting...", style = MaterialTheme.typography.labelMedium)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Post ke Threads")
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val scheduled = Calendar.getInstance().apply {
                                                set(year, month, day, hour, minute, 0)
                                            }
                                            viewModel.schedulePost(scheduled.timeInMillis)
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                cal.get(Calendar.YEAR),
                                cal.get(Calendar.MONTH),
                                cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RectangleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Accent)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Jadwalkan", color = Accent)
                    }
                }

                state.error?.let { err ->
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariant, RectangleShape)
                            .border(1.dp, MaterialTheme.colorScheme.error, RectangleShape)
                            .padding(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "Detail Log Galat Posting:",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Error Log", err)
                                        clipboard.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Log disalin!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RectangleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Salin Log", style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        viewModel.clearError()
                                    },
                                    shape = RectangleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Hapus", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun FlatField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = Outline,
            cursorColor = Accent,
            focusedLabelColor = Accent
        )
    )
}

@Composable
private fun DropdownSelector(
    label: String,
    selected: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant, RectangleShape)
                    .border(1.dp, Outline, RectangleShape)
                    .clickable { onToggle() }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selected,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onToggle() },
                modifier = Modifier.background(SurfaceContainer)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onSelect(option) }
                    )
                }
            }
        }
    }
}
