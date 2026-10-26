package com.threadcraft.app.ui.settings

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.threadcraft.app.ui.theme.Accent
import com.threadcraft.app.ui.theme.OnAccent
import com.threadcraft.app.ui.theme.Outline
import com.threadcraft.app.ui.theme.SuccessGreen
import com.threadcraft.app.ui.theme.SurfaceContainer
import com.threadcraft.app.ui.theme.SurfaceVariant
import kotlinx.coroutines.delay

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

    LaunchedEffect(state.aiTestMessage) {
        if (state.aiTestMessage != null) {
            delay(4000)
            viewModel.clearAiTestMessage()
        }
    }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            delay(6000)
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
            val crashPrefs = remember { context.getSharedPreferences("crash_dump", Context.MODE_PRIVATE) }
            var lastCrash by remember { mutableStateOf(crashPrefs.getString("last_crash", null)) }

            lastCrash?.let { crashText ->
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceVariant, RectangleShape)
                        .border(1.dp, MaterialTheme.colorScheme.error, RectangleShape)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Log Crash Terakhir:",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = crashText.take(400),
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
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Crash Log", crashText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Log disalin!", Toast.LENGTH_SHORT).show()
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
                                    crashPrefs.edit().remove("last_crash").apply()
                                    lastCrash = null
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
                Spacer(Modifier.height(12.dp))
            }

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

            state.error?.let { err ->
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceVariant, RectangleShape)
                        .border(1.dp, MaterialTheme.colorScheme.error, RectangleShape)
                        .padding(12.dp)
                ) {
                    Text(
                        text = err,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
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
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceVariant, RectangleShape)
                            .border(1.dp, Accent, RectangleShape)
                            .padding(16.dp)
                    ) {
                        Text(
                            "Menghubungkan ke Threads...",
                            color = Accent,
                            style = MaterialTheme.typography.bodyMedium
                        )
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

                    Spacer(Modifier.height(8.dp))
                    var showManualAuth by remember { mutableStateOf(false) }
                    var manualCodeInput by remember { mutableStateOf("") }

                    OutlinedButton(
                        onClick = { showManualAuth = !showManualAuth },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RectangleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
                    ) {
                        Text(
                            if (showManualAuth) "Tutup Login Browser" else "Login via Browser Luar (Alternatif 2FA)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }

                    if (showManualAuth) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceContainer, RectangleShape)
                                .border(1.dp, Outline, RectangleShape)
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    "Jika 2FA di in-app browser macet:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "1. Klik tombol di bawah untuk buka otorisasi di Chrome/browser.\n2. Login & masukkan 2FA di browser hingga dialihkan ke callback.\n3. Salin URL callback atau kodenya lalu tempel di sini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        val s = state
                                        val targetRedirect = s.redirectUri.trim().ifBlank { "https://threadcraft.app/callback" }
                                        val u = Uri.parse("https://threads.net/oauth/authorize").buildUpon()
                                            .appendQueryParameter("client_id", s.threadsAppId.trim())
                                            .appendQueryParameter("redirect_uri", targetRedirect)
                                            .appendQueryParameter("scope", "threads_basic,threads_content_publish,threads_manage_replies")
                                            .appendQueryParameter("response_type", "code")
                                            .build()
                                        try {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, u))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Gagal buka browser: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RectangleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Accent)
                                ) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Accent, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Buka Otorisasi di Browser", color = Accent, style = MaterialTheme.typography.labelSmall)
                                }
                                Spacer(Modifier.height(8.dp))
                                FlatTextField(
                                    value = manualCodeInput,
                                    onValueChange = { manualCodeInput = it },
                                    label = "Tempel URL Callback / Auth Code",
                                    placeholder = "https://threadcraft.app/callback?code=..."
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        if (manualCodeInput.isNotBlank()) {
                                            viewModel.handleOAuthCallback(manualCodeInput.trim())
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(42.dp),
                                    shape = RectangleShape,
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent)
                                ) {
                                    Text("Hubungkan Akun", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RectangleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent),
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
    val context = LocalContext.current
    var manualInput by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
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
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Gagal buka browser: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = "Buka di Browser",
                        tint = Accent
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close", tint = MaterialTheme.colorScheme.onSurface)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceVariant)
                    .border(1.dp, Outline, RectangleShape)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showManualInput) "Tempel URL callback atau kode auth:" else "2FA macet? Buka di browser luar & tempel hasil di sini",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    )
                    OutlinedButton(
                        onClick = { showManualInput = !showManualInput },
                        shape = RectangleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Accent),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(
                            text = if (showManualInput) "Tutup" else "Tempel Kode",
                            style = MaterialTheme.typography.labelSmall,
                            color = Accent
                        )
                    }
                }
            }

            if (showManualInput) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceContainer)
                        .padding(8.dp)
                ) {
                    FlatTextField(
                        value = manualInput,
                        onValueChange = { manualInput = it },
                        label = "Kode atau URL Callback",
                        placeholder = "https://threadcraft.app/callback?code=..."
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (manualInput.isNotBlank()) {
                                onCodeReceived(manualInput.trim())
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RectangleShape,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = OnAccent)
                    ) {
                        Text("Hubungkan Akun", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            AndroidView(
                factory = { ctx ->
                    val frameLayout = FrameLayout(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val mainWebView = WebView(ctx).apply {
                        layoutParams = FrameLayout.LayoutParams(
                            FrameLayout.LayoutParams.MATCH_PARENT,
                            FrameLayout.LayoutParams.MATCH_PARENT
                        )
                        isFocusable = true
                        isFocusableInTouchMode = true

                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            javaScriptCanOpenWindowsAutomatically = true
                            setSupportMultipleWindows(true)
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                            // Clean user agent to standard mobile Chrome
                            userAgentString = userAgentString
                                .replace("; wv", "")
                                .replace(Regex("Version/[0-9.]+\\s*"), "")
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onCreateWindow(
                                view: WebView?,
                                isDialog: Boolean,
                                isUserGesture: Boolean,
                                resultMsg: android.os.Message?
                            ): Boolean {
                                val popupWebView = WebView(ctx).apply {
                                    layoutParams = FrameLayout.LayoutParams(
                                        FrameLayout.LayoutParams.MATCH_PARENT,
                                        FrameLayout.LayoutParams.MATCH_PARENT
                                    )
                                    isFocusable = true
                                    isFocusableInTouchMode = true
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.userAgentString = view?.settings?.userAgentString
                                    val cm = CookieManager.getInstance()
                                    cm.setAcceptCookie(true)
                                    cm.setAcceptThirdPartyCookies(this, true)

                                    webChromeClient = object : WebChromeClient() {
                                        override fun onCloseWindow(window: WebView?) {
                                            frameLayout.removeView(window)
                                            window?.destroy()
                                        }
                                    }

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            v: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val rUrl = request?.url?.toString() ?: return false
                                            if (rUrl.startsWith(redirectUri)) {
                                                onCodeReceived(rUrl)
                                                return true
                                            }
                                            return false
                                        }
                                    }
                                }
                                frameLayout.addView(popupWebView)
                                val transport = resultMsg?.obj as? WebView.WebViewTransport
                                transport?.webView = popupWebView
                                resultMsg?.sendToTarget()
                                return true
                            }
                        }

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
                        requestFocus()
                    }

                    frameLayout.addView(mainWebView)
                    frameLayout
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}
