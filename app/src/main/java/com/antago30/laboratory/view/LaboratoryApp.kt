package com.antago30.laboratory.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.antago30.laboratory.viewmodel.LabControlViewModel

@Composable
fun LaboratoryApp(
    viewModel: LabControlViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "lab_control",
        modifier = modifier
    ) {
        composable("lab_control") {
            LabControlScreen(
                viewModel = viewModel,
                onSettingsClick = { navController.navigate("settings") }
            )
        }
        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}