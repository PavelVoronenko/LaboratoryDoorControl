package com.antago30.laboratory.view

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.ui.component.settingsScreen.SettingsHeader
import com.antago30.laboratory.ui.component.settingsScreen.bleDeviceSelectionDialog.BleDeviceSelectionDialog
import com.antago30.laboratory.ui.component.settingsScreen.staffSelectionDialog.StaffSelectionDialog
import com.antago30.laboratory.ui.theme.Primary
import com.antago30.laboratory.viewmodel.settingsScreenViewModel.SettingsScreenViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("MissingPermission")
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsScreenViewModel,
    onBack: () -> Unit,
    onAddUserClick: () -> Unit,
    onManageUsersClick: () -> Unit,
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
            if (selectedDeviceAddress == null && currentUser == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    androidx.compose.material3.Text(
                        text = "Выберите устройство и пользователя",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    androidx.compose.material3.Text(
                        text = "Нажмите на иконки 🔗 или 👤 в шапке экрана",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Ваш существующий контент (настройки, переключатели и т.д.)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ... ваши настройки ...
        }

        FloatingActionButton(
            onClick = onManageUsersClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp), // Сдвиг вверх
            containerColor = MaterialTheme.colorScheme.secondary
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Default.DeleteSweep,
                contentDescription = "Управление пользователями",
                tint = MaterialTheme.colorScheme.onSecondary
            )
        }

        // Плавающая кнопка добавления пользователя (справа снизу)
        FloatingActionButton(
            onClick = onAddUserClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = Primary
        ) {
            Icon(
                androidx.compose.material.icons.Icons.Default.PersonAdd,
                contentDescription = "Добавить пользователя",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}