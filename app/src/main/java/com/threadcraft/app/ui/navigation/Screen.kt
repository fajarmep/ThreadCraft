package com.threadcraft.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String = "", val icon: ImageVector? = null) {
    object Splash : Screen("splash")
    object CreatePost : Screen("create_post", "Buat Post", Icons.Default.Create)
    object History : Screen("history", "Riwayat", Icons.Default.History)
    object Settings : Screen("settings", "Pengaturan", Icons.Default.Settings)
}

val bottomNavItems: List<Screen>
    get() = listOf(Screen.CreatePost, Screen.History, Screen.Settings)
