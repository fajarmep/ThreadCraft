package com.threadcraft.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.ui.navigation.MainScreen
import com.threadcraft.app.ui.theme.ThreadCraftTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val prefs = SecurePrefs(this)
            setContent {
                ThreadCraftTheme {
                    MainScreen(prefs = prefs)
                }
            }
        } catch (t: Throwable) {
            android.util.Log.e("MainActivity", "Fatal startup error", t)
            setContent {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0D0D0D))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Gagal memuat aplikasi: ${t.localizedMessage ?: t.message}",
                        color = Color(0xFFED3969)
                    )
                }
            }
        }
    }
}
