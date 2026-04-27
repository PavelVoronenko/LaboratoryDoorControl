package com.antago30.laboratory

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.view.DebugScreen
import com.antago30.laboratory.view.LabControlScreen
import com.antago30.laboratory.view.SettingsScreen
import com.antago30.laboratory.view.UserManagementScreen
import com.antago30.laboratory.viewmodel.labControlViewModel.LabControlViewModel
import com.antago30.laboratory.viewmodel.manageUsersViewModel.kt.UserManagementViewModel
import com.antago30.laboratory.viewmodel.settingsScreenViewModel.SettingsScreenViewModel

@Composable
fun LaboratoryApp(
    labControlViewModel: LabControlViewModel,
    settingsScreenViewModel: SettingsScreenViewModel,
    userManagementViewModel: UserManagementViewModel,
    modifier: Modifier = Modifier,
    connectionManager: BleConnectionManager,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "lab_control",
        modifier = modifier,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { 700 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(400))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -700 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(400))
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -700 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(400))
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { 700 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(400))
        }
    ) {
        composable("lab_control") {
            LabControlScreen(
                viewModel = labControlViewModel,
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = settingsScreenViewModel,
                onManageUsersClick = { navController.navigate("user_management") },
                onDebugClick = { navController.navigate("debug") },
                connectionManager = connectionManager
            )
        }
        composable("debug") {
            DebugScreen(
                viewModel = settingsScreenViewModel,
                onBack = { navController.popBackStack() },
                connectionManager = connectionManager
            )
        }
        composable("user_management") {
            LaunchedEffect(Unit) { userManagementViewModel.fetchUsers() }

            UserManagementScreen(
                viewModel = userManagementViewModel,
                onBack = { navController.popBackStack() },
                connectionManager = connectionManager,
                onUserChanged = {
                    labControlViewModel.onUserSelected()
                }
            )
        }
    }
}
