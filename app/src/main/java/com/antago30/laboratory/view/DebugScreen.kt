package com.antago30.laboratory.view

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.ui.component.settingsScreen.SettingsHeader
import com.antago30.laboratory.viewmodel.settingsScreenViewModel.SettingsScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: SettingsScreenViewModel,
    onBack: () -> Unit
) {
    var distanceText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            SettingsHeader(
                onBack = onBack,
                showBleButton = false
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Экран отладки",
                style = MaterialTheme.typography.headlineMedium
            )

            OutlinedTextField(
                value = distanceText,
                onValueChange = { distanceText = it },
                label = { Text("Порог HC-SR04 (см)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Button(
                onClick = {
                    val dist = distanceText.toIntOrNull()
                    if (dist != null) {
                        viewModel.sendDistanceThreshold(dist)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = distanceText.isNotBlank()
            ) {
                Text("Установить порог")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.rebootController() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Перезагрузить контроллер", color = MaterialTheme.colorScheme.onError)
            }
        }
    }
}
