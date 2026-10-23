package com.threadcraft.app.ui.settings

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.threadcraft.app.ui.theme.Accent
import com.threadcraft.app.ui.theme.Outline
import com.threadcraft.app.ui.theme.SuccessGreen
import com.threadcraft.app.ui.theme.SurfaceVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            snackbarHostState.showSnackbar("Pengaturan tersimpan")
            viewModel.clearSaveSuccess()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (state.showOAuthWebView) {
        OAuthDialog(
            url = state.oauthUrl,
            redirectUri = state.redirectUri,
            onCodeReceived = { viewModel.handleOAuthCallback(it) },
            onDismiss = { viewModel.dismissOAuth() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(12.dp))
                        Text("Pengaturan")
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
            SectionHeader("Konfigurasi AI")
            Spacer(Modifier.height(8.dp))

            FlatTextField(
                value = state.aiBaseUrl,
                onValueChange = { viewModel.updateAiBaseUrl(it) },
                label = "AI Base URL",
                placeholder = "https://api.openai.com/v1"
            )
            Spacer(Modifier.height(12.dp))

            FlatTextField(
                value = state.aiApiKey,
                onValueChange = { viewModel.updateAiApiKey(it) },
                label = "API Key",
                placeholder = "sk-...",
                isPassword = true
            )
            Spacer(Modifier.height(12.dp))

            FlatTextField(
                value = state.aiModel,
                onValueChange = { viewModel.updateAiModel(it) },
                label = "Model",
                placeholder = "gpt-4o-mini"
            )
            Spacer(Modifier.height(10.dp))

            // Tombol Test Koneksi AI
            OutlinedButton(
                onClick = { viewModel.testAiConnection() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RectangleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Accent),
                enabled = !state.isTestingAi
            ) {
                if (state.isTestingAi) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Accent,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Menguji koneksi AI...", color = Accent)
                } else {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Test Koneksi AI", color = Accent)
                }
            }

            state.aiTestMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceVariant, RectangleShape)
                        .border(1.dp, SuccessGreen, RectangleShape)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = msg,
                            color = SuccessGreen,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionHeader("Threads API")
            Spacer(Modifier.height(8.dp))

            FlatTextField(
                value = state.threadsAppId,
                onValueChange = { viewModel.updateThreadsAppId(it) },
                label = "App ID",
                placeholder = "Your Threads App ID"
            )
            Spacer(Modifier.height(12.dp))

            FlatTextField(
                value = state.threadsAppSecret,
                onValueChange = { viewModel.updateThreadsAppSecret(it) },
                label = "App Secret",
                placeholder = "Your Threads App Secret",
                isPassword = true
            )
            Spacer(Modifier.height(12.dp))

            FlatTextField(
                value = state.redirectUri,
                onValueChange = { viewModel.updateRedirectUri(it) },
                label = "OAuth Redirect URI",
                placeholder = "https://threadcraft.app/callback"
            )
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daftarkan URL ini di Meta Developer App",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Redirect URI", state.redirectUri)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Redirect URI disalin!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RectangleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Outline),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Salin URI", style = MaterialTheme.typography.labelSmall, color = Accent)
                }
            }

            Spacer(Modifier.height(16.dp))

            if (state.isThreadsConnected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceVariant, RectangleShape)
                        .border(1.dp, Outline, RectangleShape)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Terhubung",
                                style = MaterialTheme.typography.labelMedium,
                                color = SuccessGreen
                            )
                            Text(
                                "@${state.threadsUsername}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { viewModel.logout() }) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                if (state.isExchangingToken) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Accent,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Menghubungkan...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    OutlinedButton(
                        onClick = { viewModel.startOAuth() },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RectangleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Accent)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Login ke Threads", color = Accent)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Accent),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Simpan Pengaturan", style = MaterialTheme.typography.labelLarge)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = Accent,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun FlatTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RectangleShape,
        singleLine = true,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else
            androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Accent,
            unfocusedBorderColor = Outline,
            cursorColor = Accent,
            focusedLabelColor = Accent
        )
    )
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OAuthDialog(
    url: String,
    redirectUri: String,
    onCodeReceived: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Login Threads",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val redirectUrl = request?.url?.toString() ?: return false
                                if (redirectUrl.startsWith(redirectUri)) {
                                    onCodeReceived(redirectUrl)
                                    return true
                                }
                                return false
                            }
                        }
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
