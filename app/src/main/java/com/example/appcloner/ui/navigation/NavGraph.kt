package com.example.appcloner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.appcloner.ui.screens.AppPickerScreen
import com.example.appcloner.ui.screens.HomeScreen
import com.example.appcloner.ui.screens.SetupScreen
import com.example.appcloner.ui.viewmodel.AppPickerViewModel
import com.example.appcloner.ui.viewmodel.HomeViewModel
import com.example.appcloner.ui.viewmodel.SetupViewModel

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Home : Screen("home")
    object AppPicker : Screen("app_picker")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Setup.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // شاشة تهيئة Work Profile الأولى
        composable(Screen.Setup.route) {
            val setupViewModel: SetupViewModel = hiltViewModel()
            SetupScreen(
                viewModel = setupViewModel,
                onSetupComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                }
            )
        }

        // الشاشة الرئيسية للتطبيقات المنسوخة
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToPicker = {
                    navController.navigate(Screen.AppPicker.route)
                }
            )
        }

        // شاشة اختيار التطبيقات لنسخها
        composable(Screen.AppPicker.route) {
            val pickerViewModel: AppPickerViewModel = hiltViewModel()
            AppPickerScreen(
                viewModel = pickerViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
