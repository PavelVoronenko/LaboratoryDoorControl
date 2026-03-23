package com.antago30.laboratory.view

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.BluetoothSearching
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.ui.component.settingsScreen.BleDeviceSelectionDialog
import com.antago30.laboratory.ui.component.settingsScreen.SettingsHeader
import com.antago30.laboratory.viewmodel.SettingsScreenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("MissingPermission")
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsScreenViewModel,
    onBack: () -> Unit, onAddUser: () -> Unit = {},
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showDeviceDialog by remember { mutableStateOf(false) }

    // Лаунчер для запроса разрешений
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val scanGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] == true
        val connectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] == true

        if (scanGranted && connectGranted) {
            viewModel.startDeviceScan()
            showDeviceDialog = true
        } else {
            coroutineScope.launch {
                snackBarHostState.showSnackbar(
                    "Разрешения Bluetooth не предоставлены. Проверьте настройки приложения."
                )
            }
        }
    }

    // Диалог выбора BLE-устройства
    if (showDeviceDialog) {
        BleDeviceSelectionDialog(
            devices = viewModel.availableDevices,
            isScanning = viewModel.isScanning,
            onDeviceSelected = { device ->
                viewModel.selectDevice(device)
                showDeviceDialog = false
            },
            onDismiss = {
                viewModel.stopDeviceScan()
                showDeviceDialog = false
            },
            onRefresh = { viewModel.startDeviceScan() })
    }

    Scaffold(snackbarHost = {
        SnackbarHost(snackBarHostState) },
        topBar = {
            SettingsHeader(
                onBack = onBack,
                onAddUser = onAddUser
            )
        }) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Блок: выбранное BLE-устройство
            Card(
                modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(8.dp)) {

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = viewModel.selectedDeviceName ?: viewModel.selectedDeviceAddress
                            ?: "Не выбрано",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.weight(1f),
                            trailingIcon = {
                                if (viewModel.selectedDeviceAddress != null) {
                                    IconButton(onClick = { viewModel.clearSelectedDevice() }) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Очистить",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            ),
                            singleLine = true
                        )

                        IconButton(
                            onClick = {
                                val hasPermissions = viewModel.checkBlePermissions(context)

                                if (hasPermissions) {
                                    // Разрешения уже есть — запускаем сканирование
                                    viewModel.startDeviceScan()
                                    showDeviceDialog = true
                                } else {
                                    //  Запрашиваем разрешения
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.BLUETOOTH_SCAN,
                                            Manifest.permission.BLUETOOTH_CONNECT
                                        )
                                    )
                                }
                            },
                            modifier = Modifier
                                .size(48.dp),
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.BluetoothSearching,
                                contentDescription = "Выбрать устройство",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}