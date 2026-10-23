package com.threadcraft.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.ui.navigation.MainScreen
import com.threadcraft.app.ui.theme.ThreadCraftTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = SecurePrefs(this)
        setContent {
            ThreadCraftTheme {
                MainScreen(prefs = prefs)
            }
        }
    }
}
