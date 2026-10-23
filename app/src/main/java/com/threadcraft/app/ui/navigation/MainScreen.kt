package com.threadcraft.app.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.threadcraft.app.data.local.SecurePrefs
import com.threadcraft.app.ui.AppViewModelFactory
import com.threadcraft.app.ui.create.CreatePostScreen
import com.threadcraft.app.ui.create.CreatePostViewModel
import com.threadcraft.app.ui.history.HistoryScreen
import com.threadcraft.app.ui.history.HistoryViewModel
import com.threadcraft.app.ui.settings.SettingsScreen
import com.threadcraft.app.ui.settings.SettingsViewModel
import com.threadcraft.app.ui.splash.SplashScreen

@Composable
fun MainScreen(prefs: SecurePrefs) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val factory = AppViewModelFactory(prefs)

    val showBottomBar = currentRoute in listOf(
        Screen.CreatePost.route,
        Screen.History.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(navController = navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Splash.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onNavigate = {
                        val destination = if (prefs.isSetupComplete && prefs.isThreadsConnected) {
                            Screen.CreatePost.route
                        } else {
                            Screen.Settings.route
                        }
                        navController.navigate(destination) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.CreatePost.route) {
                val createPostViewModel: CreatePostViewModel = viewModel(factory = factory)
                CreatePostScreen(viewModel = createPostViewModel)
            }
            composable(Screen.History.route) {
                val historyViewModel: HistoryViewModel = viewModel(factory = factory)
                HistoryScreen(viewModel = historyViewModel)
            }
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = viewModel(factory = factory)
                SettingsScreen(viewModel = settingsViewModel)
            }
        }
    }
}
