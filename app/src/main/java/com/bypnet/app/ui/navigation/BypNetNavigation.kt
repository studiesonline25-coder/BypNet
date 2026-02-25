package com.bypnet.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.bypnet.app.ui.screens.BrowserScreen
import com.bypnet.app.ui.screens.HomeScreen
import com.bypnet.app.ui.screens.LogScreen
import com.bypnet.app.ui.screens.SettingsScreen

@Composable
fun BypNetNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        enterTransition = {
            fadeIn(animationSpec = tween(200))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200))
        }
    ) {
        composable(Screen.Home.route) {
            HomeScreen()
        }

        composable(Screen.Browser.route) {
            BrowserScreen()
        }

        composable(Screen.Logs.route) {
            LogScreen()
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
