package com.antago30.laboratory.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.model.NewUserParams
import com.antago30.laboratory.ui.theme.Primary
import com.antago30.laboratory.viewmodel.addUserViewModel.kt.AddUserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserScreen(
    viewModel: AddUserViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val showError by viewModel.showError.collectAsState()
    val userAdded by viewModel.userAdded.collectAsState()

    // === Поля ввода ===
    var userId by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var selectedUuid by remember { mutableStateOf(viewModel.uuidOptions.first()) }
    var selectedServiceData by remember { mutableStateOf(viewModel.serviceDataOptions.first()) }
    var macAddress by remember { mutableStateOf("") }
    var rssiThreshold by remember { mutableStateOf("-70") }

    // === Экспандеры для выпадающих меню ===
    var uuidExpanded by remember { mutableStateOf(false) }
    var serviceDataExpanded by remember { mutableStateOf(false) }

    // === Обработка успешного добавления ===
    LaunchedEffect(userAdded) {
        if (userAdded) {
            // Показываем снекбар и возвращаемся назад
            onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить пользователя") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === ID пользователя ===
            OutlinedTextField(
                value = userId,
                onValueChange = { userId = it.filter { c -> c.isDigit() } },
                label = { Text("ID (число)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // === Имя и Отчество ===
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Имя и Отчество") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // === UUID (выпадающий список) ===
            ExposedDropdownMenuBox(
                expanded = uuidExpanded,
                onExpandedChange = { uuidExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedUuid,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("UUID") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = uuidExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = uuidExpanded,
                    onDismissRequest = { uuidExpanded = false }
                ) {
                    viewModel.uuidOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, style = MaterialTheme.typography.bodySmall) },
                            onClick = {
                                selectedUuid = option
                                uuidExpanded = false
                            }
                        )
                    }
                }
            }

            // === Service Data (выпадающий список) ===
            ExposedDropdownMenuBox(
                expanded = serviceDataExpanded,
                onExpandedChange = { serviceDataExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedServiceData,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Service Data") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceDataExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = serviceDataExpanded,
                    onDismissRequest = { serviceDataExpanded = false }
                ) {
                    viewModel.serviceDataOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                selectedServiceData = option
                                serviceDataExpanded = false
                            }
                        )
                    }
                }
            }

            // === MAC адрес ===
            OutlinedTextField(
                value = macAddress,
                onValueChange = {
                    // Автоформатирование: добавляем ":" после каждых 2 символов
                    val cleaned = it.uppercase().replace(":", "")
                    macAddress = cleaned.chunked(2).joinToString(":").take(17)
                },
                label = { Text("MAC адрес (XX:XX:XX:XX:XX:XX)") },
                placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // === RSSI порог (опционально) ===
            OutlinedTextField(
                value = rssiThreshold,
                onValueChange = {
                    rssiThreshold = it.filter { c -> c.isDigit() || c == '-' }
                },
                label = { Text("RSSI порог (по умолчанию -70)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.weight(1f))

            // === Кнопка "Добавить" ===
            Button(
                onClick = {
                    val params = NewUserParams(
                        id = userId.toIntOrNull() ?: 0,
                        name = userName,
                        uuid = selectedUuid,
                        serviceData = selectedServiceData,
                        macAddress = macAddress,
                        rssiThreshold = rssiThreshold.toIntOrNull() ?: -70
                    )
                    viewModel.addUser(params, context)
                },
                enabled = !isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Добавить пользователя")
                }
            }
        }
    }

    // === Показать ошибку, если есть ===
    showError?.let { message ->
        LaunchedEffect(message) {
            // Показываем снакбар с ошибкой
            // (реализация зависит от вашего Scaffold)
            viewModel.clearError()
        }
    }
}