package com.antago30.laboratory

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.antago30.laboratory.ble.BleConnectionManager
import com.antago30.laboratory.view.AddUserScreen
import com.antago30.laboratory.view.LabControlScreen
import com.antago30.laboratory.view.ManageUsersScreen
import com.antago30.laboratory.view.SettingsScreen
import com.antago30.laboratory.viewmodel.addUserViewModel.kt.AddUserViewModel
import com.antago30.laboratory.viewmodel.labControlViewModel.LabControlViewModel
import com.antago30.laboratory.viewmodel.manageUsersViewModel.kt.ManageUsersViewModel
import com.antago30.laboratory.viewmodel.manageUsersViewModel.kt.ManageUsersViewModelFactory
import com.antago30.laboratory.viewmodel.settingsScreenViewModel.SettingsScreenViewModel

@Composable
fun LaboratoryApp(
    labControlViewModel: LabControlViewModel,
    settingsScreenViewModel: SettingsScreenViewModel,
    modifier: Modifier = Modifier,
    addUserViewModel: AddUserViewModel,
    manageUsersViewModel: ManageUsersViewModel,
    connectionManager: BleConnectionManager,
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "lab_control",
        modifier = modifier
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
                onBack = { navController.popBackStack() },
                onAddUserClick = { navController.navigate("add_user") },
                onManageUsersClick = { navController.navigate("manage_users") }
            )
        }

        composable("add_user") {
            AddUserScreen(
                viewModel = addUserViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("manage_users") {
            val manageViewModel: ManageUsersViewModel = viewModel(
                factory = ManageUsersViewModelFactory(
                    connectionManager = connectionManager
                )
            )
            ManageUsersScreen(
                viewModel = manageViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}