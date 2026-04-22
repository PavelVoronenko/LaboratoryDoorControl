package com.antago30.laboratory.view

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.antago30.laboratory.ui.component.settingsScreen.SettingsHeader
import com.antago30.laboratory.ui.component.settingsScreen.bleDeviceSelectionDialog.BleDeviceSelectionDialog
import com.antago30.laboratory.ui.component.settingsScreen.terminalLog.TerminalLogPanel
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
    onManageUsersClick: () -> Unit,
    connectionManager: com.antago30.laboratory.ble.BleConnectionManager
) {
    val context = LocalContext.current
    val snackBarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    // Состояния диалогов
    var showDeviceDialog by remember { mutableStateOf(false) }

    // Состояния из ViewModel
    val selectedDeviceAddress by viewModel.selectedDeviceAddress.collectAsState()
    val bleConnectionState by connectionManager.connectionStateFlow.collectAsState()
    val terminalLogs by viewModel.terminalLogs.collectAsState()
    val isTerminalActive by viewModel.isTerminalActive.collectAsState()

    // Управление наблюдением за логами терминала
    LaunchedEffect(Unit) {
        viewModel.startTerminalObservation()
    }

    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.stopTerminalObservation()
        }
    }

    // Подписка на данные при готовности соединения
    LaunchedEffect(bleConnectionState) {
        if (bleConnectionState == com.antago30.laboratory.model.ConnectionState.READY) {
            connectionManager.subscribeToSensorData()
            connectionManager.requestMtu(200)

            // Запрос истории логов после готовности сервисов
            kotlinx.coroutines.delay(500)
            viewModel.requestLogHistory()
        }
    }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackBarHostState) },
        topBar = {
            SettingsHeader(
                onBack = onBack,
                onBleDeviceClick = {
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
                showBleButton = true,
                connectionState = bleConnectionState
            )
        },
        floatingActionButton = {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = Primary),
                        onClick = onManageUsersClick
                    ),
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.5.dp, Primary.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Primary.copy(alpha = 0.12f),
                                    Primary.copy(alpha = 0.04f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ManageAccounts,
                        contentDescription = "Управление пользователями",
                        tint = Primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedDeviceAddress == null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Выберите устройство",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Нажмите на иконку 🔗 в шапке экрана",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            } else {
                TerminalLogPanel(
                    logs = terminalLogs,
                    onClearLogs = { viewModel.clearLogs() },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    isTerminalActive = isTerminalActive,
                    isEnabled = bleConnectionState == com.antago30.laboratory.model.ConnectionState.READY
                )
            }
        }
    }
}
