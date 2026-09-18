package com.example.appcloner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.appcloner.ui.screens.HomeScreen
import com.example.appcloner.ui.screens.AppPickerScreen
import com.example.appcloner.ui.viewmodel.HomeViewModel
import com.example.appcloner.ui.viewmodel.AppPickerViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object AppPicker : Screen("app_picker")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            val homeViewModel: HomeViewModel = hiltViewModel()
            HomeScreen(
                viewModel = homeViewModel,
                onNavigateToPicker = {
                    navController.navigate(Screen.AppPicker.route)
                }
            )
        }

        composable(Screen.AppPicker.route) {
            val pickerViewModel: AppPickerViewModel = hiltViewModel()
            AppPickerScreen(
                viewModel = pickerViewModel,
                onAppSelected = {
                    navController.popBackStack()
                }
            )
        }
    }
}
