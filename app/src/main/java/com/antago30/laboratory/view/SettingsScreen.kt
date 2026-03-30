package com.antago30.laboratory.view

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.ui.component.settingsScreen.SettingsHeader
import com.antago30.laboratory.ui.component.settingsScreen.bleDeviceSelectionDialog.BleDeviceSelectionDialog
import com.antago30.laboratory.ui.component.settingsScreen.staffSelectionDialog.StaffSelectionDialog
import com.antago30.laboratory.viewmodel.settingsScreenViewModel.SettingsScreenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("MissingPermission")
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsScreenViewModel,
    onBack: () -> Unit,
    onAddUser: () -> Unit = {}
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Состояния диалогов
    var showDeviceDialog by remember { mutableStateOf(false) }
    var showStaffDialog by remember { mutableStateOf(false) }

    // Состояния из ViewModel
    val currentUserId by viewModel.currentUserId.collectAsState()
    val staffList by viewModel.staffList.collectAsState()
    val selectedDeviceAddress by viewModel.selectedDeviceAddress.collectAsState()

    // Находим объект текущего пользователя для отображения
    val currentUser = staffList.find { it.id == currentUserId }

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

    // === Диалог выбора BLE-устройства ===
    if (showDeviceDialog) {
        BleDeviceSelectionDialog(
            devices = viewModel.availableDevices,
            isScanning = viewModel.isScanning,
            selectedDeviceAddress = selectedDeviceAddress,
            onDeviceSelected = { device ->
                viewModel.selectDevice(device)
                showDeviceDialog = false
            },
            onDismiss = {
                viewModel.stopDeviceScan()
                showDeviceDialog = false
            },
            onRefresh = { viewModel.startDeviceScan() }
        )
    }

    // === Диалог выбора сотрудника ===
    if (showStaffDialog) {
        StaffSelectionDialog(
            staffList = staffList,
            currentUserId = currentUserId,
            onStaffSelected = { staff ->
                viewModel.selectCurrentUser(staff)
                showStaffDialog = false
            },
            onDismiss = { showStaffDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            SettingsHeader(
                onBack = onBack,
                onBleDeviceClick = {
                    // Логика открытия диалога BLE
                    val hasPermissions = viewModel.checkBlePermissions(context)
                    if (hasPermissions) {
                        viewModel.startDeviceScan()
                        showDeviceDialog = true
                    } else {
                        coroutineScope.launch {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.BLUETOOTH_SCAN,
                                    Manifest.permission.BLUETOOTH_CONNECT
                                )
                            )
                        }
                    }
                },
                onUserClick = { showStaffDialog = true },
                showBleButton = true,
                showUserButton = true
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === Инфо-блок: текущие выборы (компактный, только для отображения) ===
            /*if (selectedDeviceAddress != null || currentUser != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Инфо об устройстве
                    if (selectedDeviceAddress != null) {
                        InfoChip(
                            icon = androidx.compose.material.icons.Icons.Default.Bluetooth,
                            label = viewModel.selectedDeviceName ?: "Устройство",
                            subLabel = selectedDeviceAddress,
                            onClear = { viewModel.clearSelectedDevice() }
                        )
                    }
                    // Инфо о пользователе
                    if (currentUser != null) {
                        InfoChip(
                            icon = androidx.compose.material.icons.Icons.Default.Person,
                            label = currentUser.name,
                            subLabel = "Инициалы: ${currentUser.initials}",
                            onClear = { viewModel.clearCurrentUser() }
                        )
                    }
                }
            }*/

            if (selectedDeviceAddress == null && currentUser == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = "Выберите устройство и пользователя",
                        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    androidx.compose.material3.Text(
                        text = "Нажмите на иконки 🔗 или 👤 в шапке экрана",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// === Вспомогательный компонент для отображения выбранного ===
@Composable
private fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    subLabel: String? = null,
    onClear: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                tint = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                androidx.compose.material3.Text(
                    text = label,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                )
                if (subLabel != null) {
                    androidx.compose.material3.Text(
                        text = subLabel,
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        androidx.compose.material3.IconButton(
            onClick = onClear,
            modifier = Modifier.size(32.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Close,
                contentDescription = "Очистить",
                tint = androidx.compose.material3.MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}