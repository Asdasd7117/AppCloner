package com.example.appcloner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.appcloner.admin.ProfileManager
import com.example.appcloner.ui.screens.AppPickerScreen
import com.example.appcloner.ui.screens.HomeScreen
import com.example.appcloner.ui.screens.SetupScreen

object Routes {
    const val SETUP = "setup"
    const val HOME = "home"
    const val PICKER = "picker"
}

@Composable
fun NavGraph(profileManager: ProfileManager) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.SETUP) {
        composable(Routes.SETUP) {
            SetupScreen(
                profileManager = profileManager,
                onSetupComplete = { navController.navigate(Routes.HOME) {
                    popUpTo(Routes.SETUP) { inclusive = true }
                }}
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onAddApp = { navController.navigate(Routes.PICKER) },
                onSettings = { navController.navigate(Routes.SETUP) }
            )
        }
        composable(Routes.PICKER) {
            AppPickerScreen(onBack = { navController.popBackStack() })
        }
    }
}