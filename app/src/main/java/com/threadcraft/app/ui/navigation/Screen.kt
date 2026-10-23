package com.threadcraft.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val CREATE_POST = "create_post"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

data class NavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
)

val bottomNavItems = listOf(
    NavItem(Routes.CREATE_POST, "Buat Post", Icons.Default.Create),
    NavItem(Routes.HISTORY, "Riwayat", Icons.Default.History),
    NavItem(Routes.SETTINGS, "Pengaturan", Icons.Default.Settings)
)
